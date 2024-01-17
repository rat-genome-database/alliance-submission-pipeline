# MASTER script to submit files to AGR (the Alliance)

export MOD_NAME=RGD
export AGR_SCHEMA=1.0.2.4
export UPLOAD_NR=1
export BATCH="${MOD_NAME}_${AGR_SCHEMA}_${UPLOAD_NR}"
export AGR_RELEASE="7.0.0"

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


$WORK_DIR/gather_files.sh

$WORK_DIR/qc_files.sh
if [ $? -ne 0 ]; then
  echo "=== QC failed -- aborting the master script"
  exit 1
fi

 $WORK_DIR/submit_files.sh

echo "=== OK ==="
