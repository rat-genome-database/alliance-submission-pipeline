#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

ALLELE_SPEC="${RELEASE}_ALLELE_RGD"
ALLELE_LOC="data/${RELEASE}/alleles.10116.json.gz"
ALLELE_FILE="${ALLELE_SPEC}=@${ALLELE_LOC}"
echo "$ALLELE_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$ALLELE_FILE" \
 | tee allele_submission.log

echo "=== OK ==="
