#####
#
APITOKEN=`cat api.token`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

set -e

BGI_SPEC="${RELEASE}_BGI_RGD"
BGI_LOC="data/${RELEASE}/genes.10116.json.gz"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee bgi_submission_rat.log


BGI_SPEC="${RELEASE}_BGI_HUMAN"
BGI_LOC="data/${RELEASE}/genes.9606.json.gz"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee bgi_submission_human.log

echo "=== OK ==="
