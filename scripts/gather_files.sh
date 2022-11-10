# generate all files needed by the pipeline for submission to AGR (the Alliance)
#
# Note: master script must export the sumbission dir variables!!!

echo "======="
echo "STAGE1: gather files to staging area"
echo ""

# gene alleles
scp -p ${DATA_RELEASE_DIR}/alleles*json $DATA_DIR
cp -p $DATA_DIR/alleles.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_allele.10116.json
echo "  alleles files staged!"

# variants
scp -p ${DATA_RELEASE_DIR}/variants*json $DATA_DIR
cp -p $DATA_DIR/variants.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_variant.10116.json
echo "  variant files staged!"

# affected genomic models (strains)
scp -p ${DATA_RELEASE_DIR}/affectedGenomicModels*json $DATA_DIR
cp -p $DATA_DIR/affectedGenomicModels.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_affectedGenomicModel.10116.json
echo "  affected genomic models (strains) files staged!"

# gene expression
scp -p ${DATA_RELEASE_DIR}/expression*json $DATA_DIR
cp -p $DATA_DIR/expression.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_expression.10116.json
echo "  expression files staged!"

### phenotype files
scp -p ${DATA_RELEASE_DIR}/phenotype*.json $DATA_DIR

cp -p $DATA_DIR/phenotypes.9606.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_phenotype.9606.json
cp -p $DATA_DIR/phenotypes.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_phenotype.10116.json
echo "  phenotype files staged!"

# basic gene information
scp -p ${DATA_RELEASE_DIR}/genes*json $DATA_DIR

cp -p $DATA_DIR/genes.9606.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_gene.9606.json
cp -p $DATA_DIR/genes.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_gene.10116.json
echo "  gene files staged!"

### disease files
scp -p ${DATA_RELEASE_DIR}/*daf.json $DATA_DIR

cp -p $DATA_DIR/disease.9606.daf.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_disease.9606.json
cp -p $DATA_DIR/disease.10116.daf.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_disease.10116.json
echo "  DISEASE daf files staged!"


### GO gaf files
scp -p ${DATA_RELEASE_DIR}/*gaf $DATA_DIR

cp -p $DATA_DIR/9606_genes_go.gaf $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_go.9606.gaf
echo "  HUMAN GO GAF file staged!"


### gff3 files
scp -p ${DATA_RELEASE_DIR}/*gff3.gz $DATA_DIR

cp $DATA_DIR/genes_9606.gff3.gz $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_GFF.9606.gff3.gz
cp $DATA_DIR/genes_10116.gff3.gz $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_GFF.10116.gff3.gz
gunzip  $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_GFF.*.gff3.gz
echo "  GFF3 files staged!"


### HTP files
scp -p ${DATA_RELEASE_DIR}/HTPDATA*.json $DATA_DIR

cp -p $DATA_DIR/HTPDATASET_RGD.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_HTPDATASET.10116.json
cp -p $DATA_DIR/HTPDATASAMPLES_RGD.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_HTPDATASAMPLE.10116.json
echo "  HTP files staged!"



### REF files
scp -p ${DATA_RELEASE_DIR}/REF*.json $DATA_DIR

cp -p $DATA_DIR/REFERENCE_RGD.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_REFERENCE.10116.json
cp -p $DATA_DIR/REF-EXCHANGE_RGD.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_REF-EXCHANGE.10116.json
echo "  REF files staged!"

echo "==="

