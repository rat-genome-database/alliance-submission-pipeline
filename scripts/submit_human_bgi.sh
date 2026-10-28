#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")


BGI_SPEC="${RELEASE}_BGI_HUMAN"
BGI_LOC="data/${RELEASE}/genes.9606.json.gz"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee human_bgi_submission.log

echo "=== OK ==="

