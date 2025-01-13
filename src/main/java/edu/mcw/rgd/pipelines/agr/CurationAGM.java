package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;

import java.util.*;

public class CurationAGM extends CurationObject {

    public List<AgmModel> agm_ingest_set = new ArrayList<>();

    public AgmModel add(Strain s, Dao dao, String curie) throws Exception {

        AgmModel m = new AgmModel();
        m.mod_entity_id = curie;
        //m.mod_internal_id = curie;
        m.name = s.getSymbol();

        // we call this to find malformed strain symbols
        String friendlyName = getHumanFriendlyName(m.name, s.getRgdId());

        m.agm_secondary_id_dtos = getSecondaryIdentifiers(curie, s.getRgdId(), dao);

        m.data_provider_dto.setCrossReferenceDTO(curie, "strain", "RGD");

        RgdId id = dao.getRgdId(s.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = Utils2.formatDate(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = Utils2.formatDate(id.getLastModifiedDate());
        }

        //m.genomic_location_association_dtos = getGenomicLocationAssociation_DTOs(s.getRgdId(), SpeciesType.RAT, dao, curie);

        agm_ingest_set.add(m);

        return m;
    }

    class AgmModel {
        public List agm_secondary_id_dtos = null;
        public List component_dtos = null;
        public String created_by_curie = null;
        public List cross_reference_dtos = null;
        public DataProviderDTO data_provider_dto = new DataProviderDTO();
        public String date_created;
        public String date_updated;
        public boolean internal = false;

        // only one of these attributes can be submitted: mod_entity_id or mod_internal_id
        public String mod_entity_id = null;
        //public String mod_internal_id = null;

        public String name;
        public Boolean obsolete = null;
        public List references_curies = null;
        public String subtype_name = "strain";
        public String taxon_curie = "NCBITaxon:10116";
        public String updated_by_curie = null;
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
