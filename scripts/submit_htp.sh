# MASTER script to submit files to AGR (the Alliance)


#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

HTPDATASET_SPEC="${RELEASE}_HTPDATASET_RGD"
HTPDATASET_LOC="data/${RELEASE}/HTPDATASET_RGD.json.gz"
HTPDATASET_FILE="${HTPDATASET_SPEC}=@${HTPDATASET_LOC}"
echo "$HTPDATASET_FILE"

HTPDATASAMPLE_SPEC="${RELEASE}_HTPDATASAMPLE_RGD"
HTPDATASAMPLE_LOC="data/${RELEASE}/HTPDATASAMPLES_RGD.json.gz"
HTPDATASAMPLE_FILE="${HTPDATASAMPLE_SPEC}=@${HTPDATASAMPLE_LOC}"
echo "$HTPDATASAMPLE_FILE"


curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$HTPDATASET_FILE" \
 -F "$HTPDATASAMPLE_FILE" \
 | tee api_rat_submission_htp.log

echo "=== OK ==="

