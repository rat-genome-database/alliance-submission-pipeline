#!/bin/sh
APITOKEN=`cat ./curation.api.token`
curl -k \
 -H "Authorization: APIToken $APITOKEN" \
 -X POST "https://curation.alliancegenome.org/api/data/submit" \
 -F "GENE_RGD=@CURATION_GENES-RAT.json" \
 | tee rat-genes.log
