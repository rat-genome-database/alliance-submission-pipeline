package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.RgdVariant;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CurationVariantGenerator {

    private Dao dao;
    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        try {
            createVariantFile(SpeciesType.RAT);

        } catch( Exception e ) {
            Utils.printStackTrace(e, log);
            throw new Exception(e);
        }

        log.info("===");
        log.info("");
    }

    void createVariantFile(int speciesTypeKey) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey).toUpperCase();

        log.info("START "+speciesName+" GENE file");

        CurationVariant curationVariants = new CurationVariant();

        // setup a JSON object array to collect all CurationVariant objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        AtomicInteger obsoleteVariantCount = new AtomicInteger(0);

        List<RgdVariant> variants = dao.getVariantsForSpecies(speciesTypeKey);
        log.info("  variants: "+variants.size());

        variants.parallelStream().forEach( v -> {

            String curie = null;
            if (speciesTypeKey == SpeciesType.RAT) {
                curie = "RGD:" + v.getRgdId();
            }

            try {
                CurationVariant.VariantModel m = curationVariants.add(v, dao, curie);

                if (m.obsolete != null && m.obsolete == true) {
                    obsoleteVariantCount.incrementAndGet();
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

        // sort data, alphabetically by object symbols
        curationVariants.sort();

        // dump records to a file in JSON format
        try {
            String jsonFileName = "CURATION_VARIANT-"+speciesName+".json";
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(curationVariants));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" file:  variants="+curationVariants.variant_ingest_set.size());
        log.info("   obsolete variant count: "+obsoleteVariantCount.get());
        log.info("");
    }


    public Dao getDao() {
        return dao;
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }
}
