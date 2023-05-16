package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;

import java.util.*;

public class CurationAGM extends CurationObject {

    public String linkml_version = "v1.7.0";

    public List<AgmModel> agm_ingest_set = new ArrayList<>();

    public AgmModel add(Strain s, Dao dao, String curie) throws Exception {

        AgmModel m = new AgmModel();
        m.curie = curie;
        m.name = s.getSymbol();

        // we call this to find malformed strain symbols
        String friendlyName = getHumanFriendlyName(m.name, s.getRgdId());

        m.data_provider_dto.setCrossReferenceDTO(curie, "strain", "RGD");

        RgdId id = dao.getRgdId(s.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = Utils2.formatDate(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = Utils2.formatDate(id.getLastModifiedDate());
        }

        m.secondary_identifiers = getSecondaryIdentifiers(curie, s.getRgdId(), dao);
        //m.synonyms = getSynonyms(s.getRgdId(), dao);
        m.genomic_location_association_dtos = getGenomicLocationAssociation_DTOs(s.getRgdId(), SpeciesType.RAT, dao, curie);

        agm_ingest_set.add(m);

        return m;
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
            synonym.put("synonym", a.getValue());
            results.add(synonym);
        }
        return results;
    }

    class AgmModel {
        public List component_dtos = null;
        public List cross_reference_dtos = null;
        public String curie;
        public DataProviderDTO data_provider_dto = new DataProviderDTO();
        public String date_created;
        public String date_updated;
        public List genomic_location_association_dtos = null;
        public boolean internal = false;
        public String name;
        public Boolean obsolete = null;
        public List references_curies = null;
        public List<String> secondary_identifiers = null;
        public String subtype_name = "strain";
        //public List synonyms = null;
        public String taxon_curie = "NCBITaxon:10116";
    }


    public void sort() {

        sort(agm_ingest_set);
    }

    void sort(List<AgmModel> list) {

        Collections.sort(list, new Comparator<AgmModel>() {
            @Override
            public int compare(AgmModel a1, AgmModel a2) {
                return a1.name.compareToIgnoreCase(a2.name);
            }
        });
    }
}
