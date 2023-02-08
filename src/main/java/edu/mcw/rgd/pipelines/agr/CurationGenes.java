package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;

import java.util.*;
import java.util.Map;

public class CurationGenes extends CurationObject {

    public String linkml_version = "1.5.0";

    public List<GeneModel> gene_ingest_set = new ArrayList<>();

    public GeneModel add(Gene g, Dao dao, String curie, Set<String> canonicalProteins) throws Exception {

        GeneModel m = new GeneModel();
        m.curie = curie;
        m.taxon_curie = "NCBITaxon:" + SpeciesType.getTaxonomicId(g.getSpeciesTypeKey());
        m.gene_type_curie = g.getSoAccId();

        if( g.getSymbol().contains("<") || g.getSymbol().contains("'") || g.getSymbol().contains("\"")) {
            System.out.println("aha");
        }
        Map symbolDTO = new HashMap<>();
        symbolDTO.put("display_text", g.getSymbol());
        symbolDTO.put("format_text", g.getSymbol());
        symbolDTO.put("internal", false);
        symbolDTO.put("name_type_name", "nomenclature_symbol");
        m.gene_symbol_dto = symbolDTO;

        Map systematicNameDTO = new HashMap<>();
        systematicNameDTO.put("display_text", g.getSymbol());
        systematicNameDTO.put("format_text", g.getSymbol());
        systematicNameDTO.put("internal", false);
        systematicNameDTO.put("name_type_name", "systematic_name");
        m.gene_systematic_name_dto = systematicNameDTO;

        Map nameDTO = new HashMap<>();
        nameDTO.put("display_text", g.getName());
        nameDTO.put("format_text", g.getName());
        nameDTO.put("internal", false);
        nameDTO.put("name_type_name", "full_name");
        m.gene_full_name_dto = nameDTO;

        RgdId id = dao.getRgdId(g.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = sdf_agr.format(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = sdf_agr.format(id.getLastModifiedDate());
        }

        m.secondary_identifiers = getSecondaryIdentifiers(curie, g.getRgdId(), dao);
        m.gene_synonym_dtos = getSynonyms(g.getRgdId(), dao);
        m.genomic_location_dtos = getGenomicLocations_DTO(g.getRgdId(), g.getSpeciesTypeKey(), dao, curie);
        m.cross_reference_dtos = getCrossReferences(g, dao, canonicalProteins);

        gene_ingest_set.add(m);

        return m;
    }


    final String[] aliasTypes = {"old_gene_name","old_gene_symbol"};
    List getSynonyms(int rgdId, Dao dao) throws Exception {
        List<Alias> aliases = dao.getAliases(rgdId, aliasTypes);
        if( aliases.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( Alias a: aliases ) {
            HashMap synonym = new HashMap();
            synonym.put("internal", false);
            synonym.put("display_text", a.getValue());
            synonym.put("format_text", a.getValue());
            synonym.put("name_type_name", a.getTypeName().equals("old_gene_symbol") ? "nomenclature_symbol" : "full_name");
            results.add(synonym);
        }
        return results;
    }


    List getCrossReferences(Gene g, Dao dao, Set<String> canonicalProteins) throws Exception {

        List<XdbId> ids = dao.getXdbIds(g.getRgdId(), XdbId.XDB_KEY_UNIPROT);
        if( ids.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( XdbId id: ids ) {

            String curie = "UniProtKB:"+id.getAccId();
            List pageAreas = new ArrayList();
            if( canonicalProteins.contains(id.getAccId()) ) {
                pageAreas.add("canonical_protein");
            }

            HashMap xref = new HashMap();
            xref.put("internal", false);
            xref.put("curie", curie);
            xref.put("display_name", id.getAccId());
            xref.put("prefix", "UniProtKB");
            xref.put("page_areas", pageAreas);
            results.add(xref);
        }
        return results;
    }

    class GeneModel {
        public String created_by_curie = "RGD";
        public List cross_reference_dtos = null;
        public String curie;
        public String date_created;
        public String date_updated;
        public Map gene_full_name_dto;
        public Map gene_symbol_dto;
        public List gene_synonym_dtos = null;
        public Map gene_systematic_name_dto;
        public String gene_type_curie;
        public List genomic_location_dtos = null;

        public boolean internal = false;
        public Boolean obsolete = null;
        public List<String> secondary_identifiers = null;
        public String taxon_curie;
    }


    public void sort() {

        sort(gene_ingest_set);
    }

    void sort(List<GeneModel> list) {

        Collections.sort(list, new Comparator<GeneModel>() {
            @Override
            public int compare(GeneModel a1, GeneModel a2) {
                return a1.curie.compareToIgnoreCase(a2.curie);
            }
        });
    }
}
