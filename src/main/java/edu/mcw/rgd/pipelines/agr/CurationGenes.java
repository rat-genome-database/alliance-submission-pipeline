package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;

import java.util.*;
import java.util.Map;

public class CurationGenes extends CurationObject {

    public List<GeneModel> gene_ingest_set = new ArrayList<>();

    public GeneModel add(Gene g, Dao dao, String curie, Set<String> canonicalProteins) throws Exception {

        GeneModel m = new GeneModel();
        m.mod_entity_id = curie;
        m.mod_internal_id = "RGD:"+g.getRgdId();
        m.taxon_curie = "NCBITaxon:" + SpeciesType.getTaxonomicId(g.getSpeciesTypeKey());
        m.gene_type_curie = Utils.NVL(g.getSoAccId(), "SO:0000704");  // if SO acc id not provided, use 'gene' SO:0000704
        m.data_provider_dto.setCrossReferenceDTO("RGD:"+g.getRgdId(), "gene", "RGD");

        if( g.getSymbol().contains("<") || g.getSymbol().contains("'") || g.getSymbol().contains("\"")) {
            System.out.println(" ### punctuation RGD:"+g.getRgdId());
        }
        Map symbolDTO = new HashMap<>();
        symbolDTO.put("display_text", g.getSymbol());
        symbolDTO.put("format_text", g.getSymbol());
        symbolDTO.put("internal", false);
        symbolDTO.put("name_type_name", "nomenclature_symbol");
        m.gene_symbol_dto = symbolDTO;

        /* not used in RGD
        Map systematicNameDTO = new HashMap<>();
        systematicNameDTO.put("display_text", g.getSymbol());
        systematicNameDTO.put("format_text", g.getSymbol());
        systematicNameDTO.put("internal", false);
        systematicNameDTO.put("name_type_name", "systematic_name");
        m.gene_systematic_name_dto = systematicNameDTO;
        */

        if( !Utils.isStringEmpty( g.getName()) ) {
            Map nameDTO = new HashMap<>();
            nameDTO.put("display_text", g.getName());
            nameDTO.put("format_text", g.getName());
            nameDTO.put("internal", false);
            nameDTO.put("name_type_name", "full_name");
            m.gene_full_name_dto = nameDTO;
        }

        RgdId id = dao.getRgdId(g.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = Utils2.formatDate(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = Utils2.formatDate(id.getLastModifiedDate());
        }

        m.gene_secondary_id_dtos = getSecondaryIdentifiers(curie, g.getRgdId(), dao);
        m.gene_synonym_dtos = getSynonyms(g.getRgdId(), dao);
        m.cross_reference_dtos = getCrossReferences(g, dao, canonicalProteins);

        synchronized(gene_ingest_set) {
            gene_ingest_set.add(m);
        }

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
        ids.addAll( dao.getXdbIds(g.getRgdId(), XdbId.XDB_KEY_OMIM) );
        ids.addAll( dao.getXdbIds(g.getRgdId(), XdbId.XDB_KEY_ENSEMBL_GENES) );
        ids.addAll( dao.getXdbIds(g.getRgdId(), XdbId.XDB_KEY_NCBI_GENE) );
        ids.addAll( dao.getXdbIds(g.getRgdId(), XdbId.XDB_KEY_HGNC) );
        ids.addAll( dao.getXdbIds(g.getRgdId(), 68) ); // RNACentral

        if( ids.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( XdbId id: ids ) {

            String curie = null;
            String pageArea = null;
            String prefix = null;

            if( id.getXdbKey()==XdbId.XDB_KEY_UNIPROT ) {
                curie = "UniProtKB:" + id.getAccId();
                prefix = "UniProtKB";
                pageArea = "protein";
                if (canonicalProteins.contains(id.getAccId())) {
                    pageArea = "canonical_protein";
                }
            }
            else if( id.getXdbKey()==XdbId.XDB_KEY_OMIM ) {
                curie = "OMIM:" + id.getAccId();
                prefix = "OMIM";
                pageArea = "gene";
            }
            else if( id.getXdbKey()==XdbId.XDB_KEY_ENSEMBL_GENES ) {
                curie = "ENSEMBL:" + id.getAccId();
                prefix = "ENSEMBL";
                pageArea = "gene";
            }
            else if( id.getXdbKey()==XdbId.XDB_KEY_NCBI_GENE ) {
                curie = "NCBI_Gene:" + id.getAccId();
                prefix = "NCBI_Gene";
                pageArea = "gene";
            }
            else if( id.getXdbKey()==XdbId.XDB_KEY_HGNC ) {
                if( id.getAccId().startsWith("HGNC:") ) {
                    curie = id.getAccId();
                    prefix = "HGNC";
                    pageArea = "gene";
                } else {
                    curie = "HGNC:" + id.getAccId();
                    prefix = "HGNC";
                    pageArea = "gene";
                }
            }
            else if( id.getXdbKey()==68 ) {
                curie = "RNAcentral:" + id.getAccId();
                prefix = "RNAcentral";
                pageArea = "gene";
            }

            // not sure why other values did not work
            pageArea = "default";

            if( curie!=null ) {
                HashMap xref = new HashMap();
                xref.put("internal", false);
                xref.put("referenced_curie", curie);
                xref.put("display_name", id.getAccId());
                xref.put("prefix", prefix);
                xref.put("page_area", pageArea);
                results.add(xref);
            }
        }
        return results;
    }

    class GeneModel {
        public String created_by_curie = "RGD";
        public List cross_reference_dtos = null;
        public DataProviderDTO data_provider_dto = new DataProviderDTO();
        public String date_created;
        public String date_updated;
        public Map gene_full_name_dto;
        public List gene_secondary_id_dtos = null;
        public Map gene_symbol_dto;
        public List gene_synonym_dtos = null;
        public Map gene_systematic_name_dto; // not used in RGD
        public String gene_type_curie;

        public boolean internal = false;
        public String mod_entity_id;
        public String mod_internal_id;
        public Boolean obsolete = null;
        public String taxon_curie;
        public String updated_by_curie = null;
    }


    public void sort() {

        sort(gene_ingest_set);
    }

    void sort(List<GeneModel> list) {

        Collections.sort(list, new Comparator<GeneModel>() {
            @Override
            public int compare(GeneModel a1, GeneModel a2) {
                return a1.mod_entity_id.compareToIgnoreCase(a2.mod_entity_id);
            }
        });
    }
}
