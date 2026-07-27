package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Omim;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.Logger;

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

    /**
     * Back up an existing curation output file before it is regenerated. Generic helper that any
     * module producing an Alliance curation JSON file can call at startup.
     * <p>
     * Reads the top-level 'alliance_member_release_version' field from the current file and writes a
     * gzip-compressed copy to data/&lt;name&gt;.&lt;version&gt;.json.gz
     * (f.e. 'CURATION_AGM-RAT.json' with release version 'v2026-07-07' becomes
     * 'data/CURATION_AGM-RAT.v2026-07-07.json.gz').
     * <p>
     * No-op (logged) if the file does not yet exist or has no release version.
     *
     * @param jsonFileName name of the output file about to be regenerated (f.e. "CURATION_AGM-RAT.json")
     * @param log logger for status messages
     */
    public static void backupOutputFile(String jsonFileName, Logger log) throws IOException {

        File src = new File(jsonFileName);
        if( !src.exists() ) {
            log.info("  no existing "+jsonFileName+" to back up");
            return;
        }

        String releaseVersion = readAllianceMemberReleaseVersion(src);
        if( releaseVersion==null ) {
            log.warn("  skipping backup of "+jsonFileName+": no 'alliance_member_release_version' field found");
            return;
        }

        // strip the trailing '.json' and build 'data/<name>.<version>.json.gz'
        String baseName = src.getName();
        if( baseName.endsWith(".json") ) {
            baseName = baseName.substring(0, baseName.length()-".json".length());
        }
        File backupDir = new File("data");
        backupDir.mkdirs();
        File backupFile = new File(backupDir, baseName+"."+releaseVersion+".json.gz");

        try( InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(backupFile))) ) {
            byte[] buf = new byte[65536];
            int bytesRead;
            while( (bytesRead=in.read(buf))>0 ) {
                out.write(buf, 0, bytesRead);
            }
        }

        log.info("  backed up "+jsonFileName+" to "+backupFile.getPath());
    }

    // read the top-level 'alliance_member_release_version' field via streaming, without loading the
    // (potentially large) file into memory; the field appears near the top, before the ingest set array
    static String readAllianceMemberReleaseVersion(File src) throws IOException {

        try( JsonParser p = new ObjectMapper().getFactory().createParser(src) ) {
            if( p.nextToken()!=JsonToken.START_OBJECT ) {
                return null;
            }
            while( p.nextToken()==JsonToken.FIELD_NAME ) {
                String field = p.getCurrentName();
                p.nextToken(); // advance to the field value
                if( "alliance_member_release_version".equals(field) ) {
                    return p.getValueAsString();
                }
                p.skipChildren(); // skip nested arrays/objects we do not care about
            }
        }
        return null;
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
