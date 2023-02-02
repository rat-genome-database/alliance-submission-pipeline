package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.mapping.MapManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurationObject {

    static SimpleDateFormat sdf_agr = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    static public Map<Integer, String> loadHgncIdMap(Dao dao) throws Exception {
        Map<Integer, String> rgdId2HgncIdMap = new HashMap<>();
        List<XdbId> xdbIds = dao.getActiveXdbIds(XdbId.XDB_KEY_HGNC, RgdId.OBJECT_KEY_GENES);
        for( XdbId xdbId: xdbIds ) {
            String accIds = rgdId2HgncIdMap.get(xdbId.getRgdId());
            if( accIds==null ) {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), xdbId.getAccId());
            } else {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), accIds+","+xdbId.getAccId());
            }
        }
        return rgdId2HgncIdMap;
    }

    List getGenomicLocations(int rgdId, int speciesTypeKey, Dao dao) throws Exception {

        int mapKey1 = 0, mapKey2 = 0; // NCBI/Ensembl assemblies
        if( speciesTypeKey== SpeciesType.HUMAN ) {
            mapKey1 = 38;
            mapKey2 = 40;
        } else {
            mapKey1 = 372;
            mapKey2 = 373;
        }
        String assembly = MapManager.getInstance().getMap(mapKey1).getName();

        List<MapData> mds = getLoci(rgdId, mapKey1, mapKey2, dao);
        if( mds.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( MapData md: mds ) {
            HashMap loc = new HashMap();
            loc.put("end", md.getStopPos());
            loc.put("has_assembly", assembly);
            loc.put("internal", false);
            loc.put("object", md.getChromosome());
            loc.put("predicate", "RGD");
            loc.put("start", md.getStartPos());
            loc.put("subject", "RGD");
            results.add(loc);
        }
        return results;
    }

    List getGenomicLocations_DTO(int rgdId, int speciesTypeKey, Dao dao, String geneCurie) throws Exception {

        int mapKey1 = 0, mapKey2 = 0; // NCBI/Ensembl assemblies
        if( speciesTypeKey== SpeciesType.HUMAN ) {
            mapKey1 = 38;
            mapKey2 = 40;
        } else {
            mapKey1 = 372;
            mapKey2 = 373;
        }
        String assembly = MapManager.getInstance().getMap(mapKey1).getName();

        List<MapData> mds = getLoci(rgdId, mapKey1, mapKey2, dao);
        if( mds.isEmpty() ) {
            return null;
        }
        List results = new ArrayList();
        for( MapData md: mds ) {
            HashMap loc = new HashMap();
            loc.put("end", md.getStopPos());
            loc.put("start", md.getStartPos());
            loc.put("internal", false);
            loc.put("assembly_curie", assembly);
            loc.put("chromosome_curie", md.getChromosome());
            loc.put("predicate", "localizes_to");
            loc.put("genomic_entity_curie", geneCurie);
            results.add(loc);
        }
        return results;
    }

    // get gene loci from NCBI and Ensembl assemblies, and merge the loci that overlap
    List<MapData> getLoci(int rgdId, int mapKey1, int mapKey2, Dao dao) throws Exception {

        List<MapData> mds1 = dao.getMapData(rgdId, mapKey1);
        List<MapData> mds2 = dao.getMapData(rgdId, mapKey2);

        List<MapData> mds = new ArrayList<>();
        mergeLoci(mds, mds1);
        mergeLoci(mds, mds2);

        return mds;
    }

    void mergeLoci(List<MapData> mds1, List<MapData> mds2) {

        for( MapData md2: mds2 ) {

            // look for overlapping positions
            boolean overlappingPos = false;
            for( MapData md1: mds1 ) {
                if( !md1.getChromosome().equals(md2.getChromosome()) ) {
                    continue;
                }
                // same chr:
                if( md2.getStartPos()<=md1.getStopPos()  &&  md1.getStartPos()<=md2.getStopPos() ) {
                    // positions overlap: update 'md1'
                    md1.setStartPos(Math.min(md1.getStartPos(), md2.getStartPos()));
                    md1.setStopPos(Math.max(md1.getStopPos(), md2.getStopPos()));
                    overlappingPos = true;
                    break;
                }
            }
            if( !overlappingPos ) {
                mds1.add(md2);
            }
        }
    }

    List getSecondaryIdentifiers(String curie, int rgdId, Dao dao) throws Exception {

        List<String> secondaryIds = new ArrayList<>();
        try {
            if (curie.startsWith("HGNC")) {
                secondaryIds.add("RGD:" + rgdId);
            }
            List<Integer> secondaryRgdIds = dao.getOldRgdIds(rgdId);
            if (!secondaryRgdIds.isEmpty()) {
                for (Integer secRgdId : secondaryRgdIds) {
                    secondaryIds.add("RGD:" + secRgdId);
                }
            }
        } catch(Exception e) {
            String msg = "getOldRgdIds problem for "+curie+" RGD:"+rgdId;
            System.out.println(msg);
            Logger log = LogManager.getLogger("status");
            log.error(msg);
        }

        return secondaryIds.isEmpty() ? null : secondaryIds;
    }

}
