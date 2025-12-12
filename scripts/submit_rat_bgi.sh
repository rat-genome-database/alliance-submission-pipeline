#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`


BGI_SPEC="8.3.0_BGI_RGD"
BGI_LOC="data/genes.10116.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$BGI_FILE" \
 | tee rat_bgi_submission.log

echo "=== OK ==="

