package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Omim;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Utils2 {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    static SimpleDateFormat sdf_agr2 = new SimpleDateFormat("yyyy-MM-dd");

    synchronized public static String formatDate(Date dt) {
        String result = sdf_agr.format(dt);
        return result;
    }

    synchronized public static String formatDate2(Date dt) {
        String result = sdf_agr2.format(dt);
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

    public static String getGeneOmimId(int geneRgdId, String doId, Dao dao) {

        try {
            List<XdbId> omimIds = dao.getXdbIds(geneRgdId, XdbId.XDB_KEY_OMIM);

            // remove phenotype OMIM ids
            if( omimIds.size()>1 ) {
                //logDaf.info("  MULTIS: remove phenotype OMIM ids for "+phenotypeOmimId);
                Iterator<XdbId> it = omimIds.iterator();
                while (it.hasNext()) {
                    XdbId id = it.next();
                    Omim omim = dao.getOmimByNr(id.getAccId());
                    if( omim==null ) {
                        System.out.println("NULL OMIM table entry for OMIM:"+id.getAccId());
                    }
                    else if (omim.getMimType().equals("phenotype") || omim.getMimType().equals("moved/removed")) {
                        it.remove();
                    }
                }
            }

            if( omimIds.size()==0 ) {
                System.out.println("NO GENE OMIM for "+doId+ ", RGD:"+geneRgdId);
                return null;
            }

            String omimId = "OMIM:"+omimIds.get(0).getAccId();

            if( omimIds.size()==1 ) {
                //logDaf.info("SINGLE GENE OMIM "+omimIds.get(0).getAccId()+" for "+phenotypeOmimId);
                return omimId;
            }

            System.out.println("MULTIPLE GENE OMIMs for "+doId + ", RGD:"+geneRgdId+" {"+ Utils.concatenate(",", omimIds, "getAccId")+"}");
            // just pick an OMIM id by random
            return omimId;

        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static BufferedWriter openWriterUTF8(String fileName) throws IOException {

        Object os;
        if (!fileName.endsWith(".gz") && !fileName.contains(".gz_")) {
            os = new FileOutputStream(fileName);
        } else {
            os = new GZIPOutputStream(new FileOutputStream(fileName));
        }

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter((OutputStream)os, "UTF-8"));
        return writer;
    }
}
