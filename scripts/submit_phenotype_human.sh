#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`


GAF_SPEC="8.3.0_PHENOTYPE_HUMAN"
GAF_LOC="data/phenotypes.9606.json"
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}"
echo "$GAF_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GAF_FILE" \
 | tee human_pheno_submission.log

echo "=== OK ==="

