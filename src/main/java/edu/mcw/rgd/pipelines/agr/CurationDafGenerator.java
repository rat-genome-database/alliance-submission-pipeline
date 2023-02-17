package edu.mcw.rgd.pipelines.agr;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CurationDafGenerator {

    private Dao dao;
    private Map<Integer, String> rgdId2HgncIdMap;
    private Set<Integer> alleleRgdIds;
    private Set<Integer> spliceRgdIds;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        loadHgncIdMap();

        createDafFile(SpeciesType.RAT);
        createDafFile(SpeciesType.HUMAN);

        log.info("===");
        log.info("");
    }

    void createDafFile(int speciesTypeKey) throws Exception {

        String speciesName = SpeciesType.getCommonName(speciesTypeKey).toUpperCase();

        log.info("START "+speciesName+" DAF file");

        loadAlleleRgdIds(speciesTypeKey);
        loadSpliceRgdIds(speciesTypeKey);

        CurationDaf daf = new CurationDaf();

        // setup a JSON object array to collect all DafAnnotation objects
        ObjectMapper json = new ObjectMapper();
        // do not export fields with NULL values
        json.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        Collection<Annotation> annots = getAnnotations(speciesTypeKey);
        log.info("  annotations: "+annots.size());
        for( Annotation a: annots ) {
            daf.addDiseaseAnnotation(a, dao, rgdId2HgncIdMap, speciesTypeKey, alleleRgdIds.contains(a.getAnnotatedObjectRgdId()));
        }

        log.info("PREP "+speciesName+" daf file:  genes="+daf.disease_gene_ingest_set.size()+", agm="+daf.disease_agm_ingest_set.size()+", alleles="+daf.disease_allele_ingest_set.size());

        daf.removeDiseaseAnnotsSameAsAlleleAnnots();
        daf.removeDiseaseAnnotsSameAsAGMAnnots();

        // sort data, alphabetically by object symbols
        daf.sort();

        // dump DafAnnotation records to a file in JSON format
        try {
            String jsonFileName = "CURATION-"+speciesName+".daf";
            BufferedWriter jsonWriter = Utils.openWriter(jsonFileName);

            jsonWriter.write(json.writerWithDefaultPrettyPrinter().writeValueAsString(daf));

            jsonWriter.close();
        } catch(IOException ignore) {
        }

        log.info("END "+speciesName+" daf file:  genes="+daf.disease_gene_ingest_set.size()+", agm="+daf.disease_agm_ingest_set.size()+", alleles="+daf.disease_allele_ingest_set.size());
        log.info("");
    }

    void loadHgncIdMap() throws Exception {
        rgdId2HgncIdMap = new HashMap<>();
        List<XdbId> xdbIds = getDao().getActiveXdbIds(XdbId.XDB_KEY_HGNC, RgdId.OBJECT_KEY_GENES);
        for( XdbId xdbId: xdbIds ) {
            String accIds = rgdId2HgncIdMap.get(xdbId.getRgdId());
            if( accIds==null ) {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), xdbId.getAccId());
            } else {
                rgdId2HgncIdMap.put(xdbId.getRgdId(), accIds+","+xdbId.getAccId());
            }
        }
    }

    void loadAlleleRgdIds(int speciesTypeKey) throws Exception {
        alleleRgdIds = new HashSet<>();
        List<Gene> alleles = getDao().getGeneAlleles(speciesTypeKey);
        for( Gene g: alleles ) {
            alleleRgdIds.add(g.getRgdId());
        }
    }

    void loadSpliceRgdIds(int speciesTypeKey) throws Exception {
        spliceRgdIds = new HashSet<>();
        List<Gene> splices = getDao().getGeneSplices(speciesTypeKey);
        for( Gene g: splices ) {
            spliceRgdIds.add(g.getRgdId());
        }
    }


    Collection<Annotation> getAnnotations(int speciesTypeKey) throws Exception {
        List<Annotation> annots = getDao().getAnnotationsBySpecies(speciesTypeKey, "D", "RGD");
        annots.addAll(getDao().getAnnotationsBySpecies(speciesTypeKey, "D", "OMIM"));

        int annotCount1 = annots.size();
        Collection<Annotation> annots2 = annots.parallelStream().filter(a ->
            a.getRgdObjectKey()==1 || a.getRgdObjectKey()==5  // accept only genes and strains
        ).collect(Collectors.toList());
        int annotCount2 = annots2.size();
        log.info(annotCount1+" annotations; excluded non-genes; new count: "+annotCount2);

        Collection<Annotation> annots3 = deconsolidateAnnotations(annots2);
        Collection<Annotation> annots4 = applyFilters(annots3, speciesTypeKey);
        return annots4;
    }

    Collection<Annotation> applyFilters(Collection<Annotation> annots, int speciesTypeKey) throws Exception {

        int omimPsReplacements = 0;
        int excludedSpliceAnnotations = 0;
        int IGIsplits = 0;
        List<Annotation> annots2 = new ArrayList<>(annots.size());

        for( Annotation a: annots ) {

            // for human, annotated object rgd id must map to HGNC id
            if( speciesTypeKey==SpeciesType.HUMAN ) {
                String hgncId = rgdId2HgncIdMap.get(a.getAnnotatedObjectRgdId());
                if (hgncId == null) {
                    continue;
                }
            }

            // for genes evidence code must be a manual evidence code
            String assocType = Utils2.getGeneAssocType(a.getEvidence(), a.getRgdObjectKey(), alleleRgdIds.contains(a.getAnnotatedObjectRgdId()));
            if( assocType==null ) {
                continue;
            }

            // exclude splice annotations
            if( spliceRgdIds.contains(a.getAnnotatedObjectRgdId()) ) {
                excludedSpliceAnnotations++;
                continue;
            }

            // exclude DO+ custom terms (that were added by RGD and are not present in DO ontology)
            if( a.getTermAcc().startsWith("DOID:90") && a.getTermAcc().length() == 12) {

                // perform DO+ child-to-parent PS mapping, if possible
                // see if this term could be mapped to an OMIM PS id
                String parentTermAcc = getDao().getOmimPSTermAccForChildTerm(a.getTermAcc());
                if (parentTermAcc == null) {
                    continue;
                }

                if (parentTermAcc.startsWith("DOID:90") && parentTermAcc.length() == 12) {
                    continue;
                } else {
                    // replaced custom DO+ term with a parent non-DO+ term, via OMIM:PS mapping
                    a.setTermAcc(parentTermAcc);
                    omimPsReplacements++;
                }
            }

            // split for IGI annotations having multiple values in WITH field
            if( a.getEvidence().equals("IGI") && a.getWithInfo()!=null ) {

                String[] withFields = a.getWithInfo().split("[|,]");
                if( withFields.length==1 ) {
                    annots2.add(a);
                } else {
                    IGIsplits++;
                    for( String with: withFields ) {
                        Annotation aa = (Annotation) a.clone();
                        aa.setWithInfo(with);
                        annots2.add(aa);
                    }
                }
            } else {
                annots2.add(a);
            }
        }

        log.info(annots.size()+";  excluded DO+ terms; excluded splice annots;  annotations left: "+annots2.size());
        log.info("    OMIM:PS replacements: "+omimPsReplacements);
        log.info("    splice annotations excluded: "+excludedSpliceAnnotations);
        log.info("    IGI splits: "+IGIsplits);
        return annots2;
    }

    /**
     * NOTE: this method was almost literally copied from goc_annotation pipeline
     *
     * in RGD, we store pipeline annotations in consolidated form,
     * f.e. XREF_SOURCE: MGI:MGI:1100157|MGI:MGI:3714678|PMID:17476307|PMID:9398843
     *      NOTES:       MGI:MGI:2156556|MGI:MGI:2176173|MGI:MGI:2177226  (MGI:MGI:1100157|PMID:9398843), (MGI:MGI:3714678|PMID:17476307)
     * but GO spec says, REFERENCES column 6 can contain at most one PMID
     * so we must deconsolidate RGD annotations
     * what means we must split them into multiple, f.e.
     *    XREF_SOURCE1:  MGI:MGI:1100157|PMID:9398843
     *    XREF_SOURCE2:  MGI:MGI:3714678|PMID:17476307
     */
    Collection<Annotation> deconsolidateAnnotations(Collection<Annotation> annotations) throws Exception {

        int deconsolidatedAnnotsIncoming = 0;
        int deconsolidatedAnnotsCreated = 0;

        List<Annotation> result = new ArrayList<>(annotations.size());

        for( Annotation a: annotations ) {

            String xrefSrc = Utils.defaultString(a.getXrefSource());
            int posPmid1 = xrefSrc.indexOf("PMID:");
            int posPmid2 = xrefSrc.lastIndexOf("PMID:");
            if( !(posPmid1>=0 && posPmid2>posPmid1) ) {
                // only one PMID, annotation is already GO spec compliant
                result.add(a);
                continue;
            }
            deconsolidatedAnnotsIncoming++;

            int parPos = Utils.defaultString(a.getNotes()).indexOf("(");
            if( parPos<0 ) {
                deconsolidatedAnnotsCreated += deconsolidateWithNotesInfoMissing(a, result);
                continue;
            }
            String notesOrig = a.getNotes().substring(0, parPos).trim();

            // multi PMID annotation: deconsolidate it
            String[] xrefs = xrefSrc.split("[\\|\\,]");
            for( ;; ){
                // extract PMID from xrefSrc
                String pmid = null;
                for( int i=0; i<xrefs.length; i++ ) {
                    if( xrefs[i].startsWith("PMID:") ) {
                        pmid = xrefs[i];
                        xrefs[i] = "";
                        break;
                    }
                }
                if( pmid==null ) {
                    break;
                }

                // find corresponding PMID info in NOTES field
                int pmidPos = a.getNotes().indexOf(pmid);
                if( pmidPos<0 ) {
                    deconsolidatedAnnotsCreated += deconsolidateWithNotesInfoMissing(a, result);
                    break;
                }
                int parStartPos = a.getNotes().lastIndexOf("(", pmidPos);
                int parEndPos = a.getNotes().indexOf(")", pmidPos);
                if( parStartPos<0 || parEndPos<parStartPos ) {
                    log.warn("CANNOT DECONSOLIDATE ANNOTATION! SKIPPING IT: notes info malformed PMID: "+a.dump("|"));
                    continue;
                }
                String xrefInfo = a.getNotes().substring(parStartPos+1, parEndPos);

                Annotation ann = (Annotation)a.clone();
                ann.setXrefSource(xrefInfo);
                ann.setNotes(notesOrig);
                result.add(ann);
                deconsolidatedAnnotsCreated++;
            }
        }

        log.info(deconsolidatedAnnotsIncoming+" incoming annotations deconsolidated into "+deconsolidatedAnnotsCreated+" annotations");
        return result;
    }

    int deconsolidateWithNotesInfoMissing(Annotation a, List<Annotation> result) throws CloneNotSupportedException {

        int deconsolidatedAnnotsCreated = 0;

        String xrefSrc = Utils.defaultString(a.getXrefSource());

        // multi PMID annotation: deconsolidate it
        // we handle only xrefs with PMIDS only
        String[] xrefs = xrefSrc.split("[\\|\\,]");
        for( String xref: xrefs ){
            // extract PMID from xrefSrc
            if( !xref.startsWith("PMID:") ) {
                log.warn("CANNOT DECONSOLIDATE ANNOTATION! SKIPPING: notes info missing, not all PMIDs: "+a.dump("|"));
                return 0;
            }
        }

        for( String xref: xrefs ){
            Annotation ann = (Annotation)a.clone();
            ann.setXrefSource(xref);
            result.add(ann);
            deconsolidatedAnnotsCreated++;
        }
        return deconsolidatedAnnotsCreated;
    }


    public edu.mcw.rgd.pipelines.agr.Dao getDao() {
        return dao;
    }

    public void setDao(edu.mcw.rgd.pipelines.agr.Dao dao) {
        this.dao = dao;
    }
}
