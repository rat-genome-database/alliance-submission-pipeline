#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`

EXPR_SPEC="9.0.0_VARIATION_RGD"
EXPR_LOC="data/variants.10116.json"
EXPRESSION_FILE="${EXPR_SPEC}=@${EXPR_LOC}"
echo "$EXPRESSION_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$EXPRESSION_FILE" 

echo "=== OK ==="
