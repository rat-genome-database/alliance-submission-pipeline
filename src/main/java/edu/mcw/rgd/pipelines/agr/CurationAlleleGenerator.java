package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.Strain;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CurationAlleleGenerator {

    private Dao dao;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        try {
            createAlleleFile(SpeciesType.RAT);

        } catch( Exception e ) {
            Utils.printStackTrace(e, log);
            throw new Exception(e);
        }

        log.info("===");
        log.info("");
    }

    void createAlleleFile(int speciesTypeKey) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey).toUpperCase();

        log.info("START "+speciesName+" allele file");

        CurationAllele curationAlleles = new CurationAllele();
        curationAlleles.alliance_member_release_version = "v"+Utils2.formatDate2(new Date());

        // setup a JSON object array to collect all CurationAllele objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);



        List<Gene> alleles = dao.getGeneAlleles(speciesTypeKey);
        Collections.shuffle(alleles);

        log.info("  alleles: "+alleles.size());
        for( Gene a: alleles ) {

            String curie = null;
            if( speciesTypeKey==SpeciesType.RAT ) {
                curie = "RGD:"+a.getRgdId();
            } else {
                continue;
            }

            CurationAllele.AlleleModel m = curationAlleles.add(a, dao, curie);
        }

        // sort data, alphabetically by object symbols
        curationAlleles.sort();

        // dump records to a file in JSON format
        try {
            String jsonFileName = "CURATION_ALLELES-"+speciesName+".json";
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(curationAlleles));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" allele file:  alleles="+curationAlleles.allele_ingest_set.size());
        log.info("");
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
