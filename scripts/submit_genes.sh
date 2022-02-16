#####
#
APITOKEN=`cat api.token`

set -e

BGI_SPEC="3.2.0_BGI_RGD"
BGI_LOC="/home/rgddata/data_release/agr/genes.10116.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee bgi_submission_rat.log


BGI_SPEC="3.2.0_BGI_HUMAN"
BGI_LOC="/home/rgddata/data_release/agr/genes.9606.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee bgi_submission_human.log

echo "=== OK ==="
