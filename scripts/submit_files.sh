# submit all files to AGR
# the param required is submission directory

echo "STAGE3: submit to AGR"
echo "==="
cd $SUBMISSION_DIR

TARBALL=$SUBMISSION_DIR/${BATCH}.tar.gz
echo "  create a tarball"
tar -czvf $TARBALL *.json *.gaf *.gff3
echo "  tarball $TARBALL created"
echo ""

echo "  submit files to AGR"
echo "aws s3 ls s3://mod-datadumps/"
#exit -2

aws s3 cp $TARBALL s3://mod-datadumps/

echo "copy tarball $TARBALL to data release dir $DATA_RELEASE_DIR"
cp -p $TARBALL $DATA_RELEASE_DIR

echo "OK"
