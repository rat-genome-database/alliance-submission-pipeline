#####
#
APITOKEN=`cat api.token`

set -e

GFF_SPEC="8.3.0_GFF_RGD"
GFF_LOC="data/genes_10116.gff3.gz"
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}"
echo "$GFF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GFF_FILE" \
 | tee gff_submission_rat.log


GFF_SPEC="8.3.0_GFF_HUMAN"
GFF_LOC="data/genes_9606.gff3.gz"
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}"
echo "$GFF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GFF_FILE" \
 | tee gff_submission_human.log

echo "=== OK ==="
