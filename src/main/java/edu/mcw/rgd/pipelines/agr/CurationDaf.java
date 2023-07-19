package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;

import java.io.IOException;
import java.util.*;
import java.util.Map;

public class CurationDaf {

    public String linkml_version = "v1.7.4";
    public List<AgmDiseaseAnnotation> disease_agm_ingest_set = new ArrayList<>();
    public List<AlleleDiseaseAnnotation> disease_allele_ingest_set = new ArrayList<>();
    public List<GeneDiseaseAnnotation> disease_gene_ingest_set = new ArrayList<>();

    public void addDiseaseAnnotation(Annotation a, Dao dao, Map<Integer, String> geneRgdId2HgncIdMap, int speciesTypeKey, boolean isAllele) throws Exception {

        if( isAllele ) {
            addAlleleDiseaseAnnotation(a, dao);
        }
        else if( a.getRgdObjectKey()==1 ) {
            addGeneDiseaseAnnotation(a, dao, geneRgdId2HgncIdMap, speciesTypeKey);
        } else {
            addAGMDiseaseAnnotation(a, dao);
        }
    }

    void addAGMDiseaseAnnotation(Annotation a, Dao dao) throws Exception {

        boolean isAllele = false;

        AgmDiseaseAnnotation r = new AgmDiseaseAnnotation();

        r.agm_curie = "RGD:"+a.getAnnotatedObjectRgdId();

        List<Integer> assertedAlleles = dao.getGeneAllelesForStrain(a.getAnnotatedObjectRgdId());
        if( assertedAlleles.size()!=1 ) {
            if( assertedAlleles.size()>1 ) {
                System.out.println("ERROR: strain associated with " + assertedAlleles.size() + " alleles; STRAIN_RGD_ID=" + a.getAnnotatedObjectRgdId());
            }
        } else {
            int alleleRgdId = assertedAlleles.get(0);
            r.asserted_allele_curie = "RGD:"+alleleRgdId;

            List<Gene> assertedGenes = dao.getGenesForAllele(alleleRgdId);
            if( !assertedGenes.isEmpty() ) {
                r.asserted_gene_curies = new ArrayList<>();
                for( Gene g: assertedGenes ) {
                    r.asserted_gene_curies.add("RGD:" + g.getRgdId());
                }
            }
        }

        // process common fields
        if( processDTO(a, dao, isAllele, r, SpeciesType.RAT) ) {
            disease_agm_ingest_set.add(r);
        }
    }

    void addAlleleDiseaseAnnotation(Annotation a, Dao dao) throws Exception {

        boolean isAllele = true;

        AlleleDiseaseAnnotation r = new AlleleDiseaseAnnotation();

        r.allele_curie = "RGD:"+a.getAnnotatedObjectRgdId();

        List<Gene> assertedGenes = dao.getGenesForAllele(a.getAnnotatedObjectRgdId());
        if( !assertedGenes.isEmpty() ) {
            r.asserted_gene_curies = new ArrayList<>();
            for( Gene g: assertedGenes ) {
                r.asserted_gene_curies.add("RGD:" + g.getRgdId());
            }
        }

        // process common fields
        if( processDTO(a, dao, isAllele, r, SpeciesType.RAT) ) {
            disease_allele_ingest_set.add(r);
        }
    }

    void addGeneDiseaseAnnotation(Annotation a, Dao dao, Map<Integer, String> geneRgdId2HgncIdMap, int speciesTypeKey) throws Exception {

        boolean isAllele = false;

        GeneDiseaseAnnotation r = new GeneDiseaseAnnotation();

        if( speciesTypeKey== SpeciesType.HUMAN ) {
            r.gene_curie = geneRgdId2HgncIdMap.get(a.getAnnotatedObjectRgdId());
        } else if( speciesTypeKey==SpeciesType.RAT ) {
            r.gene_curie = "RGD:"+a.getAnnotatedObjectRgdId();
        }

        // process common fields
        if( processDTO(a, dao, isAllele, r, speciesTypeKey) ) {
            disease_gene_ingest_set.add(r);
        }
    }

    boolean processDTO(Annotation a, Dao dao, boolean isAllele, DiseaseAnnotation_DTO r, int speciesTypeKey) throws Exception {

        r.date_created = Utils2.formatDate(a.getCreatedDate());
        if( a.getLastModifiedDate()!=null ) {
            r.date_updated = Utils2.formatDate(a.getLastModifiedDate());
        }

        String alliancePage = speciesTypeKey==SpeciesType.RAT ? "disease/rat" : speciesTypeKey==SpeciesType.HUMAN ? "disease/human" : "disease/all";
        if( a.getDataSrc().equals("OMIM") ) {
            // OMIM via RGD
            r.data_provider_dto = new DataProviderDTO();
            r.data_provider_dto.source_organization_abbreviation = "OMIM";
            String omimGeneId = Utils2.getGeneOmimId(a.getAnnotatedObjectRgdId(), a.getTermAcc(), dao);
            if( omimGeneId!=null ) {
                r.data_provider_dto.setCrossReferenceDTO( omimGeneId, "gene", "OMIM");
            }

            r.secondary_data_provider_dto = new DataProviderDTO(); // "RGD"
            r.secondary_data_provider_dto.setCrossReferenceDTO( a.getTermAcc(), alliancePage, "RGD");
        } else {
            r.data_provider_dto = new DataProviderDTO(); // "RGD"
            r.data_provider_dto.setCrossReferenceDTO( a.getTermAcc(), alliancePage, "RGD");
        }

        r.disease_qualifier_names = getDiseaseQualifiers(a, dao);
        r.disease_relation_name = Utils2.getGeneAssocType(a.getEvidence(), a.getRgdObjectKey(), isAllele);

        r.do_term_curie = a.getTermAcc();
        // exclude non-DO term accessions
        if( !r.do_term_curie.startsWith("DOID:") ) {
            return false;
        }

        r.evidence_code_curies = getEvidenceCodes(a.getEvidence());
        r.negated = getNegatedValue(a);

        String freeText = Utils.defaultString(a.getNotes()).trim();
        if( freeText.length()>0 ) {
            HashMap noteDto = new HashMap();
            noteDto.put("note_type_name", "disease_note");
            noteDto.put("free_text", freeText);
            noteDto.put("internal", false);

            List notes = new ArrayList();
            notes.add(noteDto);
            r.note_dtos = notes;
        }

        String pmid = dao.getPmid(a.getRefRgdId());
        if( pmid==null ) {
            r.reference_curie = "RGD:"+a.getRefRgdId();
        } else {
            r.reference_curie = "PMID:"+pmid;
        }

        List conditionRelationDtos = new ArrayList();
        List withGeneCuries = new ArrayList();
        boolean isNegated = r.negated==null ? false : r.negated;
        r.handleWithInfo_DTO(a, isNegated, dao, conditionRelationDtos, withGeneCuries);

        if( !conditionRelationDtos.isEmpty() ) {
            r.condition_relation_dtos = conditionRelationDtos;
        }
        if( !withGeneCuries.isEmpty() ) {
            r.with_gene_curies = withGeneCuries;
        }

        // special rules for processing IGI annotations
        if( Utils.stringsAreEqual(a.getEvidence(), "IGI") ) {
            if( r.with_gene_curies!=null && !r.with_gene_curies.isEmpty() ) {
                if( r.with_gene_curies.size()>1 ) {
                    throw new Exception("multiple ids in WITH field for IGI annotations");
                }

                // qualifier='ameliorates' => disease_genetic_modifier_relation_name='ameliorated_by'
                // qualifier='treatment' => skip annotation
                // qualifier=null or other => disease_genetic_modifier_relation_name='exacerbated_by'
                String qualifier = Utils.NVL(a.getQualifier(), "").trim();
                if( qualifier.equals("treatment") ) {
                    return false;
                }

                r.disease_genetic_modifier_curies = r.with_gene_curies;
                r.with_gene_curies = null;
                if( qualifier.equals("ameliorates") ) {
                    r.disease_genetic_modifier_relation_name = "ameliorated_by";
                } else {
                    r.disease_genetic_modifier_relation_name = "exacerbated_by";
                }
            }
        }
        return true;
    }

    List<String> getDiseaseQualifiers(Annotation a, Dao dao) {

        if( a.getQualifier()==null ) {
            return null;
        }

        List<String> qualifiers = null;

        String rgdQualifier = a.getQualifier().trim();
        String allianceQualifier = dao.getDiseaseQualifierMappings().get(rgdQualifier);
        if( allianceQualifier!=null ) {
            qualifiers = new ArrayList<>();
            qualifiers.add(allianceQualifier);
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

    class AgmDiseaseAnnotation extends DiseaseAnnotation_DTO {
        public String agm_curie;
        public String asserted_allele_curie;
        public List<String> asserted_gene_curies;
        public String inferred_allele_curie; // not used
        public String inferred_gene_curie;   // not used

        String getCurie() {
            return agm_curie;
        }
    }

    class AlleleDiseaseAnnotation extends DiseaseAnnotation_DTO {
        public String allele_curie;
        public List<String> asserted_gene_curies;

        String getCurie() {
            return allele_curie;
        }
    }

    class GeneDiseaseAnnotation extends DiseaseAnnotation_DTO {
        public String gene_curie;

        String getCurie() {
            return gene_curie;
        }
    }

    abstract class DiseaseAnnotation_DTO extends CurationObject {

        abstract String getCurie();

        public String annotation_type_name; // not used
        public List condition_relation_dtos;
        public String created_by_curie = "RGD:curator";

        public DataProviderDTO data_provider_dto;

        public String date_created;
        public String date_updated;
        public List<String> disease_genetic_modifier_curies;
        public String disease_genetic_modifier_relation_name;
        public List<String> disease_qualifier_names;
        public String disease_relation_name; // assoc_type, one of (is_implicated_in, is_marker_for)
        public String do_term_curie;
        public List<String> evidence_code_curies;
        public List<String> evidence_curies; // not used
        public String genetic_sex_name;      // not used
        public String inferred_gene_curie;   // not used
        public Boolean internal = false;
        public String mod_entity_id;         // not used
        public Boolean negated = null;
        public List note_dtos;
        public String reference_curie;

        public DataProviderDTO secondary_data_provider_dto;

        public String updated_by_curie = "RGD:curator";
        public List<String> with_gene_curies;
    }

    void removeDuplicatesFromDiseaseGenes() throws IOException {

        System.out.println(" start removeDuplicatesFromDiseaseGenes ...");

        int duplicatesRemoved = 0;

        // the objects should be already sorted by calling sort()
        for( int i=0; i<disease_gene_ingest_set.size()-1; i++ ) {

            GeneDiseaseAnnotation a1 = disease_gene_ingest_set.get(i);
            GeneDiseaseAnnotation a2 = disease_gene_ingest_set.get(i+1);

            String dtCreated = a1.date_created;
            String dtUpdated = a1.date_updated;
            a1.date_created = null;
            a1.date_updated = null;
            String json1 = Utils2.toJson(a1);
            a1.date_created = dtCreated;
            a1.date_updated = dtUpdated;

            dtCreated = a2.date_created;
            dtUpdated = a2.date_updated;
            a2.date_created = null;
            a2.date_updated = null;
            String json2 = Utils2.toJson(a2);
            a2.date_created = dtCreated;
            a2.date_updated = dtUpdated;

            if( json1.equals(json2) ) {
                //System.out.println(" xxx duplicate annotation via json equality");
                disease_gene_ingest_set.set(i, null);
                duplicatesRemoved++;
            }
        }

        if( duplicatesRemoved>0 ) {
            for( int i=disease_gene_ingest_set.size()-1; i>=0; i-- ) {
                if( disease_gene_ingest_set.get(i)==null ) {
                    disease_gene_ingest_set.remove(i);
                }
            }
        }

        System.out.println(" total duplicate annotations via json equality removed: "+duplicatesRemoved);
        System.out.println(" disease_gene_ingest_set size: "+disease_gene_ingest_set.size());
    }

    public void sort() {

        Comparator_DTO dto = new Comparator_DTO();

        Collections.sort(disease_agm_ingest_set, dto);
        Collections.sort(disease_allele_ingest_set, dto);
        Collections.sort(disease_gene_ingest_set, dto);
    }

    class Comparator_DTO implements Comparator<DiseaseAnnotation_DTO> {
        @Override
        public int compare(DiseaseAnnotation_DTO a1, DiseaseAnnotation_DTO a2) {
            int r = a1.getCurie().compareTo(a2.getCurie());
            if( r!=0 ) {
                return r;
            }
            r = a1.do_term_curie.compareTo(a2.do_term_curie);
            if( r!=0 ) {
                return r;
            }
            return a1.reference_curie.compareTo(a2.reference_curie);
        }
    }

    public void removeDiseaseAnnotsSameAsAlleleAnnots() {

        if( !disease_allele_ingest_set.isEmpty() ) {
            System.out.println("=== removing gene disease annotations same as allele annotations===");


            Map<String, Integer> diseaseGeneMap = new HashMap<>();
            for (int i = 0; i < disease_gene_ingest_set.size(); i++) {
                GeneDiseaseAnnotation ga = disease_gene_ingest_set.get(i);
                String key = createKey(ga, ga.gene_curie);
                Integer old = diseaseGeneMap.put(key, i);
                if (old != null) {
                    GeneDiseaseAnnotation gaOld = disease_gene_ingest_set.get(old);
                    System.out.println("ERROR: problem in removeDiseaseAnnotsSameAsAlleleAnnots");
                }
            }

            Set<Integer> geneAnnotIndexesForDelete = new TreeSet<>(); // use TreeSet to store indexes in numeric order

            System.out.println(" disease alleles annots to process: " + disease_allele_ingest_set.size());

            for (AlleleDiseaseAnnotation aa : disease_allele_ingest_set) {
                if (aa.asserted_gene_curies != null) {
                    for( String assertedGeneCurie: aa.asserted_gene_curies ) {
                        String alleleKey = createKey(aa, assertedGeneCurie);
                        Integer geneAnnotIndex = diseaseGeneMap.get(alleleKey);
                        if (geneAnnotIndex != null) {
                            geneAnnotIndexesForDelete.add(-geneAnnotIndex); // store negative indexes to enforce descending order
                        } else {
                            System.out.println(" unexpected 1");
                        }
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
                GeneDiseaseAnnotation ga = disease_gene_ingest_set.get(i);
                String key = createKey2(ga, ga.getCurie());
                Integer old = diseaseGeneMap.put(key, i);
                if (old != null) {
                    GeneDiseaseAnnotation gaOld = disease_gene_ingest_set.get(old);
                    System.out.println("ERROR: problem 1 in removeDiseaseAnnotsSameAsAGMAnnots");
                }
            }

            Set<Integer> geneAnnotIndexesForDelete = new TreeSet<>(); // use TreeSet to store indexes in numeric order

            for (AgmDiseaseAnnotation aa : disease_agm_ingest_set) {
                if( aa.asserted_gene_curies!=null ) {
                    for( String assertedGeneCurie: aa.asserted_gene_curies ) {
                        String alleleKey = createKey2(aa, assertedGeneCurie);
                        Integer geneAnnotIndex = diseaseGeneMap.get(alleleKey);
                        if (geneAnnotIndex != null) {
                            geneAnnotIndexesForDelete.add(-geneAnnotIndex); // store negative indexes to enforce descending order
                        }
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
                AlleleDiseaseAnnotation ga = disease_allele_ingest_set.get(i);
                String key = createKey2(ga, ga.getCurie());
                Integer old = diseaseAlleleMap.put(key, i);
                if (old != null) {
                    AlleleDiseaseAnnotation gaOld = disease_allele_ingest_set.get(old);
                    System.out.println("ERROR: problem 2 in removeDiseaseAnnotsSameAsAGMAnnots");
                }
            }

            TreeSet<Integer> alleleAnnotIndexesForDelete = new TreeSet<>(); // use TreeSet to store indexes in numeric order

            for (AgmDiseaseAnnotation aa : disease_agm_ingest_set) {
                if( aa.asserted_allele_curie!=null ) {
                    String alleleKey = createKey2(aa, aa.asserted_allele_curie);
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

    String createKey(DiseaseAnnotation_DTO ga, String id) {
        String key = id+"|"+ga.getCurie()+"|"+ga.do_term_curie+"|"+ga.data_provider_dto.source_organization_abbreviation+"|"+ga.reference_curie+"|"+ga.negated
                +"|"+Utils.concatenate(ga.disease_qualifier_names,",")
                +"|"+Utils.concatenate(ga.evidence_code_curies,",")
                +"|"+(ga.note_dtos==null ? 0 : ga.note_dtos.size())
                +"|"+(ga.condition_relation_dtos==null ? 0 : ga.condition_relation_dtos.size());
        return key;
    }

    String createKey2(DiseaseAnnotation_DTO ga, String id) {
        String key = id+"|"+ga.getCurie()+"|"+ga.do_term_curie+"|"+ga.reference_curie+"|"+ga.negated
                +"|"+Utils.concatenate(ga.disease_qualifier_names,",")
                +"|"+Utils.concatenate(ga.evidence_code_curies,",")
                +"|"+(ga.condition_relation_dtos==null ? 0 : ga.condition_relation_dtos.size());
        return key;
    }
}
