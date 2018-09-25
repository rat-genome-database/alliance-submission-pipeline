# generate all files needed by the pipeline for submission to AGR (the Alliance)
#
# Note: master script must export the sumbission dir variables!!!

echo "======="
echo "STAGE1: gather files to staging area"
echo ""

# gene alleles
scp -p ${DATA_RELEASE_DIR}/alleles*json $DATA_DIR
cp -p $DATA_DIR/alleles.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_allele.10116.json
echo "  alleles files staged!"

# gene expression
scp -p ${DATA_RELEASE_DIR}/expression*json $DATA_DIR
cp -p $DATA_DIR/expression.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_expression.10116.json
echo "  expression files staged!"

### phenotype files
scp -p ${DATA_RELEASE_DIR}/phenotype*.json $DATA_DIR

cp -p $DATA_DIR/phenotypes.9606.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_phenotype.9606.json
cp -p $DATA_DIR/phenotypes.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_phenotype.10116.json
echo "  phenotype files staged!"

# basic gene information
scp -p ${DATA_RELEASE_DIR}/bgi*json $DATA_DIR

cp -p $DATA_DIR/bgi.9606.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_BGI.9606.json
cp -p $DATA_DIR/bgi.10116.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_BGI.10116.json
echo "  BGI files staged!"

### disease files
scp -p ${DATA_RELEASE_DIR}/*daf.json $DATA_DIR

cp -p $DATA_DIR/disease.9606.daf.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_disease.9606.json
cp -p $DATA_DIR/disease.10116.daf.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_disease.10116.json
echo "  DISEASE daf files staged!"


### GO gaf files
scp -p ${DATA_RELEASE_DIR}/*gaf $DATA_DIR

cp -p $DATA_DIR/9606_genes_go.gaf $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_go.9606.gaf
echo "  HUMAN GO GAF file staged!"


### gff3 files
scp -p ${DATA_RELEASE_DIR}/*gff3.gz $DATA_DIR

cp $DATA_DIR/${MOD_NAME}_1.0_9696.gff3.gz $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_GFF.9606.gff3.gz
cp $DATA_DIR/${MOD_NAME}_1.0_10116.gff3.gz $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_GFF.10116.gff3.gz
gunzip  $SUBMISSION_DIR/${MOD_NAME}_${AGR_VER}_GFF.*.gff3.gz

echo "  GFF3 files staged!"
echo "==="

