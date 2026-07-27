package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.util.*;

public class CurationAGM extends CurationObject {

    public List<AgmModel> agm_ingest_set = new ArrayList<>();

    // when true, emit the current schema-v2.16.0 form: a nomenclature symbol (agm_symbol_dto) plus,
    // when available, the sparse strain full name (agm_full_name_dto).
    // when false, revert to the legacy form: submit the always-populated strain symbol as full_name,
    // and emit no separate nomenclature symbol.
    public boolean emitBothFullNamesAndSymbols = false;

    public AgmModel add(Strain s, Dao dao, String curie) throws Exception {

        AgmModel m = new AgmModel();
        m.primary_external_id = curie;

        // we call this to find malformed strain symbols
        String friendlyName1 = getHumanFriendlyName(s.getSymbol(), s.getRgdId());
        String friendlyName2 = getHumanFriendlyName(s.getTaglessStrainSymbol(), s.getRgdId());

        if( emitBothFullNamesAndSymbols ) {
            // current schema-v2.16.0 form: nomenclature symbol + (when available) strain full name
            HashMap agmSymbolDto = new HashMap();
            agmSymbolDto.put("name_type_name", "nomenclature_symbol");
            agmSymbolDto.put("display_text", s.getSymbol());
            agmSymbolDto.put("format_text", taglessSymbolFormatText(s));
            agmSymbolDto.put("internal", false);
            m.agm_symbol_dto = agmSymbolDto;

            // strain full name, when available
            if( !Utils.isStringEmpty(s.getName()) ) {
                HashMap agmFullNameDto = new HashMap();
                agmFullNameDto.put("name_type_name", "full_name");
                agmFullNameDto.put("display_text", s.getName());
                agmFullNameDto.put("format_text", generateTaglessSymbol(s.getName()));
                agmFullNameDto.put("internal", false);
                m.agm_full_name_dto = agmFullNameDto;
            }
        } else {
            // legacy form (requested Jul 2026): submit the always-populated strain symbol as full_name,
            // so the Alliance public site (which displays full_name) has a name for every RGD strain;
            // no separate nomenclature symbol is emitted
            HashMap agmFullNameDto = new HashMap();
            agmFullNameDto.put("name_type_name", "full_name");
            agmFullNameDto.put("display_text", s.getSymbol());
            agmFullNameDto.put("format_text", taglessSymbolFormatText(s));
            agmFullNameDto.put("internal", false);
            m.agm_full_name_dto = agmFullNameDto;
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

    // the always-populated name field depends on the mode: agm_symbol_dto when emitting both,
    // agm_full_name_dto (which holds the symbol) in legacy mode
    String sortName(AgmModel m) {
        HashMap dto = emitBothFullNamesAndSymbols ? m.agm_symbol_dto : m.agm_full_name_dto;
        return dto.get("display_text").toString();
    }
}
