#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`

EXPR_SPEC="9.0.0_DAF_RGD"
EXPR_LOC="data/disease.10116.daf.json"
EXPRESSION_FILE="${EXPR_SPEC}=@${EXPR_LOC}"
echo "$EXPRESSION_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$EXPRESSION_FILE" \
 | tee expr_submission.log


EXPR_SPEC="9.0.0_DAF_HUMAN"
EXPR_LOC="data/disease.9606.daf.json"
EXPRESSION_FILE="${EXPR_SPEC}=@${EXPR_LOC}"
echo "$EXPRESSION_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$EXPRESSION_FILE" \
 | tee expr_submission.log

echo "=== OK ==="
