#!/bin/sh
APITOKEN=`cat ./curation.api.token`
curl -k \
 -H "Authorization: APIToken $APITOKEN" \
 -X POST "https://curation.alliancegenome.org/api/data/submit" \
 -F "ALLELE_ASSOCIATION_RGD=@CURATION_ALLELE_ASSOCIATIONS-RAT.json" \
 | tee allele_associations.log
