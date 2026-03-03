#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`


GAF_SPEC="9.0.0_GAF_HUMAN"
GAF_LOC="data/9606_genes_go.gaf"
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}"
echo "$GAF_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GAF_FILE" \
 | tee human_gaf_submission.log

echo "=== OK ==="

