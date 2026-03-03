#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`


BGI_SPEC="9.0.0_BGI_HUMAN"
BGI_LOC="data/genes.9606.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee human_bgi_submission.log

echo "=== OK ==="

