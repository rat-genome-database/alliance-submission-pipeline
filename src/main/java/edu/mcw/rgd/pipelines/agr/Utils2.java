package edu.mcw.rgd.pipelines.agr;

public class Utils2 {

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
}
