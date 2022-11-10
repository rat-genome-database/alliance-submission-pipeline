#####
## submission via API for rat files (taxon id: 10116)
#
TAXON=10116
SPECIES=RGD
APITOKEN=`cat api.token`




export MOD_NAME=RGD
export AGR_SCHEMA=1.0.1.4
export UPLOAD_NR=2
export BATCH="${MOD_NAME}_${AGR_SCHEMA}_${UPLOAD_NR}"
export AGR_RELEASE="4.0.0"

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR/data

export SUBMISSION_DIR=$DATA_DIR/${BATCH}





BGI_SPEC="${AGR_RELEASE}_BGI_HUMAN"
BGI_LOC="data/genes.9606.json.fix"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee human_bgi_submission.log

echo "=== OK ==="

