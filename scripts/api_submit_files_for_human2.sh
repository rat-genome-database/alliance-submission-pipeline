#####
## submission via API for rat files (taxon id: 9606)
#
TAXON=9606
SPECIES=HUMAN
APITOKEN=`cat api.token`

PHENOTYPE_SPEC="${AGR_RELEASE}_PHENOTYPE_${SPECIES}"
PHENOTYPE_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_phenotype.${TAXON}.json"
PHENOTYPE_FILE="${PHENOTYPE_SPEC}=@${PHENOTYPE_LOC}"
echo "$PHENOTYPE_FILE"

DISEASE_SPEC="${AGR_RELEASE}_DAF_${SPECIES}"
DISEASE_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_disease.${TAXON}.json"
DISEASE_FILE="${DISEASE_SPEC}=@${DISEASE_LOC}"
echo "$DISEASE_FILE"

BGI_SPEC="${AGR_RELEASE}_BGI_${SPECIES}"
BGI_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_gene.${TAXON}.json"
BGI_FILE="${BGI_SPEC}=@${BGI_LOC}"
echo "$BGI_FILE"

GAF_SPEC="${AGR_RELEASE}_GAF_${SPECIES}"
GAF_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_go.${TAXON}.gaf"
gzip $GAF_LOC
GAF_FILE="${GAF_SPEC}=@${GAF_LOC}.gz"
echo "$GAF_FILE"

GFF_SPEC="${AGR_RELEASE}_GFF_${SPECIES}"
GFF_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_GFF.${TAXON}.gff3"
gzip $GFF_LOC
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}.gz"
echo "$GFF_FILE"

$CMD=$(cat <<EOF
curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$PHENOTYPE_FILE" \
 -F "$DISEASE_FILE" \
 -F "$BGI_FILE" \
 -F "$GAF_FILE" \
 -F "$GFF_FILE" \
 | tee api_human_submission22.log
EOF
)
echo $CMD

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$PHENOTYPE_FILE" \
 -F "$DISEASE_FILE" \
 -F "$BGI_FILE" \
 -F "$GAF_FILE" \
 -F "$GFF_FILE" \
 | tee api_human_submission22.log

echo "=== OK ==="
