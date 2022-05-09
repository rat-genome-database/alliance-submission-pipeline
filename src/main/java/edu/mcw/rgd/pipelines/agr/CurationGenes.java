package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;

import java.util.*;

public class CurationGenes extends CurationObject {

    public List<GeneModel> gene_ingest_set = new ArrayList<>();

    public GeneModel add(Gene g, Dao dao, String curie, Set<String> canonicalProteins) throws Exception {

        GeneModel m = new GeneModel();
        m.curie = curie;
        m.symbol = g.getSymbol();
        m.taxon = "NCBITaxon:" + SpeciesType.getTaxonomicId(g.getSpeciesTypeKey());
        m.automated_gene_description = g.getAgrDescription();
        m.gene_synopsis = g.getMergedDescription();
        m.gene_type = g.getSoAccId();
        m.name = g.getName();

        RgdId id = dao.getRgdId(g.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = sdf_agr.format(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = sdf_agr.format(id.getLastModifiedDate());
        }

        m.secondary_identifiers = getSecondaryIdentifiers(curie, g.getRgdId(), dao);
        m.synonyms = getSynonyms(g.getRgdId(), dao);
        m.genomic_locations = getGenomicLocations(g.getRgdId(), g.getSpeciesTypeKey(), dao);
        m.cross_references = getCrossReferences(g, dao, canonicalProteins);

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
            synonym.put("synonym", a.getValue());
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
        public String automated_gene_description;
        public String created_by = "RGD";
        public List cross_references = null;
        public String curie;
        public String date_created;
        public String date_updated;
        public String gene_synopsis;
        public String gene_type;
        public List genomic_locations = null;
        public boolean internal = false;
        public String name;
        public Boolean obsolete = null;
        public List<String> secondary_identifiers = null;
        public String symbol;
        public List synonyms = null;
        public String taxon;
    }


    public void sort() {

        sort(gene_ingest_set);
    }

    void sort(List<GeneModel> list) {

        Collections.sort(list, new Comparator<GeneModel>() {
            @Override
            public int compare(GeneModel a1, GeneModel a2) {
                return a1.symbol.compareToIgnoreCase(a2.symbol);
            }
        });
    }
}
