#####
#
APITOKEN=`cat api.token`

HTVCF_SPEC="5.1.0_HTVCF_RGD"
HTVCF_LOC="5.1.0_HTVCF_RGD.vcf.gz"
HTVCF_FILE="${HTVCF_SPEC}=@${HTVCF_LOC}"
echo "$HTVCF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$HTVCF_FILE" \
 | tee htvcf_submission.log

echo "=== OK ==="
