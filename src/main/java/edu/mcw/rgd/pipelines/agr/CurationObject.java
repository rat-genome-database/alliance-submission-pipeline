package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;
import edu.mcw.rgd.process.mapping.MapManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map;

public class CurationObject {

    public String linkml_version = "v2.9.1";
    public String alliance_member_release_version = "v"+Utils2.formatDate2(new Date());


    static public Map<Integer, String> loadHgncIdMap(Dao dao) throws Exception {
        Map<Integer, String> rgdId2HgncIdMap = new HashMap<>();
        List<XdbId> xdbIds = dao.getActiveXdbIds(XdbId.XDB_KEY_HGNC, RgdId.OBJECT_KEY_GENES);
        for( XdbId xdbId: xdbIds ) {
            String accIds = rgdId2HgncIdMap.get(xdbId.getRgdId());
            if( accIds==null ) {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), xdbId.getAccId());
            } else if( !accIds.contains(xdbId.getAccId()) ){
                rgdId2HgncIdMap.put(xdbId.getRgdId(), accIds+","+xdbId.getAccId());
            }
        }
        return rgdId2HgncIdMap;
    }

    // human friendly name: we replace all html tags with square brackets
    String getHumanFriendlyName(String name, int rgdId) {

        if( name==null || !name.contains("<") ) {
            return name;
        }

        String name2 = name
            .replace("<i>", "")
            .replace("</i>", "")
            .replace("<I>", "")
            .replace("</I>", "")
            .replace("<sup>", "[")
            .replace("</sup>", "]")
            .replace("<SUP>", "[")
            .replace("</SUP>", "]");

        if( name2.contains("<") ) {
            System.out.println("unhandled conversion in getHumanFriendlyName("+name+") RGD:"+rgdId);
        }
        return name2;
    }

    List getGenomicLocationAssociation_DTOs(int rgdId, int speciesTypeKey, Dao dao, String geneCurie) throws Exception {

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
            loc.put("predicate_name", "has_genomic_location");
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

        List<Map> secondaryIds = new ArrayList<>();
        try {
            if (curie.startsWith("HGNC")) {
                secondaryIds.add(getSecondaryIdData(rgdId, dao));
            }
            List<Integer> secondaryRgdIds = dao.getOldRgdIds(rgdId);
            if (!secondaryRgdIds.isEmpty()) {
                for (Integer secRgdId : secondaryRgdIds) {
                    secondaryIds.add(getSecondaryIdData(secRgdId, dao));
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

    private Map getSecondaryIdData(int rgdId, Dao dao) throws Exception {
        RgdId id = dao.getRgdId(rgdId);
        Map map = new HashMap();
        map.put("secondary_id", "RGD:"+rgdId);
        map.put("internal", false);
        if( !id.getObjectStatus().equals("ACTIVE") ) {
            map.put("obsolete", true);
        }
        if( id.getCreatedDate()!=null ) {
            map.put("date_created", Utils2.formatDate(id.getCreatedDate()));
        }
        if( id.getLastModifiedDate()!=null ) {
            map.put("date_updated", Utils2.formatDate(id.getLastModifiedDate()));
        }
        return map;
    }

    List getNotes_DTO(int rgdId, Dao dao) throws Exception {
        List<Note> notes = dao.getNotes(rgdId);
        Map<String, Set<Integer>> map = mergeNotes(notes);

        List results = new ArrayList();
        for( Map.Entry<String, Set<Integer>> entry: map.entrySet() ) {

            String internal = entry.getKey().substring(0, 1);
            boolean isInternal = internal.equals("1");
            String freeText = entry.getKey().substring(1);

            HashMap noteDto = new HashMap();
            //noteDto.put("note_type_name", n.getNotesTypeName());
            noteDto.put("note_type_name", "disease_note");
            noteDto.put("free_text", freeText);
            noteDto.put("internal", isInternal);

            Set<Integer> refs = entry.getValue();
            if( !refs.isEmpty() ) {

                List evidenceCuries = new ArrayList();

                for( int refRgdId: refs ) {
                    HashMap refMap = new HashMap();
                    refMap.put("internal", false);
                    refMap.put("curie", "RGD:"+refRgdId);
                    evidenceCuries.add(refMap);

                    // optional PMID
                    String pmid = dao.getPmid(refRgdId);
                    if( !Utils.isStringEmpty(pmid) ) {
                        refMap = new HashMap();
                        refMap.put("internal", false);
                        refMap.put("curie", "PMID:"+pmid);
                        evidenceCuries.add(refMap);
                    }
                }

                noteDto.put("evidence_curies", evidenceCuries);
            }

            results.add(noteDto);
        }
        if( results.isEmpty() ) {
            return null;
        }
        return results;
    }

    /**
     * merge notes: 'internal flag|note-text' -> Set of ref rgd ids
     * 'internal flag: 1 if internal (private), 0 if public
     * @param notes
     * @return
     */
    Map<String, Set<Integer>> mergeNotes(List<Note> notes) {

        Map<String, Set<Integer>> result = new HashMap<>();

        for( Note note: notes ) {

            if( Utils.isStringEmpty(note.getNotesTypeName()) || Utils.isStringEmpty(note.getNotes()) ) {
                continue;
            }

            String key = note.getPublicYN().equals("Y") ? "0" : "1";
            key += note.getNotes();

            Set<Integer> refs = result.get(key);
            if (refs == null) {
                refs = new HashSet<>();
                result.put(key, refs);
            }

            if( note.getRefId()!=null ) {
                refs.add(note.getRefId());
            }
        }
        return result;
    }

    boolean handleWithInfo_DTO(Annotation a, boolean isNegated, Dao dao, List conditionRelationDtos, List withGeneCuries) throws Exception {

        if( a.getWithInfo()==null ) {
            return true;
        }

        // only a subset of qualifiers is allowed
        String condRelType = null;
        if( a.getQualifier() == null ) {
            condRelType = "has_condition";
        } else if( a.getQualifier().contains("induced") || a.getQualifier().contains("induces") ) {
            condRelType = "induced_by";
        } else if( a.getQualifier().contains("treatment") || a.getQualifier().contains("ameliorates") ) {
            condRelType = "ameliorated_by";
        } else if( a.getQualifier().contains("exacerbates") ) {
            condRelType = "exacerbated_by";
        } else {
            System.out.println("UNMAPPED QUALIFIER: "+a.getQualifier());
            condRelType = "has_condition";
        }
        if( !condRelType.equals("has_condition") && isNegated ) {
            condRelType = "not_"+condRelType;
        }


        // remove all whitespace from WITH field to simplify parsing
        String withInfo = a.getWithInfo().replaceAll("\\s", "");

        // if the separator is '|', create separate conditionRelation object
        // if the separator is ',', combine conditions
        boolean or;

        // out[0]: token;  out[1]: separator before token
        String[] out = new String[2];
        String str = withInfo;
        for( ;; ) {
            str = getNextToken(str, out);
            if( out[0]==null ) {
                break;
            }

            String withValue = out[0];
            if( out[1]!=null && out[1].equals(",") ) {
                or = false;
            } else {
                or = true;
            }

            withValue = transformRgdId(withValue, dao);
            if( withValue==null ) {
                return false;
            }

            String[] conditionOntologies = {"XCO:", "CHEBI:", "UBERON:", "GO:"};
            boolean generateConditionData = false;
            for( String condOnt: conditionOntologies ) {
                if( withValue.startsWith(condOnt) ) {
                    generateConditionData = true;
                    break;
                }
            }
            if( generateConditionData ) {
                AgrExperimentalConditionMapper.Info info = AgrExperimentalConditionMapper.getInstance().getInfo(withValue);
                if (info == null) {
                    System.out.println("UNEXPECTED WITH VALUE: " + withValue);
                    return false;
                }


                HashMap h = new HashMap(); // ExperimentalConditionDTO
                h.put("condition_class_curie", info.zecoAcc);
                if (info.xcoAcc != null && info.xcoAcc.startsWith("CHEBI:")) {
                    h.put("condition_chemical_curie", info.xcoAcc);
                } else if( info.xcoAcc != null && info.xcoAcc.startsWith("UBERON:")) {
                    h.put("condition_anatomy_curie", info.xcoAcc);
                } else if( info.xcoAcc != null && info.xcoAcc.startsWith("GO:")) {
                    h.put("condition_gene_ontology_curie", info.xcoAcc);
                } else {
                    h.put("condition_id_curie", info.xcoAcc);
                }
                h.put("condition_free_text", info.conditionStatement);
                h.put("internal", false);

                if (or) {
                    Map condRel = new HashMap();
                    condRel.put("condition_relation_type_name", condRelType);
                    condRel.put("internal", false);

                    List conditions = new ArrayList();
                    condRel.put("condition_dtos", conditions);
                    conditions.add(h);

                    conditionRelationDtos.add(condRel);
                } else {
                    // 'and' operator: update last condition
                    Map condRel = (Map) conditionRelationDtos.get(conditionRelationDtos.size() - 1);
                    List conditions = (List) condRel.get("condition_dtos");
                    conditions.add(h);
                }
            } else {
                // suppress generation of 'with_info' having RGD ids
                if( !withValue.startsWith("RGD:") ) {
                    withGeneCuries.add(withValue);
                }
            }
        }

        return true;
    }

    // convert human rgd ids to HGNC ids, and mouse rgd ids to MGI ids
    String transformRgdId(String with, Dao dao) throws Exception {

        if (with.startsWith("RGD:")) {
            Integer rgdId = Integer.parseInt(with.substring(4));
            RgdId id = dao.getRgdId(rgdId);
            if (id == null) {
                System.out.println("ERROR: invalid RGD ID " + with + "; skipping annotation");
                return null;
            }

            if (id.getSpeciesTypeKey() == SpeciesType.HUMAN) {
                List<XdbId> xdbIds = dao.getXdbIds(rgdId, XdbId.XDB_KEY_HGNC);
                if (xdbIds.isEmpty()) {
                    System.out.println("ERROR: cannot map " + with + " to human HGNC ID");
                    return null;
                }
                if (xdbIds.size() > 1) {
                    System.out.println("WARNING: multiple HGNC ids for " + with);
                }
                String hgncId = xdbIds.get(0).getAccId();
                return hgncId;
            } else if (id.getSpeciesTypeKey() == SpeciesType.MOUSE) {
                List<XdbId> xdbIds = dao.getXdbIds(rgdId, XdbId.XDB_KEY_MGD);
                if (xdbIds.isEmpty()) {
                    System.out.println("ERROR: cannot map " + with + " to mouse MGI ID");
                    return null;
                }
                if (xdbIds.size() > 1) {
                    System.out.println("WARNING: multiple MGI ids for " + with);
                }
                String mgiId = xdbIds.get(0).getAccId();
                return mgiId;
            } else if (id.getSpeciesTypeKey() == SpeciesType.RAT) {
                return with;
            } else {
                System.out.println("ERROR: RGD id for species other than rat,mouse,human in WITH field");
                return null;
            }
        }

        return with;
    }

    // str: string to be parsed
    // out: out[0]-extracted term; out[1]-separator before term
    // return rest of string 'str' after extracting the token
    String getNextToken(String str, String[] out) {

        if( str==null ) {
            out[0] = null;
            out[1] = null;
            return null;
        }

        int startPos = 0;
        if( str.startsWith("|") ) {
            out[1] = "|";
            startPos = 1;
        } else if( str.startsWith(",") ) {
            out[1] = ",";
            startPos = 1;
        } else {
            out[1] = null;
        }

        int endPos = str.length();

        int barPos = str.indexOf('|', startPos);
        if( barPos>=0 && barPos < endPos ) {
            endPos = barPos;
        }
        int commaPos = str.indexOf(',', startPos);
        if( commaPos>=0 && commaPos < endPos ) {
            endPos = commaPos;
        }

        out[0] = str.substring(startPos, endPos);

        if( endPos < str.length() ) {
            return str.substring(endPos);
        } else {
            return null;
        }
    }
}
