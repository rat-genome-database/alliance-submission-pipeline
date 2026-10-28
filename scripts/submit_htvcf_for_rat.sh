#####
#
APITOKEN=`cat api.token`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

HTVCF_SPEC="${RELEASE}_HTVCF_RGD"
HTVCF_LOC="data/${RELEASE}/${RELEASE}_HTVCF_RGD.vcf.gz"
HTVCF_FILE="${HTVCF_SPEC}=@${HTVCF_LOC}"
echo "$HTVCF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$HTVCF_FILE" \
 | tee htvcf_submission.log

echo "=== OK ==="
