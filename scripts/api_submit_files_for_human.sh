#####
## submission via API for rat files (taxon id: 9606)
#
SPECIES=9606
APITOKEN=`cat api.token`

PHENOTYPE_SPEC="${AGR_VER}_PHENOTYPE_${SPECIES}"
PHENOTYPE_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_phenotype.${SPECIES}.json"
PHENOTYPE_FILE="${PHENOTYPE_SPEC}=@${PHENOTYPE_LOC}"
echo "$PHENOTYPE_FILE"

DISEASE_SPEC="${AGR_VER}_DAF_${SPECIES}"
DISEASE_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_disease.${SPECIES}.json"
DISEASE_FILE="${DISEASE_SPEC}=@${DISEASE_LOC}"
echo "$DISEASE_FILE"

BGI_SPEC="${AGR_VER}_BGI_${SPECIES}"
BGI_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_BGI.${SPECIES}.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

GAF_SPEC="${AGR_VER}_GAF_${SPECIES}"
GAF_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_go.${SPECIES}.gaf"
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}"
echo "$GAF_FILE"

GFF_SPEC="${AGR_VER}_GFF_${SPECIES}"
GFF_LOC="data/$BATCH/${MOD_NAME}_${AGR_VER}_GFF.${SPECIES}.gff3"
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}"
echo "$GFF_FILE"

curl -k \
 -H "api_access_token: $APITOKEN" \
 -X POST "https://www.alliancegenome.org/api/data/submit" \
 -F "$PHENOTYPE_FILE" \
 -F "$DISEASE_FILE" \
 -F "$BGI_FILE" \
 -F "$GAF_FILE" \
 -F "$GFF_FILE" \
 | tee api_human_submission.log

echo "=== OK ==="
