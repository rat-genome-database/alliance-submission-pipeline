#!/usr/bin/env bash
. /etc/profile

APPNAME="alliance-submission-pipeline"
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu
fi

cd $APPDIR
$APPDIR/run.sh --curation-allele-generator

