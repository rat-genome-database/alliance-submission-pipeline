#####
## submission via API for rat files (taxon id: 10116)
#
TAXON=10116
SPECIES=RGD
APITOKEN=`cat api.token`




export MOD_NAME=RGD
export AGR_SCHEMA=1.0.1.4
export AGR_RELEASE="4.1.0"

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR/data

SOURCE_DIR=/home/rgddata/pipelines/ftp-file-extracts-pipeline/data/agr






REFERENCE_SPEC="${AGR_RELEASE}_REFERENCE_${SPECIES}"
REFERENCE_LOC="${SOURCE_DIR}/REFERENCE_RGD.json"
REFERENCE_FILE="${REFERENCE_SPEC}=@${REFERENCE_LOC}"
echo "$REFERENCE_FILE"

REFEXCHANGE_SPEC="${AGR_RELEASE}_REF-EXCHANGE_${SPECIES}"
REFEXCHANGE_LOC="${SOURCE_DIR}/REF-EXCHANGE_RGD.json"
REFEXCHANGE_FILE="${REFEXCHANGE_SPEC}=@${REFEXCHANGE_LOC}"
echo "$REFEXCHANGE_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$REFERENCE_FILE" \
 -F "$REFEXCHANGE_FILE" \
 | tee ref_submission.log

echo "=== OK ==="

