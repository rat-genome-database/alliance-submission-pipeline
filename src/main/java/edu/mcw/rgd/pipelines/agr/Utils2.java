package edu.mcw.rgd.pipelines.agr;

public class Utils2 {

    public static String getGeneAssocType(String evidence) {

        // for genes evidence code must be a manual evidence code
        String assocType = null;
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
        return assocType;
    }
}
