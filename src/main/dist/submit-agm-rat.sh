
#!/bin/sh
APITOKEN=`cat ./curation.api.token`
curl -k \
 -H "Authorization: APIToken $APITOKEN" \
 -X POST "https://curation.alliancegenome.org/api/data/submit" \
 -F "AGM_RGD=@CURATION_AGM-RAT.json" \
 | tee agm.log

