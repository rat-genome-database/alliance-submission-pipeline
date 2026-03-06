#####
## submission via API for rat files (taxon id: 10116)
#
APITOKEN=`cat APIToken`


GAF_SPEC="9.0.0_PHENOTYPE_RGD"
GAF_LOC="data/phenotypes.10116.json"
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}"
echo "$GAF_FILE"



curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GAF_FILE" \
 | tee rat_pheno_submission.log

echo "=== OK ==="

