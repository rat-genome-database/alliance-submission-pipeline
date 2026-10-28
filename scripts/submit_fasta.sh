#####
## submission via API for rat files (taxon id: 10116)
#
TAXON=10116
SPECIES=RGD
APITOKEN=`cat api.token`
RELEASE=$(tr -d "[:space:]" < "$(dirname "$0")/RELEASE.txt")




export MOD_NAME=RGD

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR


FASTA_SPEC="${RELEASE}_FASTA_mRatBN7.2"
FASTA_LOC="data/${RELEASE}/mRatBN7.2.fa.gz"
FASTA_FILE="${FASTA_SPEC}=@${FASTA_LOC}"
echo "$FASTA_FILE"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$FASTA_FILE" \
 | tee fasta_submission.log

echo "=== OK ==="

