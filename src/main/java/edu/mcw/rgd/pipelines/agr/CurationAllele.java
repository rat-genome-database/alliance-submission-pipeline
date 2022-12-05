package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;

import java.util.*;

public class CurationAllele extends CurationObject {

    public String linkml_version = "1.3.2";
    public List<AlleleModel> allele_ingest_set = new ArrayList<>();

    public AlleleModel add(Gene a, Dao dao, String curie) throws Exception {

        AlleleModel m = new AlleleModel();
        m.curie = curie;
        m.name = a.getName();
        m.symbol = a.getSymbol();

        RgdId id = dao.getRgdId(a.getRgdId());
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            m.obsolete = true;
        }

        m.date_created = sdf_agr.format(id.getCreatedDate());
        if( id.getLastModifiedDate()!=null ) {
            m.date_updated = sdf_agr.format(id.getLastModifiedDate());
        }

        m.secondary_identifiers = getSecondaryIdentifiers(curie, a.getRgdId(), dao);
        m.synonyms = getSynonyms(a.getRgdId(), dao);
        m.genomic_locations = getGenomicLocations(a.getRgdId(), SpeciesType.RAT, dao);
        m.references = getReferences(a.getRgdId(), dao);

        allele_ingest_set.add(m);

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
        public String created_by = "RGD";
        public String curie;
        public String date_created;
        public String date_updated;
        public List genomic_locations = null;
        public boolean internal = false;
        public String name;
        public Boolean obsolete = null;
        public List<String> references = null;
        public List<String> secondary_identifiers = null;
        public String symbol;
        public List synonyms = null;
        public String taxon = "NCBITaxon:10116";
    }


    public void sort() {

        sort(allele_ingest_set);
    }

    void sort(List<AlleleModel> list) {

        Collections.sort(list, new Comparator<AlleleModel>() {
            @Override
            public int compare(AlleleModel a1, AlleleModel a2) {
                return a1.symbol.compareToIgnoreCase(a2.symbol);
            }
        });
    }
}
