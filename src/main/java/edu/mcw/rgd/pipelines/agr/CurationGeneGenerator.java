package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class CurationGeneGenerator {

    private Dao dao;
    private Map<Integer, String> rgdId2HgncIdMap;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        try {
            rgdId2HgncIdMap = CurationObject.loadHgncIdMap(dao);

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

        AtomicInteger obsoleteGeneCount = new AtomicInteger(0);

        Set<String> canonicalProteins = dao.getCanonicalProteins(speciesTypeKey);

        List<Gene> genes = dao.getAllGenes(speciesTypeKey);
        log.info("  genes: "+genes.size());
        genes.parallelStream().forEach( g -> {
            String curie = null;
            if (speciesTypeKey == SpeciesType.RAT) {
                curie = "RGD:" + g.getRgdId();
            } else if (speciesTypeKey == SpeciesType.HUMAN) {
                String hgncId = rgdId2HgncIdMap.get(g.getRgdId());
                if (hgncId == null) {
                    return;
                }
                curie = hgncId;
            }

            try {
                CurationGenes.GeneModel m = curationGenes.add(g, dao, curie, canonicalProteins);

                if (m.obsolete != null && m.obsolete == true) {
                    obsoleteGeneCount.incrementAndGet();
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        // sort data, alphabetically by object symbols
        curationGenes.sort();

        // dump DafAnnotation records to a file in JSON format
        try {
            String jsonFileName = "CURATION_GENES-"+speciesName+".json";
            BufferedWriter jsonWriter = Utils2.openWriterUTF8(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(curationGenes));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" gene file:  genes="+curationGenes.gene_ingest_set.size());
        log.info("   obsolete gene count: "+obsoleteGeneCount.get());
        log.info("");
    }


    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
