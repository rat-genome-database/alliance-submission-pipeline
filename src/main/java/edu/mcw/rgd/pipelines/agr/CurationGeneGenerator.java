package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.dao.impl.GeneDAO;
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
    private String phenotypeFile;

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

        // for human, load the set of HGNC ids that have phenotype data so that
        // gene/phenotypes xrefs are emitted only for genes that actually have phenotypes;
        // the phenotypes file is mandatory: the run is aborted if it is missing or does not load properly
        if( speciesTypeKey == SpeciesType.HUMAN ) {
            curationGenes.setPhenotypeHgncIds(loadHumanPhenotypeHgncIds());
        }

        // setup a JSON object array to collect all CurationGene objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        AtomicInteger obsoleteGeneCount = new AtomicInteger(0);

        Set<String> canonicalProteins = dao.getCanonicalProteins(speciesTypeKey);

        List<Gene> genes = dao.getAllGenes(speciesTypeKey);

        log.info("  genes: "+genes.size());
        genes.stream().parallel().forEach( g -> {
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

    /**
     * Load the HGNC ids having phenotype data from the AGR phenotypes JSON file (HUMAN only).
     * The file may be plain or gzipped (by .gz extension); if the configured name is not found,
     * its sibling name (with/without .gz) is tried before giving up.
     * <p>
     * The phenotypes file is required: if it is not configured, not found, unreadable, or yields
     * no HGNC ids, an error is written to the status log and to the console, and the run is aborted.
     */
    Set<String> loadHumanPhenotypeHgncIds() throws Exception {

        if( phenotypeFile == null || phenotypeFile.isEmpty() ) {
            throw abort("HUMAN phenotypes file is NOT CONFIGURED: set property 'phenotypeFile' of bean 'curationGeneGenerator' in AppConfigure.xml");
        }

        File file = new File(phenotypeFile);
        if( !file.exists() ) {
            // the AGR phenotypes file may be delivered gzipped or plain: try the sibling name before giving up
            File sibling = new File(phenotypeFile.endsWith(".gz")
                    ? phenotypeFile.substring(0, phenotypeFile.length()-3)
                    : phenotypeFile+".gz");
            if( !sibling.exists() ) {
                throw abort("HUMAN phenotypes file NOT FOUND: "+file.getAbsolutePath()+" (also tried "+sibling.getAbsolutePath()+")");
            }
            log.info("  HUMAN phenotypes file "+file.getAbsolutePath()+" not found; using "+sibling.getAbsolutePath());
            file = sibling;
        }
        if( !file.isFile() || !file.canRead() ) {
            throw abort("HUMAN phenotypes file is NOT A READABLE FILE: "+file.getAbsolutePath());
        }

        Set<String> phenoHgncIds;
        try {
            phenoHgncIds = CurationGenes.loadPhenotypeHgncIds(file.getAbsolutePath());
        } catch( Exception e ) {
            throw abort("HUMAN phenotypes file COULD NOT BE LOADED: "+file.getAbsolutePath()+" ("+e+")");
        }

        if( phenoHgncIds.isEmpty() ) {
            throw abort("HUMAN phenotypes file DID NOT LOAD PROPERLY: no HGNC ids found in "+file.getAbsolutePath()
                    +" (file size: "+Utils.formatThousands(file.length())+" bytes)");
        }

        log.info("  loaded HGNC ids with phenotype data from "+file.getAbsolutePath()+": "+Utils.formatThousands(phenoHgncIds.size()));
        return phenoHgncIds;
    }

    /** write the error to the status log and to the console, and return the exception that aborts the run */
    Exception abort(String msg) {
        String fullMsg = "ERROR: "+msg+" -- ABORTING HUMAN GENE file generation";
        log.error(fullMsg);
        return new Exception(fullMsg);
    }


    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public String getPhenotypeFile() {
        return phenotypeFile;
    }

    public void setPhenotypeFile(String phenotypeFile) {
        this.phenotypeFile = phenotypeFile;
    }
}
