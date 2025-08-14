#!/bin/sh
APITOKEN=`cat ./curation.api.token`
curl -i \
 -H "Authorization: APIToken $APITOKEN" \
 -X POST "https://curation.alliancegenome.org/api/data/submit" \
 -F "GENE_HUMAN=@CURATION_GENES-HUMAN.json" \
 | tee human-genes.log

