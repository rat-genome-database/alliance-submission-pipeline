#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

DAF_SPEC="${RELEASE}_DAF_RGD"
DAF_LOC="data/${RELEASE}/disease.10116.daf.json.gz"
DAF_FILE="${DAF_SPEC}=@${DAF_LOC}"
echo "$DAF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$DAF_FILE" \
 | tee daf_submission_rat.log


DAF_SPEC="${RELEASE}_DAF_HUMAN"
DAF_LOC="data/${RELEASE}/disease.9606.daf.json.gz"
DAF_FILE="${DAF_SPEC}=@${DAF_LOC}"
echo "$DAF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$DAF_FILE" \
 | tee daf_submission_human.log

echo "=== OK ==="
