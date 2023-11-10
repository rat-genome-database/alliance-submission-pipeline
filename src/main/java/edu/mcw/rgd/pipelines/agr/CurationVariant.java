package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.util.Map;
import java.util.*;

// Note: there is a column VARIANT.LAST_MODIFIED_DATE
// should it be used as 'date_updated' in submission?

public class CurationVariant extends CurationObject {

    public String linkml_version = "v1.11.0";
    public String alliance_member_release_version = null;

    public List<VariantModel> variant_ingest_set = new ArrayList<>();

    public VariantModel add(RgdVariant v, Dao dao, String curie) throws Exception {

        VariantModel m = new VariantModel();
        m.curie = curie;
        m.taxon_curie = "NCBITaxon:" + SpeciesType.getTaxonomicId(v.getSpeciesTypeKey());
        m.variant_type_curie = v.getType();
        m.data_provider_dto.setCrossReferenceDTO("RGD:"+v.getRgdId(), "allele", "RGD");

        List noteDtos = new ArrayList();

        if( !Utils.isStringEmpty(v.getName()) ) {

            Map dto = new HashMap<>();
            dto.put("free_text", "Name: " + v.getName());
            dto.put("internal", false);
            dto.put("note_type_name", "comment");

            noteDtos.add(dto);
        }

        if( !Utils.isStringEmpty(v.getDescription()) ) {

            Map dto = new HashMap<>();
            dto.put("free_text", "Description: "+v.getDescription());
            dto.put("internal", false);
            dto.put("note_type_name", "comment");

            noteDtos.add(dto);
        }

        if( !Utils.isStringEmpty(v.getNotes()) ) {

            Map dto = new HashMap<>();
            dto.put("free_text", "Note: "+v.getNotes());
            dto.put("internal", false);
            dto.put("note_type_name", "comment");

            noteDtos.add(dto);
        }
        m.note_dtos = noteDtos;

        RgdId id = dao.getRgdId(v.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = Utils2.formatDate(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = Utils2.formatDate(id.getLastModifiedDate());
        }

        synchronized(variant_ingest_set) {
            variant_ingest_set.add(m);
        }

        return m;
    }


    class VariantModel {
        public String created_by_curie = "RGD:curator";
        public List cross_reference_dtos = null;
        public String curie;
        public DataProviderDTO data_provider_dto = new DataProviderDTO();
        public String date_created;
        public String date_updated;
        public boolean internal = false;
        public List note_dtos = null;
        public Boolean obsolete = null;

        // Curie of the SOTerm (child of SO:0001576 - transcript_variant) that describes the consequence of the variant,
        // as stated in the source reference when no transcript ID is provided. Since a curator would determine variant location
        // and consequences relative to at least one specific genome assembly, transcript and/or polypeptide,
        // no slot for curated general consequence is provided.
        public String source_general_consequence_curie;

        public String taxon_curie;
        public String updated_by_curie = "RGD:curator";
        public String variant_status_name = "public";
        public String variant_type_curie; // SO:xxxxxxx
    }


    public void sort() {

        sort(variant_ingest_set);
    }

    void sort(List<VariantModel> list) {

        Collections.sort(list, new Comparator<VariantModel>() {
            @Override
            public int compare(VariantModel a1, VariantModel a2) {
                return a1.curie.compareToIgnoreCase(a2.curie);
            }
        });
    }
}
