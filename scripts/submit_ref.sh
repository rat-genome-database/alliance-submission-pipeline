#####
## submission via API for rat files (taxon id: 10116)
#
TAXON=10116
SPECIES=RGD
APITOKEN=`cat api.token`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")




export MOD_NAME=RGD

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR/data







REFERENCE_SPEC="${RELEASE}_REFERENCE_${SPECIES}"
REFERENCE_LOC="data/${RELEASE}/REFERENCE_RGD.json.gz"
REFERENCE_FILE="${REFERENCE_SPEC}=@${REFERENCE_LOC}"
echo "$REFERENCE_FILE"

REFEXCHANGE_SPEC="${RELEASE}_REF-EXCHANGE_${SPECIES}"
REFEXCHANGE_LOC="data/${RELEASE}/REF-EXCHANGE_RGD.json.gz"
REFEXCHANGE_FILE="${REFEXCHANGE_SPEC}=@${REFEXCHANGE_LOC}"
echo "$REFEXCHANGE_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$REFERENCE_FILE" \
 -F "$REFEXCHANGE_FILE" \
 | tee ref_submission.log

echo "=== OK ==="

