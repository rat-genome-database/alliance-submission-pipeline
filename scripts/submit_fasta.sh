#####
## submission via API for rat files (taxon id: 10116)
#
TAXON=10116
SPECIES=RGD
APITOKEN=`cat api.token`




export MOD_NAME=RGD
export AGR_SCHEMA=1.0.1.4
export AGR_RELEASE="5.1.0"

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR


FASTA="$WORK_DIR/mRatBN7.2.fa.gz"

curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "5.1.0_FASTA_mRatBN7.2=@${FASTA}" \
 | tee fasta_submission.log

echo "=== OK ==="

