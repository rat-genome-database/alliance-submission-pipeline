#####
## submission via API for rat files (taxon id: 9606)
#
TAXON=9606
SPECIES=HUMAN
APITOKEN=`cat api.token`

export MOD_NAME=RGD
export AGR_VER=1.0.1.0
export UPLOAD_NR=4
export BATCH="${MOD_NAME}_${AGR_VER}_${UPLOAD_NR}"
export AGR_RELEASE="3.0.0"

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR/data

export SUBMISSION_DIR=$DATA_DIR/${BATCH}


PHENOTYPE_SPEC="${AGR_RELEASE}_PHENOTYPE_${SPECIES}"
PHENOTYPE_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_phenotype.${TAXON}.json"
PHENOTYPE_FILE="${PHENOTYPE_SPEC}=@${PHENOTYPE_LOC}"
echo "$PHENOTYPE_FILE"

DISEASE_SPEC="${AGR_RELEASE}_DAF_${SPECIES}"
DISEASE_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_disease.${TAXON}.json"
DISEASE_FILE="${DISEASE_SPEC}=@${DISEASE_LOC}"
echo "$DISEASE_FILE"

BGI_SPEC="${AGR_RELEASE}_BGI_${SPECIES}"
BGI_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_gene.${TAXON}.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

GAF_SPEC="${AGR_RELEASE}_GAF_${SPECIES}"
GAF_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_go.${TAXON}.gaf"
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}"
echo "$GAF_FILE"

GFF_SPEC="${AGR_RELEASE}_GFF_${SPECIES}"
GFF_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_GFF.${TAXON}.gff3"
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}"
echo "$GFF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$PHENOTYPE_FILE" \
 -F "$DISEASE_FILE" \
 -F "$BGI_FILE" \
 -F "$GAF_FILE" \
 -F "$GFF_FILE" \
 | tee api_human_submission.log

echo "=== OK ==="
