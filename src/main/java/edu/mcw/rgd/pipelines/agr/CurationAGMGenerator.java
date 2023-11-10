package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class CurationAGMGenerator {

    private Dao dao;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        try {
            createAgmFile(SpeciesType.RAT);

        } catch( Exception e ) {
            Utils.printStackTrace(e, log);
            throw new Exception(e);
        }

        log.info("===");
        log.info("");
    }

    void createAgmFile(int speciesTypeKey) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey).toUpperCase();

        log.info("START "+speciesName+" AGM file");

        CurationAGM curationAGM = new CurationAGM();
        curationAGM.alliance_member_release_version = "v"+Utils2.formatDate2(new Date());

        // setup a JSON object array to collect all CurationAGM objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        List<Strain> strains = dao.getActiveStrains();
        log.info("  strains: "+strains.size());
        for( Strain s: strains ) {

            String curie = null;
            if( speciesTypeKey==SpeciesType.RAT ) {
                curie = "RGD:"+s.getRgdId();
            } else {
                continue;
            }

            CurationAGM.AgmModel m = curationAGM.add(s, dao, curie);
        }

        // sort data, alphabetically by object symbols
        curationAGM.sort();

        // dump records to a file in JSON format
        try {
            String jsonFileName = "CURATION_AGM-"+speciesName+".json";
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(curationAGM));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" agm file:  strains="+curationAGM.agm_ingest_set.size());
        log.info("");
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
