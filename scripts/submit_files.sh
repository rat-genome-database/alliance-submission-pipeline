# submit all files to AGR
echo "STAGE3: submit to AGR via API"
echo "==="

##echo "####### EXITING BY DESIGN #####"
##exit -1

#
$WORK_DIR/api_submit_files_for_rat2.sh

$WORK_DIR/api_submit_files_for_human2.sh


####
exit -4
####

echo "STAGE4: create a tarball and copy it to release dir"
echo "==="

cd $SUBMISSION_DIR
TARBALL=$SUBMISSION_DIR/${BATCH}.tar.gz
echo "  create a tarball"
gunzip *.gz
tar -czvf $TARBALL *.json *.gaf *.gff3
echo "  tarball $TARBALL created"
echo ""
echo "copy tarball $TARBALL to data release dir $DATA_RELEASE_DIR"
cp -p $TARBALL $DATA_RELEASE_DIR

echo "=== OK ==="

exit 0



# ###############
# === old method: submission to AGR S3 bucket
# === discontinued in year 2019


#echo "  submit files to AGR"
#echo "aws s3 ls s3://mod-datadumps/"
#aws s3 cp $TARBALL s3://mod-datadumps/
