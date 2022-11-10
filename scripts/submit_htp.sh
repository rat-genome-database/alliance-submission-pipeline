# MASTER script to submit files to AGR (the Alliance)

export MOD_NAME=RGD
export AGR_SCHEMA=1.0.1.4
export UPLOAD_NR=3
export BATCH="${MOD_NAME}_${AGR_SCHEMA}_${UPLOAD_NR}"
export AGR_RELEASE="4.0.0"

export WORK_DIR=/home/rgddata/pipelines/AgrPipeline
cd $WORK_DIR
export DATA_DIR=$WORK_DIR/data

export SUBMISSION_DIR=$DATA_DIR/${BATCH}

if [ -d "$SUBMISSION_DIR" ]; then
  echo "Exiting: submission dir already exists: $SUBMISSION_DIR -- increment UPLOAD_NR in this script"

  exit -1
  #echo "*** temporary patch: delete the submission directory"
  #rm -r $SUBMISSION_DIR
fi
mkdir $SUBMISSION_DIR
echo "submission dir created: $SUBMISSION_DIR"


SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
if [ "$SERVER" == "REED" ]; then
  SERVER_DIR=""
else
  SERVER_DIR="rgddata@reed.rgd.mcw.edu:"
fi
export DATA_RELEASE_DIR="${SERVER_DIR}/home/rgddata/data_release/agr"


### HTP files
scp -p ${DATA_RELEASE_DIR}/HTPDATA*.json $DATA_DIR

cp -p $DATA_DIR/HTPDATASET_RGD.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_HTPDATASET.10116.json
cp -p $DATA_DIR/HTPDATASAMPLES_RGD.json $SUBMISSION_DIR/${MOD_NAME}_${AGR_SCHEMA}_HTPDATASAMPLE.10116.json
echo "  HTP files staged!"


cd agr_schemas


#refresh local copy of AGR schemas, from https://github.com/alliance-genome/agr_schemas :
#
GITHUB_BRANCH="release-${AGR_SCHEMA}"
git fetch
git tag
git checkout $GITHUB_BRANCH
git pull origin $GITHUB_BRANCH


bin/agr_validate.py -d ../data/HTPDATASET_RGD.json -s ingest/htp/dataset/datasetMetaDataDefinition.json
bin/agr_validate.py -d ../data/HTPDATASAMPLES_RGD.json -s ingest/htp/datasetSample/datasetSampleMetaDataDefinition.json

echo
cd $WORK_DIR

echo "==="

## submission via API for rat files (taxon id: 10116)
#
TAXON=10116
SPECIES=RGD
APITOKEN=`cat api.token`

HTPDATASET_SPEC="${AGR_RELEASE}_HTPDATASET_${SPECIES}"
HTPDATASET_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_HTPDATASET.${TAXON}.json"
HTPDATASET_FILE="${HTPDATASET_SPEC}=@${HTPDATASET_LOC}"
echo "$HTPDATASET_FILE"

HTPDATASAMPLE_SPEC="${AGR_RELEASE}_HTPDATASAMPLE_${SPECIES}"
HTPDATASAMPLE_LOC="data/$BATCH/${MOD_NAME}_${AGR_SCHEMA}_HTPDATASAMPLE.${TAXON}.json"
HTPDATASAMPLE_FILE="${HTPDATASAMPLE_SPEC}=@${HTPDATASAMPLE_LOC}"
echo "$HTPDATASAMPLE_FILE"


curl -k \
 -H "Authorization: Bearer $APITOKEN" \
 -X POST "https://fms.alliancegenome.org/api/data/submit" \
 -F "$HTPDATASET_FILE" \
 -F "$HTPDATASAMPLE_FILE" \
 | tee api_rat_submission_htp.log

echo "=== OK ==="

