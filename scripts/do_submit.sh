# submit all files to AGR
echo "STAGE3: submit to AGR via API"
echo "==="
#
$WORK_DIR/do_rat.sh
$WORK_DIR/do_human.sh


####
#exit -4
####
#echo "  submit files to AGR"
#echo "aws s3 ls s3://mod-datadumps/"
#aws s3 cp $TARBALL s3://mod-datadumps/


