# MASTER script to submit files to AGR (the Alliance)


#
APITOKEN=`cat APIToken`

HTPDATASET_SPEC="8.3.0_HTPDATASET_RGD"
HTPDATASET_LOC="data/HTPDATASET_RGD.json"
HTPDATASET_FILE="${HTPDATASET_SPEC}=@${HTPDATASET_LOC}"
echo "$HTPDATASET_FILE"

HTPDATASAMPLE_SPEC="8.3.0_HTPDATASAMPLE_RGD"
HTPDATASAMPLE_LOC="data/HTPDATASAMPLES_RGD.json"
HTPDATASAMPLE_FILE="${HTPDATASAMPLE_SPEC}=@${HTPDATASAMPLE_LOC}"
echo "$HTPDATASAMPLE_FILE"


curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$HTPDATASET_FILE" \
 -F "$HTPDATASAMPLE_FILE" \
 | tee api_rat_submission_htp.log

echo "=== OK ==="

