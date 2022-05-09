package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.mapping.MapManager;

import java.text.SimpleDateFormat;
import java.util.*;

public class CurationGenes {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

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

        if( curie.startsWith("HGNC") ) {
            List<String> secondaryIds = new ArrayList<>();
            secondaryIds.add("RGD:"+g.getRgdId());
            m.secondary_identifiers = secondaryIds;
        }

        m.synonyms = getSynonyms(g.getRgdId(), dao);
        m.genomic_locations = getGenomicLocations(g, dao);
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

    List getGenomicLocations(Gene g, Dao dao) throws Exception {

        int mapKey1 = 0, mapKey2 = 0; // NCBI/Ensembl assemblies
        if( g.getSpeciesTypeKey()==SpeciesType.HUMAN ) {
            mapKey1 = 38;
            mapKey2 = 40;
        } else {
            mapKey1 = 372;
            mapKey2 = 373;
        }
        String assembly = MapManager.getInstance().getMap(mapKey1).getName();

        List<MapData> mds = getLoci(g.getRgdId(), mapKey1, mapKey2, dao);
        if( mds.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( MapData md: mds ) {
            HashMap loc = new HashMap();
            loc.put("end", md.getStopPos());
            loc.put("has_assembly", assembly);
            loc.put("internal", false);
            loc.put("object", md.getChromosome());
            loc.put("predicate", "RGD");
            loc.put("start", md.getStartPos());
            loc.put("subject", "RGD");
            results.add(loc);
        }
        return results;
    }

    // get gene loci from NCBI and Ensembl assemblies, and merge the loci that overlap
    List<MapData> getLoci(int rgdId, int mapKey1, int mapKey2, Dao dao) throws Exception {

        List<MapData> mds1 = dao.getMapData(rgdId, mapKey1);
        List<MapData> mds2 = dao.getMapData(rgdId, mapKey2);

        List<MapData> mds = new ArrayList<>();
        mergeLoci(mds, mds1);
        mergeLoci(mds, mds2);

        return mds;
    }

    void mergeLoci(List<MapData> mds1, List<MapData> mds2) {

        for( MapData md2: mds2 ) {

            // look for overlapping positions
            boolean overlappingPos = false;
            for( MapData md1: mds1 ) {
                if( !md1.getChromosome().equals(md2.getChromosome()) ) {
                    continue;
                }
                // same chr:
                if( md2.getStartPos()<=md1.getStopPos()  &&  md1.getStartPos()<=md2.getStopPos() ) {
                    // positions overlap: update 'md1'
                    md1.setStartPos(Math.min(md1.getStartPos(), md2.getStartPos()));
                    md1.setStopPos(Math.max(md1.getStopPos(), md2.getStopPos()));
                    overlappingPos = true;
                    break;
                }
            }
            if( !overlappingPos ) {
                mds1.add(md2);
            }
        }
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
            xref.put("pageAreas", pageAreas);
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
