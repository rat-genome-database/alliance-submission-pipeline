package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.EvidenceCode;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CurationDaf {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public List<GeneDiseaseAnnotation> disease_gene_ingest_set = new ArrayList<>();

    public void addGeneDiseaseAnnotation(Annotation a, Dao dao, Map<Integer,String> geneRgdId2HgncIdMap) throws Exception {


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

        r.subject = geneRgdId2HgncIdMap.get(a.getAnnotatedObjectRgdId());

        if( a.getWithInfo()!=null ) {
            System.out.println("WITH "+a.getWithInfo());
            String[] with = a.getWithInfo().split("[\\,\\|\\ ]");
            r.with = new ArrayList<>();
            for( String w: with ) {
                if( w.startsWith("RGD:") ) {
                    String idStr = w.substring(4).trim();
                    int rgdId = Integer.parseInt(idStr);
                    String hgncId = geneRgdId2HgncIdMap.get(rgdId);
                    if( hgncId!=null ) {
                        r.with.add(hgncId);
                    } else {
                        r.with.add(w);
                    }
                }
            }
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
            case "onset":
            case "sexual_dimorphism":
            case "susceptibility":
            case "resistance":
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

            case "disease_progression":
            case "disease progression":
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
    }


    public void sort() {
    }
}
