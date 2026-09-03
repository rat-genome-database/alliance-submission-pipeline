package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.util.*;

public class CurationAGM extends CurationObject {

    public List<AgmModel> agm_ingest_set = new ArrayList<>();

    public AgmModel add(Strain s, Dao dao, String curie) throws Exception {

        AgmModel m = new AgmModel();
        m.primary_external_id = curie;

        // we call this to find malformed strain symbols
        String friendlyName1 = getHumanFriendlyName(s.getSymbol(), s.getRgdId());
        String friendlyName2 = getHumanFriendlyName(s.getTaglessStrainSymbol(), s.getRgdId());

        // per agreement with AGR disease WG (Sep 2026): submit the always-populated strain symbol
        // as full_name, so the Alliance public site (which displays full_name) shows the symbol
        // for every RGD strain
        HashMap agmFullNameDto = new HashMap();
        agmFullNameDto.put("name_type_name", "full_name");
        agmFullNameDto.put("display_text", s.getSymbol());
        agmFullNameDto.put("format_text", taglessSymbolFormatText(s));
        agmFullNameDto.put("internal", false);
        m.agm_full_name_dto = agmFullNameDto;

        // the strain full name, when available, is submitted as a synonym of type 'full_name'
        if( !Utils.isStringEmpty(s.getName()) ) {
            HashMap synonymDto = new HashMap();
            synonymDto.put("name_type_name", "full_name");
            synonymDto.put("display_text", s.getName());
            synonymDto.put("format_text", generateTaglessSymbol(s.getName()));
            synonymDto.put("internal", false);
            List synonymDtos = new ArrayList();
            synonymDtos.add(synonymDto);
            m.agm_synonym_dtos = synonymDtos;
        }

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

    // strip html tags from a symbol/name so it can be used as 'format_text'
    // '<sup>'/'</sup>' become '^['/']'; all other tags (f.e. '<i>','</i>') are removed
    // (same logic as data-qc-pipeline QC.generateTaglessSymbol, which builds tagless_strain_symbol)
    String generateTaglessSymbol( String symbol ) {

        for( ;; ) {

            int tagStartPos = symbol.indexOf('<');
            int tagStopPos = symbol.indexOf('>');
            if( tagStartPos<0 || tagStopPos<0 || tagStartPos>tagStopPos ) {
                break;
            }

            // we have a tag!
            String tag = symbol.substring(tagStartPos+1, tagStopPos).trim().toLowerCase();
            String replacement = "";
            if( tag.equals("sup") ) {
                replacement = "^[";
            }
            else if( tag.equals("/sup") ) {
                replacement = "]";
            }

            symbol = symbol.substring(0, tagStartPos) + replacement + symbol.substring(tagStopPos+1);
        }

        return symbol;
    }

    // format_text (tagless symbol) is a required field and must be non-empty; prefer the strain's
    // tagless_strain_symbol, but fall back to computing it from the symbol when that DB field is
    // not yet populated (f.e. before the RGD tagless-symbol pipeline has run for a new strain)
    String taglessSymbolFormatText(Strain s) {
        String formatText = s.getTaglessStrainSymbol();
        if( Utils.isStringEmpty(formatText) ) {
            formatText = generateTaglessSymbol(s.getSymbol());
        }
        return formatText;
    }

    class AgmModel {
        public HashMap agm_full_name_dto = null;
        public List agm_secondary_id_dtos = null;
        public HashMap agm_symbol_dto = null;
        public List agm_synonym_dtos = null;
        public String created_by_curie = null;
        public List cross_reference_dtos = null;
        public DataProviderDTO data_provider_dto = new DataProviderDTO();
        public String date_created;
        public String date_updated;
        public boolean internal = false;
        public List note_dtos = null;
        public Boolean obsolete = null;

        // as of Jan 22, 2025: mod_entity_id or mod_internal_id should not be used
        public String primary_external_id = null;

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
                return sortName(a1).compareToIgnoreCase(sortName(a2));
            }
        });
    }

    // agm_full_name_dto is always populated (it holds the strain symbol)
    String sortName(AgmModel m) {
        return m.agm_full_name_dto.get("display_text").toString();
    }
}
