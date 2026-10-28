#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

AGM_SPEC="${RELEASE}_AGM_RGD"
AGM_LOC="data/${RELEASE}/affectedGenomicModels.10116.json.gz"
AGM_FILE="${AGM_SPEC}=@${AGM_LOC}"
echo "$AGM_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$AGM_FILE" \
 | tee agm_submission.log

echo "=== OK ==="
