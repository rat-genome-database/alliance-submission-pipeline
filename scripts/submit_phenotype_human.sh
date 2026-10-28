#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")


PHENOTYPE_SPEC="${RELEASE}_PHENOTYPE_HUMAN"
PHENOTYPE_LOC="data/${RELEASE}/phenotypes.9606.json.gz"
PHENOTYPE_FILE="${PHENOTYPE_SPEC}=@${PHENOTYPE_LOC}"
echo "$PHENOTYPE_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$PHENOTYPE_FILE" \
 | tee human_pheno_submission.log

echo "=== OK ==="

