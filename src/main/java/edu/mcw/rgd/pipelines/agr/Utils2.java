package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils2 {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    synchronized public static String formatDate(Date dt) {
        String result = sdf_agr.format(dt);
        return result;
    }

    public static String getGeneAssocType(String evidence, int rgdObjectKey, boolean isAllele) {

        if( isAllele ) {
            return "is_implicated_in";
        }

        // for genes evidence code must be a manual evidence code
        String assocType = null;
        if( rgdObjectKey==1 ) {
            switch (evidence) {
                case "IEP":
                    assocType = "is_marker_for";
                    break;
                case "IAGP":
                case "IMP":
                case "IDA":
                case "IGI":
                    assocType = "is_implicated_in";
                    break;
            }
        }
        else
        if( rgdObjectKey==5 ) {
            // for strains: skip annotations with IDA, IEA, IEP, QTM or TAS evidence codes
            switch (evidence) {
                case "IDA":
                case "IEA":
                case "IEP":
                case "QTM":
                case "TAS":
                    return null;
                default:
                    assocType = "is_model_of";
                    break;
            }
        }
        return assocType;
    }

    public static String toJson( Object o ) throws IOException {

        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        String out = json.writerWithDefaultPrettyPrinter().writeValueAsString(o);

        return out;
    }
}
