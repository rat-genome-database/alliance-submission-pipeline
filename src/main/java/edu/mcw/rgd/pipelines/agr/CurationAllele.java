package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CurationAllele extends CurationObject {

    public String linkml_version = "v1.11.0";
    public String alliance_member_release_version = null;

    public List<AlleleModel> allele_ingest_set = new ArrayList<>();

    public AlleleModel add(Gene a, Dao dao, String curie) throws Exception {

        AlleleModel m = new AlleleModel();
        m.curie = curie;

        // if allele name is not available, we use allele symbol instead
        String alleleName = Utils.NVL(a.getName(), a.getSymbol());

        Map nameMap = new HashMap();
        nameMap.put("display_text", alleleName);
        nameMap.put("format_text", getHumanFriendlyName(alleleName, a.getRgdId()));
        nameMap.put("name_type_name", "full_name");
        nameMap.put("internal", false);
        m.allele_full_name_dto = nameMap;

        Map symbolMap = new HashMap();
        symbolMap.put("display_text", a.getSymbol());
        symbolMap.put("format_text", getHumanFriendlyName(a.getSymbol(), a.getRgdId()));
        symbolMap.put("name_type_name", "nomenclature_symbol");
        symbolMap.put("internal", false);
        m.allele_symbol_dto = symbolMap;

        RgdId id = dao.getRgdId(a.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = Utils2.formatDate(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = Utils2.formatDate(id.getLastModifiedDate());
        }

        m.allele_secondary_id_dtos = getSecondaryIdentifiers(curie, a.getRgdId(), dao);
        m.reference_curies = getReferences(a.getRgdId(), dao);

        // HACK: suppress nomen events -- per AGR request 1/16/2024
        //     nomen events can be submitted next month, during next submission
        m.allele_nomenclature_event_dtos = getNomenEvents(a.getRgdId(), dao);

        m.note_dtos = getAlleleNotes_DTO(a, dao);
        m.allele_synonym_dtos = getSynonyms(a.getRgdId(), dao);

        m.data_provider_dto = new DataProviderDTO();
        m.data_provider_dto.setCrossReferenceDTO("RGD:"+a.getRgdId(), "allele", "RGD");

        allele_ingest_set.add(m);

        return m;
    }

    List getAlleleNotes_DTO(Gene g, Dao dao) throws Exception {
        List<Note> notes = dao.getNotes(g.getRgdId());
        Map<String, Set<Integer>> map = mergeNotes(notes);

        List results = new ArrayList();
        for( Map.Entry<String, Set<Integer>> entry: map.entrySet() ) {

            String internal = entry.getKey().substring(0, 1);
            boolean isInternal = internal.equals("1");
            String freeText = entry.getKey().substring(1);

            HashMap noteDto = new HashMap();
            noteDto.put("note_type_name", "comment");
            noteDto.put("free_text", freeText);
            noteDto.put("internal", isInternal);

            Set<Integer> refs = entry.getValue();
            if( !refs.isEmpty() ) {

                List<String> evidenceCuries = new ArrayList<>();

                for( int refRgdId: refs ) {
                    evidenceCuries.add("RGD:"+refRgdId);

                    // optional PMID
                    String pmid = dao.getPmid(refRgdId);
                    if( !Utils.isStringEmpty(pmid) ) {
                        evidenceCuries.add("PMID:"+pmid);
                    }
                }

                noteDto.put("evidence_curies", evidenceCuries);
            }

            results.add(noteDto);
        }

        if( !Utils.isStringEmpty(g.getDescription()) ) {
            HashMap noteDto = new HashMap();
            noteDto.put("note_type_name", g.getDescription().contains("mutation")?"mutation_description":"comment");
            noteDto.put("free_text", g.getDescription());
            noteDto.put("internal", false);
            results.add(noteDto);
        }

        if( results.isEmpty() ) {
            return null;
        }
        return results;
    }

    static Map<String, Integer> _hitCount = new ConcurrentHashMap<>();

    List getNomenEvents(int rgdId, Dao dao) throws Exception {
        List<NomenclatureEvent> events = dao.getNomenEvents(rgdId);
        if( events.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( NomenclatureEvent event: events ) {

            String nomenEventName = null;
            switch( event.getDesc() ) {
                case "gene nomenclature is perpetuated to the allele symbol"
                        -> nomenEventName = "gene_nomenclature_extended_to_allele";
                case "Name changed", "Name and Type changed (type changed from [gene] to [allele])"
                        -> nomenEventName = "name_updated";
                case "Symbol changed", "Symbol updated", "Symbol and Type changed (type changed from [allele] to [gene])"
                        -> nomenEventName = "symbol_updated";
                case "'predicted' is removed", "Name and Symbol changed"
                        -> nomenEventName = "symbol_and_name_updated";
                case "Symbol and Name status set to provisional"
                        -> nomenEventName = "symbol_and_name_status_set_to_provisional";
                case "Symbol and Name status set to approved"
                        -> nomenEventName = "symbol_and_name_status_set_to_approved";
                case "Symbol and name updated at request of researcher"
                        -> nomenEventName = "symbol_and_name_updated_at_request_of_researcher";
                case "Data Merged", "Type changed (type changed from [gene] to [allele])"
                        -> nomenEventName = "data_merged";
            }

            if( event.getDesc().equals("changed") || event.getDesc().equals("Nomenclature updated to reflect human and mouse nomenclature") ) {
                String oldName = Utils.NVL(event.getPreviousName(), event.getName());
                String oldSymbol = Utils.NVL(event.getPreviousSymbol(), event.getSymbol());
                boolean nameChanged = !oldName.equalsIgnoreCase(event.getName());
                boolean symbolChanged = !oldSymbol.equalsIgnoreCase(event.getSymbol());
                if( nameChanged && symbolChanged ) {
                    nomenEventName = "symbol_and_name_updated";
                } else {
                    if( nameChanged ) {
                        nomenEventName = "name_updated";
                    }
                    if( symbolChanged ) {
                        nomenEventName = "symbol_updated";
                    }

                    if( !nameChanged && !symbolChanged ) {
                        System.out.println("nomen event conflict");
                    }
                }
            }

            if( nomenEventName==null ) {
                System.out.println("*** *** nomen event skipped *** ****");
                continue;
            }
            synchronized(_hitCount) {
                Integer cnt = _hitCount.get(nomenEventName);
                if (cnt == null) {
                    cnt = 0;
                }
                _hitCount.put(nomenEventName, cnt+1);
            }

            Map eventMap = new HashMap();
            eventMap.put("internal", false);
            eventMap.put("nomenclature_event_name", nomenEventName);
            eventMap.put("created_by_curie", "RGD");
            eventMap.put("date_created", Utils2.formatDate(event.getEventDate()));
            int refRgdId = dao.getRefRgdId(Integer.parseInt(event.getRefKey()));
            if( refRgdId!=0 ) {
                List<String> refRgdIds = new ArrayList<>();
                refRgdIds.add("RGD:"+refRgdId);
                eventMap.put("evidence_curies", refRgdIds);
            }
            results.add(eventMap);
        }

        return results;
    }

    List getSynonyms(int rgdId, Dao dao) throws Exception {
        List<Alias> aliases = dao.getAliases(rgdId);
        if( aliases.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( Alias a: aliases ) {
            HashMap synonym = new HashMap();
            synonym.put("internal", false);
            synonym.put("display_text", a.getValue());
            synonym.put("format_text", getHumanFriendlyName(a.getValue(), rgdId));
            if( a.getTypeName().equals("old_gene_symbol") ) {
                synonym.put("name_type_name", "nomenclature_symbol");
            }
            else if( a.getTypeName().equals("old_gene_name") ) {
                synonym.put("name_type_name", "full_name");
            }
            else {
                synonym.put("name_type_name", "unspecified");
            }
            results.add(synonym);
        }
        return results;
    }

    List getReferences(int rgdId, Dao dao) throws Exception {
        // extract curated ref rgd ids
        List<Reference> refs = dao.getReferenceAssociations(rgdId);
        if( refs.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( Reference r: refs ) {
            results.add("RGD:"+r.getRgdId());
        }

        // and pubmed ids if available
        List<XdbId> xdbIds = dao.getCuratedPubmedIds(rgdId);
        for( XdbId xdbId: xdbIds ) {
            results.add("PMID:"+xdbId.getAccId());
        }

        return results;
    }


    class AlleleModel {
        public Object allele_database_status_dto = null;
        public Map allele_full_name_dto;
        public List allele_functional_impact_dtos;
        public Object allele_germline_transmission_status_dto;
        public List allele_inheritance_mode_dtos;
        public List allele_mutation_type_dtos;
        public List allele_nomenclature_event_dtos;
        public List allele_secondary_id_dtos = null;
        public Map allele_symbol_dto;
        public List allele_synonym_dtos = null;

        public String created_by_curie = "RGD";
        public List cross_reference_dtos = null;
        public String curie;
        public DataProviderDTO data_provider_dto;
        public String date_created;
        public String date_updated;
        public String in_collection_name = null;
        public boolean internal = false;
        public Boolean is_extinct = null;
        public List note_dtos;
        public Boolean obsolete = null;
        public List<String> reference_curies = null;
        public String taxon_curie = "NCBITaxon:10116";
    }


    public void sort() {

        sort(allele_ingest_set);

        System.out.println("NOMEN EVENT TYPES:");
        System.out.println("=====");
        for( Map.Entry<String,Integer> entry: _hitCount.entrySet() ) {
            System.out.println(entry.getKey()+" : "+entry.getValue());
        }
    }

    void sort(List<AlleleModel> list) {

        Collections.sort(list, new Comparator<AlleleModel>() {
            @Override
            public int compare(AlleleModel a1, AlleleModel a2) {
                return a1.curie.compareToIgnoreCase(a2.curie);
            }
        });
    }
}
