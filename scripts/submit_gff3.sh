#####
#
APITOKEN=`cat APIToken`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")

set -e

GFF_SPEC="${RELEASE}_GFF_RGD"
GFF_LOC="data/${RELEASE}/genes_10116.gff3.gz"
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}"
echo "$GFF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GFF_FILE" \
 | tee gff_submission_rat.log


GFF_SPEC="${RELEASE}_GFF_HUMAN"
GFF_LOC="data/${RELEASE}/genes_9606.gff3.gz"
GFF_FILE="${GFF_SPEC}=@${GFF_LOC}"
echo "$GFF_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$GFF_FILE" \
 | tee gff_submission_human.log

echo "=== OK ==="
