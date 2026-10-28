#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

EXPRESSION_SPEC="${RELEASE}_EXPRESSION_RGD"
EXPRESSION_LOC="data/${RELEASE}/expression.10116.json.gz"
EXPRESSION_FILE="${EXPRESSION_SPEC}=@${EXPRESSION_LOC}"
echo "$EXPRESSION_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$EXPRESSION_FILE" \
 | tee expr_submission.log

echo "=== OK ==="
