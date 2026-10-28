#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

VARIATION_SPEC="${RELEASE}_VARIATION_RGD"
VARIATION_LOC="data/${RELEASE}/variants.10116.json.gz"
VARIATION_FILE="${VARIATION_SPEC}=@${VARIATION_LOC}"
echo "$VARIATION_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$VARIATION_FILE" 

echo "=== OK ==="
