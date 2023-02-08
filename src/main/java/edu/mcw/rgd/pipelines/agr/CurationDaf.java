package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map;

public class CurationDaf {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public String linkml_version = "1.5.0";
    public List<DiseaseAnnotation> disease_agm_ingest_set = new ArrayList<>();
    public List<DiseaseAnnotation> disease_allele_ingest_set = new ArrayList<>();
    public List<DiseaseAnnotation> disease_gene_ingest_set = new ArrayList<>();

    public void addDiseaseAnnotation(Annotation a, Dao dao, Map<Integer, String> geneRgdId2HgncIdMap, int speciesTypeKey, boolean isAllele) throws Exception {

        DiseaseAnnotation r = new DiseaseAnnotation();
        r.date_created = sdf_agr.format(a.getCreatedDate());
        if( a.getLastModifiedDate()!=null ) {
            r.date_updated = sdf_agr.format(a.getLastModifiedDate());
        }
        r.disease_qualifiers = getDiseaseQualifiers(a);
        r.evidence_codes = getEvidenceCodes(a.getEvidence());
        r.negated = getNegatedValue(a);
        r.object = a.getTermAcc();
        r.predicate = Utils2.getGeneAssocType(a.getEvidence(), a.getRgdObjectKey(), isAllele);

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
        // TODO: temporarily suppressed export of WITH field
        r.with = null;

        if( !Utils.isStringEmpty(a.getNotes()) ) {
            r.related_notes = new ArrayList();
            HashMap noteMap = new HashMap();
            noteMap.put("internal", false);
            noteMap.put("note_type", "disease_note");
            noteMap.put("free_text", a.getNotes().trim());
            r.related_notes.add(noteMap);
        }

        if( isAllele ) {
            List<Gene> assertedGenes = dao.getGenesForAllele(a.getAnnotatedObjectRgdId());
            if( assertedGenes.size()!=1 ) {
                System.out.println("ERROR: allele associated with "+assertedGenes.size()+" genes; ALLELE_RGD_ID="+a.getAnnotatedObjectRgdId());
            } else {
                r.asserted_gene = "RGD:"+assertedGenes.get(0).getRgdId();
            }

            disease_allele_ingest_set.add(r);
        }
        else if( a.getRgdObjectKey()==1 ) {
            disease_gene_ingest_set.add(r);
        } else {
            // AGM
            List<Integer> assertedAlleles = dao.getGeneAllelesForStrain(a.getAnnotatedObjectRgdId());
            if( assertedAlleles.size()!=1 ) {
                if( assertedAlleles.size()>1 ) {
                    System.out.println("ERROR: strain associated with " + assertedAlleles.size() + " alleles; STRAIN_RGD_ID=" + a.getAnnotatedObjectRgdId());
                }
            } else {
                int alleleRgdId = assertedAlleles.get(0);
                r.asserted_allele = "RGD:"+alleleRgdId;

                List<Gene> assertedGenes = dao.getGenesForAllele(alleleRgdId);
                if( assertedGenes.size()!=1 ) {
                    if( assertedGenes.size()>1 ) {
                        System.out.println("ERROR: AGM allele associated with " + assertedGenes.size() + " genes; ALLELE_RGD_ID=" + a.getAnnotatedObjectRgdId());
                    }
                } else {
                    r.asserted_gene = "RGD:"+assertedGenes.get(0).getRgdId();
                }
            }

            disease_agm_ingest_set.add(r);
        }
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

    boolean handleWithInfo(Annotation a, DiseaseAnnotation r, Dao dao) throws Exception {

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
                h.put("internal", false);

                if (or) {
                    Map condRel = new HashMap();
                    condRel.put("condition_relation_type", condRelType);
                    condRel.put("internal", false);

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

    class DiseaseAnnotation {
        public String annotation_type = "manually_curated";
        public String asserted_allele; // for AGMs
        public String asserted_gene; // for AGMs and alleles
        public List condition_relations;
        public String created_by = "RGD:curator";
        public String date_created;
        public String data_provider;
        public String date_updated;
        public List<String> disease_qualifiers;
        public List<String> evidence;
        public List<String> evidence_codes;
        public String inferred_gene;
        public Boolean internal = false;
        public Boolean negated = null;
        public String object; // DOID
        public String predicate; // assoc_type, one of (is_implicated_in, is_marker_for)
        public List related_notes;
        public String secondary_data_provider;
        public String single_reference;
        public String subject; // HGNC ID
        public String updated_by = "RGD:curator";
        public List<String> with;
    }


    public void sort() {

        sort(disease_agm_ingest_set);
        sort(disease_allele_ingest_set);
        sort(disease_gene_ingest_set);
    }

    void sort(List<DiseaseAnnotation> list) {

        Collections.sort(list, new Comparator<DiseaseAnnotation>() {
            @Override
            public int compare(DiseaseAnnotation a1, DiseaseAnnotation a2) {
                int r = a1.object.compareTo(a2.object);
                if( r!=0 ) {
                    return r;
                }
                return a1.subject.compareTo(a2.subject);
            }
        });
    }

    public void removeDiseaseAnnotsSameAsAlleleAnnots() {

        if( !disease_allele_ingest_set.isEmpty() ) {
            System.out.println("=== removing gene disease annotations same as allele annotations===");


            Map<String, Integer> diseaseGeneMap = new HashMap<>();
            for (int i = 0; i < disease_gene_ingest_set.size(); i++) {
                DiseaseAnnotation ga = disease_gene_ingest_set.get(i);
                String key = createKey(ga, ga.subject);
                Integer old = diseaseGeneMap.put(key, i);
                if (old != null) {
                    DiseaseAnnotation gaOld = disease_gene_ingest_set.get(old);
                    System.out.println("ERROR: problem in removeDiseaseAnnotsSameAsAlleleAnnots");
                }
            }

            Set<Integer> geneAnnotIndexesForDelete = new TreeSet<>(); // use TreeSet to store indexes in numeric order

            System.out.println(" disease alleles annots to process: " + disease_allele_ingest_set.size());

            for (DiseaseAnnotation aa : disease_allele_ingest_set) {
                if (aa.asserted_gene != null) {
                    String alleleKey = createKey(aa, aa.asserted_gene);
                    Integer geneAnnotIndex = diseaseGeneMap.get(alleleKey);
                    if (geneAnnotIndex != null) {
                        geneAnnotIndexesForDelete.add(-geneAnnotIndex); // store negative indexes to enforce descending order
                    } else {
                        System.out.println(" unexpected 1");
                    }
                }
            }

            for (int geneAnnotIndex : geneAnnotIndexesForDelete) {
                disease_gene_ingest_set.remove(-geneAnnotIndex);
            }

            System.out.println(" disease gene annots deleted: " + geneAnnotIndexesForDelete.size());
        }
    }

    public void removeDiseaseAnnotsSameAsAGMAnnots() {
        System.out.println("=== removing gene disease annotations same as AGM annotations===");
        System.out.println(" disease AGM annots to process: "+disease_agm_ingest_set.size());

        // remove disease gene annots same as disease AGM annots
        //
        if( !disease_agm_ingest_set.isEmpty() )
        {
            Map<String, Integer> diseaseGeneMap = new HashMap<>();
            for (int i = 0; i < disease_gene_ingest_set.size(); i++) {
                DiseaseAnnotation ga = disease_gene_ingest_set.get(i);
                String key = createKey2(ga, ga.subject);
                Integer old = diseaseGeneMap.put(key, i);
                if (old != null) {
                    DiseaseAnnotation gaOld = disease_gene_ingest_set.get(old);
                    System.out.println("ERROR: problem 1 in removeDiseaseAnnotsSameAsAGMAnnots");
                }
            }

            Set<Integer> geneAnnotIndexesForDelete = new TreeSet<>(); // use TreeSet to store indexes in numeric order

            for (DiseaseAnnotation aa : disease_agm_ingest_set) {
                if( aa.asserted_gene!=null ) {
                    String alleleKey = createKey2(aa, aa.asserted_gene);
                    Integer geneAnnotIndex = diseaseGeneMap.get(alleleKey);
                    if (geneAnnotIndex != null) {
                        geneAnnotIndexesForDelete.add(-geneAnnotIndex); // store negative indexes to enforce descending order
                    }
                }
            }

            for (int geneAnnotIndex : geneAnnotIndexesForDelete) {
                disease_gene_ingest_set.remove(-geneAnnotIndex);
            }

            System.out.println(" AGM: disease gene annots deleted: " + geneAnnotIndexesForDelete.size());
        }

        // remove disease allele annots same as disease AGM annots
        //
        if( !disease_agm_ingest_set.isEmpty() )
        {
            Map<String, Integer> diseaseAlleleMap = new HashMap<>();
            for (int i = 0; i < disease_allele_ingest_set.size(); i++) {
                DiseaseAnnotation ga = disease_allele_ingest_set.get(i);
                String key = createKey2(ga, ga.subject);
                Integer old = diseaseAlleleMap.put(key, i);
                if (old != null) {
                    DiseaseAnnotation gaOld = disease_allele_ingest_set.get(old);
                    System.out.println("ERROR: problem 2 in removeDiseaseAnnotsSameAsAGMAnnots");
                }
            }

            TreeSet<Integer> alleleAnnotIndexesForDelete = new TreeSet<>(); // use TreeSet to store indexes in numeric order

            for (DiseaseAnnotation aa : disease_agm_ingest_set) {
                if( aa.asserted_allele!=null ) {
                    String alleleKey = createKey2(aa, aa.asserted_allele);
                    Integer alleleAnnotIndex = diseaseAlleleMap.get(alleleKey);
                    if (alleleAnnotIndex != null) {
                        alleleAnnotIndexesForDelete.add(-alleleAnnotIndex); // store negative indexes to enforce descending order
                    }
                }
            }

            for (int alleleAnnotIndex : alleleAnnotIndexesForDelete) {
                disease_allele_ingest_set.remove(-alleleAnnotIndex);
            }

            System.out.println(" AGM: disease allele annots deleted: " + alleleAnnotIndexesForDelete.size());
        }
    }

    String createKey(DiseaseAnnotation ga, String id) {
        String key = id+"|"+ga.object+"|"+ga.predicate+"|"+ga.data_provider+"|"+ga.single_reference+"|"+ga.negated
                +"|"+Utils.concatenate(ga.disease_qualifiers,",")
                +"|"+Utils.concatenate(ga.evidence_codes,",")
                +"|"+(ga.related_notes==null ? 0 : ga.related_notes.size())
                +"|"+(ga.condition_relations==null ? 0 : ga.condition_relations.size());
        return key;
    }

    String createKey2(DiseaseAnnotation ga, String id) {
        String key = id+"|"+ga.object+"|"+ga.data_provider+"|"+ga.single_reference+"|"+ga.negated
                +"|"+Utils.concatenate(ga.disease_qualifiers,",")
                +"|"+Utils.concatenate(ga.evidence_codes,",")
                +"|"+(ga.condition_relations==null ? 0 : ga.condition_relations.size());
        return key;
    }
}
