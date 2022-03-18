package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.EvidenceCode;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;

import java.text.SimpleDateFormat;
import java.util.*;

public class CurationDaf {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public List<GeneDiseaseAnnotation> disease_gene_ingest_set = new ArrayList<>();

    public void addGeneDiseaseAnnotation(Annotation a, Dao dao, Map<Integer, String> geneRgdId2HgncIdMap, int speciesTypeKey) throws Exception {

        GeneDiseaseAnnotation r = new GeneDiseaseAnnotation();
        r.creation_date = sdf_agr.format(a.getCreatedDate());
        if( a.getLastModifiedDate()!=null ) {
            r.data_last_modified = sdf_agr.format(a.getLastModifiedDate());
        }
        r.disease_qualifiers = getDiseaseQualifiers(a);
        r.evidence_codes = getEvidenceCodes(a.getEvidence());
        r.negated = getNegatedValue(a);
        r.object = a.getTermAcc();
        r.predicate = Utils2.getGeneAssocType(a.getEvidence());

        if( a.getDataSrc().equals("OMIM") ) {
            r.data_provider = "OMIM";
            r.secondary_data_provider = "RGD";
        } else {
            r.data_provider = "RGD";
        }

        String pmid = dao.getPmid(a.getRefRgdId());
        if( pmid==null ) {
            r.single_reference = "RGD:"+a.getRefRgdId();
        } else {
            r.single_reference = "PMID:"+pmid;
        }

        if( speciesTypeKey== SpeciesType.HUMAN ) {
            r.subject = geneRgdId2HgncIdMap.get(a.getAnnotatedObjectRgdId());
        } else if( speciesTypeKey==SpeciesType.RAT ) {
            r.subject = "RGD:"+a.getAnnotatedObjectRgdId();
        }

        handleWithInfo(a, r, dao);

        if( !Utils.isStringEmpty(a.getNotes()) ) {
            r.related_notes = new ArrayList();
            HashMap noteMap = new HashMap();
            noteMap.put("internal", false);
            noteMap.put("note_type", "disease_note");
            noteMap.put("free_text", a.getNotes().trim());
            r.related_notes.add(noteMap);
        }

        disease_gene_ingest_set.add(r);
    }

    List<String> getDiseaseQualifiers(Annotation a) {

        if( a.getQualifier()==null ) {
            return null;
        }
        List<String> qualifiers = null;

        String qualifier = a.getQualifier().trim();

        switch( qualifier ) {
            case "susceptibility":
            case "penetrance":
                qualifiers = new ArrayList<>();
                qualifiers.add(qualifier);
                break;

            case "ameliorates":
            case "exacerbates":
            case "severity":
                qualifiers = new ArrayList<>();
                qualifiers.add("severity");
                break;

            case "onset":
            case "MODEL: onset":
                qualifiers = new ArrayList<>();
                qualifiers.add("onset");
                break;

            case "resistance":
            case "resistant":
                qualifiers = new ArrayList<>();
                qualifiers.add("resistance");
                break;

            case "sexual_dimorphism":
            case "sexual dimorphism":
                qualifiers = new ArrayList<>();
                qualifiers.add("sexual_dimorphism");
                break;

            case "disease_progression":
            case "disease progression":
            case "MODEL: disease progression":
                qualifiers = new ArrayList<>();
                qualifiers.add("disease_progression");
                break;


            case "no_association":
            case "NOT":

            case "induced":
            case "induces":
            case "spontaneous":
            case "treatment":
            case "Treatment":
                break; // ignore

            // ignored for rat gene annotations
            case "MODEL":
            case "MODEL: control":
            case "MODEL:spontaneous":
            case "MODEL: spontaneous":
            case "MODEL: treatment":
            case "MODEL:induced":
            case "MODEL: induced":
                break; // ignore

            default:
                System.out.println("q "+a.getQualifier());
        }

        return qualifiers;
    }

    List<String> getEvidenceCodes(String evidence) {
        List<String> evidences = new ArrayList<>();
        evidences.add(getEcoId(evidence));
        return evidences;
    }
    // as of Aug 2021, EvidenceCode.getEcoId() is not thread safe and it was causing problems
    //  therefore we synchronise calls to it explicitly
    synchronized String getEcoId(String evidenceCode) {

        try {
            return EvidenceCode.getEcoId(evidenceCode);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    Boolean getNegatedValue(Annotation a) {

        if( a.getQualifier()==null ) {
            return null;
        }
        String qualifier = a.getQualifier().trim().toLowerCase();

        switch( qualifier ) {
            case "no_association":
            case "not":
                return true;

            default:
                if( qualifier.contains("control") ) {
                    return true;
                } else {
                    return null;
                }
        }
    }

    boolean handleWithInfo(Annotation a, GeneDiseaseAnnotation r, Dao dao) throws Exception {

        if( a.getWithInfo()==null ) {
            return true;
        }

        // only a subset of qualifiers is allowed
        String condRelType = null;
        if( a.getQualifier() == null ) {
            condRelType = "has_condition";
        } else if( a.getQualifier().contains("induced") || a.getQualifier().contains("induces") ) {
            condRelType = "induced_by";
        } else if( a.getQualifier().contains("treatment") || a.getQualifier().contains("ameliorates") ) {
            condRelType = "ameliorated_by";
        } else if( a.getQualifier().contains("exacerbates") ) {
            condRelType = "exacerbated_by";
        } else {
            System.out.println("UNMAPPED QUALIFIER: "+a.getQualifier());
            condRelType = "has_condition";
        }
        if( !condRelType.equals("has_condition") && (r.negated!=null && r.negated==true) ) {
            condRelType = "not_"+condRelType;
        }


        // remove all whitespace from WITH field to simplify parsing
        String withInfo = a.getWithInfo().replaceAll("\\s", "");
        List conditionRelations = new ArrayList();

        // if the separator is '|', create separate conditionRelation object
        // if the separator is ',', combine conditions
        boolean or;

        // out[0]: token;  out[1]: separator before token
        String[] out = new String[2];
        String str = withInfo;
        for( ;; ) {
            str = getNextToken(str, out);
            if( out[0]==null ) {
                break;
            }

            String withValue = out[0];
            if( out[1]!=null && out[1].equals(",") ) {
                or = false;
            } else {
                or = true;
            }

            withValue = transformRgdId(withValue, dao);
            if( withValue==null ) {
                return false;
            }

            if( withValue.startsWith("XCO:") ) {
                AgrExperimentalConditionMapper.Info info = AgrExperimentalConditionMapper.getInstance().getInfo(withValue);
                if (info == null) {
                    System.out.println("UNEXPECTED WITH VALUE: " + withValue);
                    return false;
                }


                HashMap h = new HashMap();
                h.put("condition_class", info.zecoAcc);
                if (info.xcoAcc != null && info.xcoAcc.startsWith("CHEBI:")) {
                    h.put("condition_chemical", info.xcoAcc);
                } else if( info.xcoAcc != null && info.xcoAcc.startsWith("UBERON:")) {
                    h.put("condition_anatomy", info.xcoAcc);
                } else if( info.xcoAcc != null && info.xcoAcc.startsWith("GO:")) {
                    h.put("condition_gene_ontology", info.xcoAcc);
                } else {
                    h.put("condition_id", info.xcoAcc);
                }
                h.put("condition_statement", info.conditionStatement);

                if (or) {
                    Map condRel = new HashMap();
                    condRel.put("condition_relation_type", condRelType);

                    List conditions = new ArrayList();
                    condRel.put("conditions", conditions);
                    conditions.add(h);

                    conditionRelations.add(condRel);
                } else {
                    // 'and' operator: update last condition
                    Map condRel = (Map) conditionRelations.get(conditionRelations.size() - 1);
                    List conditions = (List) condRel.get("conditions");
                    conditions.add(h);
                }
            } else {
                // NOTE: per Alliance request, we suppress export of any WITH fields
                //
                // non-XCO with value
                if( r.with==null ) {
                    r.with = new ArrayList<>();
                }
                r.with.add(withValue);
            }
        }

        if( !conditionRelations.isEmpty() ) {
            r.condition_relations = conditionRelations;

            if( conditionRelations.size()>2 ) {
                System.out.println("MULTI CONDRELS "+r.object+" "+r.subject);
            }
        }
        return true;
    }

    // convert human rgd ids to HGNC ids, and mouse rgd ids to MGI ids
    String transformRgdId(String with, Dao dao) throws Exception {

        if (with.startsWith("RGD:")) {
            Integer rgdId = Integer.parseInt(with.substring(4));
            RgdId id = dao.getRgdId(rgdId);
            if (id == null) {
                System.out.println("ERROR: invalid RGD ID " + with + "; skipping annotation");
                return null;
            }

            if (id.getSpeciesTypeKey() == SpeciesType.HUMAN) {
                List<XdbId> xdbIds = dao.getXdbIds(rgdId, XdbId.XDB_KEY_HGNC);
                if (xdbIds.isEmpty()) {
                    System.out.println("ERROR: cannot map " + with + " to human HGNC ID");
                    return null;
                }
                if (xdbIds.size() > 1) {
                    System.out.println("WARNING: multiple HGNC ids for " + with);
                }
                String hgncId = xdbIds.get(0).getAccId();
                return hgncId;
            } else if (id.getSpeciesTypeKey() == SpeciesType.MOUSE) {
                List<XdbId> xdbIds = dao.getXdbIds(rgdId, XdbId.XDB_KEY_MGD);
                if (xdbIds.isEmpty()) {
                    System.out.println("ERROR: cannot map " + with + " to mouse MGI ID");
                    return null;
                }
                if (xdbIds.size() > 1) {
                    System.out.println("WARNING: multiple MGI ids for " + with);
                }
                String mgiId = xdbIds.get(0).getAccId();
                return mgiId;
            } else if (id.getSpeciesTypeKey() == SpeciesType.RAT) {
                return with;
            } else {
                System.out.println("ERROR: RGD id for species other than rat,mouse,human in WITH field");
                return null;
            }
        }

        return with;
    }

    // str: string to be parsed
    // out: out[0]-extracted term; out[1]-separator before term
    // return rest of string 'str' after extracting the token
    String getNextToken(String str, String[] out) {

        if( str==null ) {
            out[0] = null;
            out[1] = null;
            return null;
        }

        int startPos = 0;
        if( str.startsWith("|") ) {
            out[1] = "|";
            startPos = 1;
        } else if( str.startsWith(",") ) {
            out[1] = ",";
            startPos = 1;
        } else {
            out[1] = null;
        }

        int endPos = str.length();

        int barPos = str.indexOf('|', startPos);
        if( barPos>=0 && barPos < endPos ) {
            endPos = barPos;
        }
        int commaPos = str.indexOf(',', startPos);
        if( commaPos>=0 && commaPos < endPos ) {
            endPos = commaPos;
        }

        out[0] = str.substring(startPos, endPos);

        if( endPos < str.length() ) {
            return str.substring(endPos);
        } else {
            return null;
        }
    }

    class GeneDiseaseAnnotation {
        public String annotation_type = "manually_curated";
        public String created_by = "RGD:curator";
        public String creation_date;
        public String data_provider;
        public String data_last_modified;
        public List<String> disease_qualifiers;
        public List<String> evidence_codes;
        public String modified_by = "RGD:curator";
        public Boolean negated = null;
        public String object; // DOID
        public String predicate; // assoc_type, one of (is_implicated_in, is_marker_for)
        public String secondary_data_provider;
        public String single_reference;
        public String subject; // HGNC ID
        public List<String> with;
        public List related_notes;
        public List condition_relations;
    }


    public void sort() {

        Collections.sort(disease_gene_ingest_set, new Comparator<GeneDiseaseAnnotation>() {
            @Override
            public int compare(GeneDiseaseAnnotation a1, GeneDiseaseAnnotation a2) {
                int r = a1.object.compareTo(a2.object);
                if( r!=0 ) {
                    return r;
                }
                return a1.subject.compareTo(a2.subject);
            }
        });
    }
}
