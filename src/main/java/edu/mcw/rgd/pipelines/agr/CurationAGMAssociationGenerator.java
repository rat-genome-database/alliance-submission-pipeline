package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.Strain;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CurationAGMAssociationGenerator {

    private Dao dao;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        try {
            createAGMAssociationFile(SpeciesType.RAT);

        } catch( Exception e ) {
            Utils.printStackTrace(e, log);
            throw new Exception(e);
        }

        log.info("===");
        log.info("");
    }

    void createAGMAssociationFile(int speciesTypeKey) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey).toUpperCase();

        log.info("START "+speciesName+" agm association file");

        CurationAGMAssociation curationAGMAssociations = new CurationAGMAssociation();

        // setup a JSON object array to collect all objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        List<Strain> strains = dao.getActiveStrains();
        Collections.shuffle(strains);

        log.info("  agms: "+strains.size());
        for( Strain s: strains ) {

            curationAGMAssociations.add(s, dao);
        }

        // sort data
        curationAGMAssociations.sort();

        // dump records to a file in JSON format
        try {
            String jsonFileName = "CURATION_AGM_ASSOCIATIONS-"+speciesName+".json";
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(curationAGMAssociations));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" agm associations file:  count="+curationAGMAssociations.agm_allele_association_ingest_set.size());
        log.info("");
    }

    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
