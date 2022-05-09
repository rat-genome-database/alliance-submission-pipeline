package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.dao.DataSourceFactory;
import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.dao.impl.variants.VariantDAO;
import edu.mcw.rgd.dao.spring.IntStringMapQuery;
import edu.mcw.rgd.dao.spring.StringListQuery;
import edu.mcw.rgd.dao.spring.variants.VariantMapQuery;
import edu.mcw.rgd.dao.spring.variants.VariantSampleQuery;
import edu.mcw.rgd.dao.spring.variants.VariantTranscriptQuery;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.variants.VariantMapData;
import edu.mcw.rgd.datamodel.variants.VariantSampleDetail;
import edu.mcw.rgd.datamodel.variants.VariantTranscript;
import org.springframework.jdbc.core.SqlParameter;

import javax.sql.DataSource;
import java.sql.Types;
import java.util.*;
import java.util.Map;

/**
 * @author mtutaj
 * @since 2/16/2022
 * All database code lands here
 */
public class Dao {

    private AliasDAO aliasDAO = new AliasDAO();
    private AnnotationDAO annotationDAO = new AnnotationDAO();
    private GeneDAO geneDAO = new GeneDAO();
    private MapDAO mapDAO = new MapDAO();
    private ProteinDAO proteinDAO = new ProteinDAO();
    private ReferenceDAO refDAO = new ReferenceDAO();
    private RGDManagementDAO rgdIdDAO = new RGDManagementDAO();
    private XdbIdDAO xdbIdDAO = new XdbIdDAO();
    private edu.mcw.rgd.dao.impl.variants.VariantDAO variantDAO = new VariantDAO();

    public String getConnectionInfo() {
        return variantDAO.getConnectionInfo();
    }

    public DataSource getVariantDataSource() throws Exception {
        return DataSourceFactory.getInstance().getCarpeNovoDataSource();
    }

    public List<VariantMapData> getVariants(int speciesTypeKey, int mapKey, String chr) throws Exception{
        String sql = "SELECT * FROM variant v inner join variant_map_data vm on v.rgd_id = vm. rgd_id WHERE v.species_type_key = ? and vm.map_key = ? and vm.chromosome=?";

        VariantMapQuery q = new VariantMapQuery(getVariantDataSource(), sql);
        q.declareParameter(new SqlParameter(Types.INTEGER));
        q.declareParameter(new SqlParameter(Types.INTEGER));
        q.declareParameter(new SqlParameter(Types.VARCHAR));
        return q.execute(speciesTypeKey, mapKey, chr);
    }

    public List<VariantTranscript> getVariantTranscripts(int mapKey, String chr) throws Exception{
        String sql = "select vt.* from variant_transcript vt inner join  variant_map_data v on v.rgd_id=vt.variant_rgd_id AND v.map_key=? AND v.chromosome=?";
        return this.executeVarTranscriptQuery(sql, mapKey, chr);
    }

    public List<VariantTranscript> getVariantTranscripts(int rgdId) throws Exception{
        String sql = "select * from variant_transcript where variant_rgd_id = ?";
        return this.executeVarTranscriptQuery(sql, rgdId);
    }

    public List<VariantSampleDetail> getSampleIds(int mapKey, String chr) throws Exception{
        String sql = "select vs.* from variant_sample_detail vs inner join variant_map_data vm on vm.rgd_id = vs.rgd_id and vm.map_key=? and vm.chromosome = ?";
        VariantSampleQuery q = new VariantSampleQuery(this.getVariantDataSource(), sql);
        q.declareParameter(new SqlParameter(Types.INTEGER));
        q.declareParameter(new SqlParameter(Types.VARCHAR));
        return q.execute(mapKey, chr);
    }

    public List<String> getChromosomes(int mapKey) throws Exception {

        String sql = "SELECT DISTINCT chromosome FROM chromosomes WHERE map_key=? ";
        StringListQuery q = new StringListQuery(mapDAO.getDataSource(), sql);
        q.declareParameter(new SqlParameter(Types.INTEGER));
        q.compile();
        return q.execute(new Object[]{mapKey});
    }

    public Sample getSample(int id) throws Exception{
        SampleDAO sampleDAO = new SampleDAO();
        sampleDAO.setDataSource(this.getVariantDataSource());
        return sampleDAO.getSample(id);
    }

    public int getSpeciesFromMap(int mapKey) throws Exception{
        return mapDAO.getSpeciesTypeKeyForMap(mapKey);
    }

    public Map<String,Integer> getChromosomeSizes(int mapKey) throws Exception{
        return mapDAO.getChromosomeSizes(mapKey);
    }

    public List<MapData> getMapData(int rgdId, int mapKey) throws Exception {
        return mapDAO.getMapData(rgdId, mapKey);
    }

    public List<VariantMapData> executeVariantQuery(String query, Object... params) throws Exception {
        VariantMapQuery q = new VariantMapQuery(getVariantDataSource(), query);
        return q.execute(params);
    }

    public List<VariantTranscript> executeVarTranscriptQuery(String query, Object... params) throws Exception {
        VariantTranscriptQuery q = new VariantTranscriptQuery(getVariantDataSource(), query);
        return q.execute(params);
    }



    public List<Annotation> getAnnotationsBySpecies(int speciesTypeKey, String aspect, String source) throws Exception {
        return annotationDAO.getAnnotationsBySpeciesAspectAndSource(speciesTypeKey, aspect, source);
    }

    public String getOmimPSTermAccForChildTerm(String childTermAcc) throws Exception {
        String sql = "SELECT parent_term_acc FROM omim_ps_custom_do WHERE child_term_acc=?";
        List<String> termAccIds = StringListQuery.execute(annotationDAO, sql, childTermAcc);
        if( termAccIds.isEmpty() ) {
            return null;
        }
        return termAccIds.get(0);
    }


    public String getPmid(int refRgdId) throws Exception {
        if( _pmidMap==null ) {
            List<IntStringMapQuery.MapPair> pmidList = refDAO.getPubmedIdsAndRefRgdIds();
            _pmidMap = new HashMap<>(pmidList.size());
            for (IntStringMapQuery.MapPair pair : pmidList) {
                String pmid = _pmidMap.put(pair.keyValue, pair.stringValue);
                if( pmid != null ) {
                    System.out.println("multiple PMIDs for REF_RGD_ID:"+pair.keyValue+", PMID:"+pmid);
                }
            }
        }
        return _pmidMap.get(refRgdId);
    }
    Map<Integer,String> _pmidMap;

    public List<XdbId> getActiveXdbIds(int xdbKey, int objectKey) throws Exception {
        return xdbIdDAO.getActiveXdbIds(xdbKey, objectKey);
    }

    public List<XdbId> getXdbIds(int rgdId, int xdbKey) throws Exception {
        return xdbIdDAO.getXdbIdsByRgdId(xdbKey, rgdId);
    }

    public RgdId getRgdId(int rgdId) throws Exception {
        return rgdIdDAO.getRgdId2(rgdId);
    }

    public List<Gene> getGeneAlleles(int speciesTypeKey) throws Exception {
        return geneDAO.getActiveGenesByType("allele", speciesTypeKey);
    }

    public List<Gene> getAllGenes(int speciesTypeKey) throws Exception {
        List<Gene> genes = geneDAO.getAllGenes(speciesTypeKey);
        genes.removeIf(Gene::isVariant);
        return genes;
    }

    public List<Alias> getAliases(int rgdId, String[]aliasTypes) throws Exception {
        return aliasDAO.getAliases(rgdId, aliasTypes);
    }

    public Set<String> getCanonicalProteins(int speciesTypeKey) throws Exception {
        Set<String> canonicalProteinSet = new HashSet<>();

        List<Protein> proteins = proteinDAO.getProteins(speciesTypeKey);
        for( Protein p: proteins ) {
            if( p.isCanonical() ) {
                canonicalProteinSet.add(p.getUniprotId());
            }
        }
        return canonicalProteinSet;
    }
}
