#!/bin/sh
APITOKEN=`cat ./curation.api.token`
curl -k \
 -H "Authorization: APIToken $APITOKEN" \
 -X POST "https://curation.alliancegenome.org/api/data/submit" \
 -F "AGM_ASSOCIATION_RGD=@CURATION_AGM_ASSOCIATIONS-RAT.json" \
 | tee agm_associations.log
