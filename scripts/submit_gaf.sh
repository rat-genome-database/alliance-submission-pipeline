#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")


GAF_SPEC="${RELEASE}_GAF_HUMAN"
GAF_LOC="data/${RELEASE}/9606_genes_go.gaf.gz"
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}"
echo "$GAF_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GAF_FILE" \
 | tee human_gaf_submission.log

echo "=== OK ==="

