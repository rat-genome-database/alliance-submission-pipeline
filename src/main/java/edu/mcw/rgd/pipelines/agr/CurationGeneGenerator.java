package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class CurationGeneGenerator {

    private Dao dao;
    private Map<Integer, String> rgdId2HgncIdMap;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        try {
            loadHgncIdMap();

            createGeneFile(SpeciesType.RAT);
            createGeneFile(SpeciesType.HUMAN);

        } catch( Exception e ) {
            Utils.printStackTrace(e, log);
            throw new Exception(e);
        }

        log.info("===");
        log.info("");
    }

    void createGeneFile(int speciesTypeKey) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey).toUpperCase();

        log.info("START "+speciesName+" GENE file");

        CurationGenes curationGenes = new CurationGenes();

        // setup a JSON object array to collect all CurationGene objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        int obsoleteGeneCount = 0;

        Set<String> canonicalProteins = dao.getCanonicalProteins(speciesTypeKey);

        List<Gene> genes = dao.getAllGenes(speciesTypeKey);
        log.info("  genes: "+genes.size());
        for( Gene g: genes ) {

            String curie = null;
            if( speciesTypeKey==SpeciesType.RAT ) {
                curie = "RGD:"+g.getRgdId();
            } else if( speciesTypeKey==SpeciesType.HUMAN ) {
                String hgncId = rgdId2HgncIdMap.get(g.getRgdId());
                if( hgncId==null ) {
                    continue;
                }
                curie = hgncId;
            }

            CurationGenes.GeneModel m = curationGenes.add(g, dao, curie, canonicalProteins);

            if( m.obsolete!=null && m.obsolete==true ) {
                obsoleteGeneCount++;
            }
        }

        // sort data, alphabetically by object symbols
        curationGenes.sort();

        // dump DafAnnotation records to a file in JSON format
        try {
            String jsonFileName = "CURATION_GENES-"+speciesName+".json";
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(curationGenes));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" gene file:  genes="+curationGenes.gene_ingest_set.size());
        log.info("   obsolete gene count: "+obsoleteGeneCount);
        log.info("");
    }

    void loadHgncIdMap() throws Exception {
        rgdId2HgncIdMap = new HashMap<>();
        List<XdbId> xdbIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_HGNC, RgdId.OBJECT_KEY_GENES);
        for( XdbId xdbId: xdbIds ) {
            String accIds = rgdId2HgncIdMap.get(xdbId.getRgdId());
            if( accIds==null ) {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), xdbId.getAccId());
            } else {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), accIds+","+xdbId.getAccId());
            }
        }
    }

/*
    Collection<Annotation> getAnnotations(int speciesTypeKey) throws Exception {
        List<Annotation> annots = getDao().getAnnotationsBySpecies(speciesTypeKey, "D", "RGD");
        annots.addAll(getDao().getAnnotationsBySpecies(speciesTypeKey, "D", "OMIM"));

        int annotCount1 = annots.size();
        Collection<Annotation> annots2 = annots.parallelStream().filter(a ->
            a.getRgdObjectKey()==1 || a.getRgdObjectKey()==5  // accept only genes and strains
        ).collect(Collectors.toList());
        int annotCount2 = annots2.size();
        log.info(annotCount1+" annotations; excluded non-genes; new count: "+annotCount2);

        Collection<Annotation> annots3 = deconsolidateAnnotations(annots2);
        Collection<Annotation> annots4 = applyFilters(annots3, speciesTypeKey);
        return annots4;
    }

    Collection<Annotation> applyFilters(Collection<Annotation> annots, int speciesTypeKey) throws Exception {

        int omimPsReplacements = 0;
        List<Annotation> annots2 = new ArrayList<>(annots.size());

        for( Annotation a: annots ) {

            // for human, annotated object rgd id must map to HGNC id
            if( speciesTypeKey==SpeciesType.HUMAN ) {
                String hgncId = rgdId2HgncIdMap.get(a.getAnnotatedObjectRgdId());
                if (hgncId == null) {
                    continue;
                }
            }

            // for genes evidence code must be a manual evidence code
            String assocType = Utils2.getGeneAssocType(a.getEvidence(), a.getRgdObjectKey(), alleleRgdIds.contains(a.getAnnotatedObjectRgdId()));
            if( assocType==null ) {
                continue;
            }

            // exclude DO+ custom terms (that were added by RGD and are not present in DO ontology)
            if( a.getTermAcc().startsWith("DOID:90") && a.getTermAcc().length() == 12) {

                // perform DO+ child-to-parent PS mapping, if possible
                // see if this term could be mapped to an OMIM PS id
                String parentTermAcc = getDao().getOmimPSTermAccForChildTerm(a.getTermAcc());
                if (parentTermAcc == null) {
                    continue;
                }

                if (parentTermAcc.startsWith("DOID:90") && parentTermAcc.length() == 12) {
                    continue;
                } else {
                    // replaced custom DO+ term with a parent non-DO+ term, via OMIM:PS mapping
                    a.setTermAcc(parentTermAcc);
                    omimPsReplacements++;
                }
            }
            annots2.add(a);
        }

        log.info(annots.size()+";  excluded DO+ terms;  annotations left: "+annots2.size());
        log.info("    OMIM:PS replacements: "+omimPsReplacements);
        return annots2;
    }

*/

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
