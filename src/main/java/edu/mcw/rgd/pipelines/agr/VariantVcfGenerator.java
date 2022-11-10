package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.variants.VariantMapData;
import edu.mcw.rgd.datamodel.variants.VariantSampleDetail;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class VariantVcfGenerator {

    private Dao dao;
    private Set<Integer> rn7Samples;

    Logger log = LogManager.getLogger("status");

    public void run() throws Exception {

        log.info("START VariantVcfGenerator for rn7");

        final int mapKey = 372;
        createVariantFile2(mapKey);

        log.info("END VariantVcfGenerator for rn7");
        log.info("===");
        log.info("");
    }

    public void createVariantFile(int mapKey) throws Exception{

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileDate = sdf.format(new Date());

        int speciesTypeKey = dao.getSpeciesFromMap(mapKey);
        List<String> chrs = dao.getChromosomes(mapKey);
        Collections.shuffle(chrs);

        //#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO    FORMAT
        for(String chr:chrs) {
            //chr="MT";
            String fname = "data/" + chr + ".vcf.gz";
            File f1 = new File(fname);
            File f2 = new File("data/" + chr + "_copy.vcf.gz");
            if( f2.exists() ) {
                continue;
            }

            //String chr = "MT";
            BufferedWriter writer = Utils.openWriter(fname);
            log.info("start for mapkey " + mapKey + " and chr " + chr);

            List<VariantMapData> variants = dao.getVariants(speciesTypeKey, mapKey, chr);
            List<VariantSampleDetail> sampleDetails = dao.getSampleIds(mapKey, chr);
            HashMap<Long, HashMap<Integer, VariantSampleDetail>> sampleMap = new HashMap<>();
            for (VariantSampleDetail v : sampleDetails) {
                if( !rn7Samples.contains(v.getSampleId()) ) {
                    continue;
                }
                HashMap<Integer, VariantSampleDetail> mdata = sampleMap.get(v.getId());
                if (mdata == null) {
                    mdata = new HashMap<>();
                    sampleMap.put(v.getId(), mdata);
                }
                mdata.put(v.getSampleId(), v);
            }
            log.info("Variants retrieved successfully for mapkey " + mapKey + " and chr " + chr);

            Map<String, Integer> chromosomeMap = dao.getChromosomeSizes(mapKey);
            //header
            writer.write("##fileformat=VCFv4.2\n");
            writer.write("##fileDate="+fileDate+"\n");
            writer.write("##INFO=<ID=VT,Number=0,Type=Integer,Description=\"Variant type: SNV, INS, and DEL\">\n");
            writer.write("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">\n");
            writer.write("##FORMAT=<ID=DP,Number=2,Type=String,Description=\"Depth\">\n");
            writer.write("##contig=<ID=" + chr + ",length=" + chromosomeMap.get(chr) + ",assembly=mRatBN7.2,species=\"Rattus norvegicus\">\n");

            writer.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
            for( int sampleId: rn7Samples) {
                edu.mcw.rgd.datamodel.Sample s = dao.getSample(sampleId);
                writer.write("\tRGD:" + s.getStrainRgdId());
            }
            writer.write("\n");
            for (VariantMapData variant : variants) {

                log.debug("Processing variant id: " + variant.getId());
                HashMap<Integer, VariantSampleDetail> sampleDetailList = sampleMap.get(variant.getId());
                if (sampleDetailList!=null && sampleDetailList.size() != 0) {

                    long pos = variant.getStartPos();
                    String refNuc = Utils.defaultString(variant.getReferenceNucleotide());
                    String varNuc = Utils.defaultString(variant.getVariantNucleotide());

                    // adjust for padding base
                    String paddingBase = Utils.defaultString(variant.getPaddingBase());
                    if( paddingBase.length()>0  ) {
                        pos -= paddingBase.length();
                        refNuc = paddingBase + refNuc;
                        varNuc = paddingBase + varNuc;
                    }

                    writer.write(chr);
                    writer.write("\t");
                    writer.write(String.valueOf(pos));
                    writer.write("\t");
                    writer.write(".");
                    writer.write("\t");
                    writer.write(refNuc);
                    writer.write("\t");
                    writer.write(varNuc);
                    writer.write("\t");
                    writer.write(".");//Qual
                    writer.write("\t");
                    writer.write("PASS"); //Filter
                    writer.write("\t");
                    writer.write("VT=" + variant.getVariantType());
                    writer.write("\t");
                    writer.write("GT:DP");
                    writer.write("\t");
                    for (int sampleId: rn7Samples) {
                        VariantSampleDetail detail = sampleDetailList.get(sampleId);
                        if (detail == null)
                            writer.write("./.\t");
                        else if (detail.getZygosityStatus().equalsIgnoreCase("heterozygous"))
                            writer.write("0/1:" + detail.getDepth() + "\t");
                        else if (detail.getZygosityStatus().equalsIgnoreCase("homozygous") || detail.getZygosityStatus().equalsIgnoreCase("possibly homozygous"))
                            writer.write("1/1:" + detail.getDepth() + "\t");

                    }
                    writer.write("\n");
                }
            }
            writer.close();

            if( !f2.exists() ) {
                f1.renameTo(f2);
            }
        }
    }

    public void createVariantFile2(int mapKey) throws Exception{

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String fileDate = sdf.format(new Date());

        int speciesTypeKey = dao.getSpeciesFromMap(mapKey);
        List<String> chrs = dao.getChromosomes(mapKey);

        String fname = "data/rn7.vcf.gz";
        BufferedWriter out = Utils.openWriter(fname);

        // write header
        Map<String, Integer> chromosomeMap = dao.getChromosomeSizes(mapKey);
        //header
        out.write("##fileformat=VCFv4.2\n");
        out.write("##fileDate="+fileDate+"\n");
        out.write("##INFO=<ID=VT,Number=0,Type=Integer,Description=\"Variant type: SNV, INS, and DEL\">\n");
        out.write("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">\n");
        out.write("##FORMAT=<ID=DP,Number=2,Type=String,Description=\"Depth\">\n");
        for(String chr:chrs) {
            out.write("##contig=<ID=" + chr + ",length=" + chromosomeMap.get(chr) + ",assembly=mRatBN7.2,species=\"Rattus norvegicus\">\n");
        }
        out.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");

        for( int sampleId: rn7Samples) {
            edu.mcw.rgd.datamodel.Sample s = dao.getSample(sampleId);
            if( s.getId()==23 ) { // EVA sample does not have a strain rgd id
                out.write("\tEVA release 3");
            } else {
                out.write("\tRGD:" + s.getStrainRgdId());
            }
        }
        out.write("\n");

        //#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO    FORMAT
        for(String chr:chrs) {

            log.info("start for mapkey " + mapKey + " and chr " + chr);

            List<VariantMapData> variants = dao.getVariants(speciesTypeKey, mapKey, chr);
            List<VariantSampleDetail> sampleDetails = dao.getSampleIds(mapKey, chr);
            HashMap<Long, HashMap<Integer, VariantSampleDetail>> sampleMap = new HashMap<>();
            for (VariantSampleDetail v : sampleDetails) {
                if( !rn7Samples.contains(v.getSampleId()) ) {
                    continue;
                }
                HashMap<Integer, VariantSampleDetail> mdata = sampleMap.get(v.getId());
                if (mdata == null) {
                    mdata = new HashMap<>();
                    sampleMap.put(v.getId(), mdata);
                }
                mdata.put(v.getSampleId(), v);
            }
            log.info("    variants retrieved: "+variants.size());
            int linesWritten = 0;

            for (VariantMapData variant : variants) {

                log.debug("    processing variant id: " + variant.getId());
                HashMap<Integer, VariantSampleDetail> sampleDetailList = sampleMap.get(variant.getId());
                if (sampleDetailList==null || sampleDetailList.isEmpty() ) {
                    continue;
                }

                long pos = variant.getStartPos();
                String refNuc = Utils.defaultString(variant.getReferenceNucleotide());
                String varNuc = Utils.defaultString(variant.getVariantNucleotide());

                // adjust for padding base
                String paddingBase = Utils.defaultString(variant.getPaddingBase());
                if( paddingBase.length()>0  ) {
                    pos -= paddingBase.length();
                    refNuc = paddingBase + refNuc;
                    varNuc = paddingBase + varNuc;
                }

                out.write(chr);
                out.write("\t");
                out.write(String.valueOf(pos));
                out.write("\t");
                if( Utils.isStringEmpty(variant.getRsId()) ) {
                    out.write(".");
                } else {
                    out.write(variant.getRsId());
                }
                out.write("\t");
                out.write(refNuc);
                out.write("\t");
                out.write(varNuc);
                out.write("\t");
                out.write(".");//Qual
                out.write("\t");
                out.write("PASS"); //Filter
                out.write("\t");
                out.write("VT=" + variant.getVariantType());
                out.write("\t");
                out.write("GT:DP");
                out.write("\t");
                for (int sampleId: rn7Samples) {
                    VariantSampleDetail detail = sampleDetailList.get(sampleId);
                    if (detail == null)
                        out.write("./.\t");
                    else {
                        if( detail.getZygosityStatus()==null ) {
                            // for EVA
                            out.write("1/1:"+ detail.getDepth() + "\t");
                        } else
                        if (detail.getZygosityStatus().equalsIgnoreCase("heterozygous"))
                            out.write("0/1:" + detail.getDepth() + "\t");
                        else if (detail.getZygosityStatus().equalsIgnoreCase("homozygous") || detail.getZygosityStatus().equalsIgnoreCase("possibly homozygous"))
                            out.write("1/1:" + detail.getDepth() + "\t");
                    }

                }
                out.write("\n");
                linesWritten++;
            }
            log.info("    vcf lines written: " + linesWritten);
        }
        out.close();
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public Dao getDao() {
        return dao;
    }

    public Set<Integer> getRn7Samples() {
        return rn7Samples;
    }

    public void setRn7Samples(Set<Integer> rn7Samples) {
        this.rn7Samples = rn7Samples;
    }
}
