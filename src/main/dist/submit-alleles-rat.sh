
#!/bin/sh
APITOKEN=`cat ./curation.api.token`
curl -k \
 -H "Authorization: APIToken $APITOKEN" \
 -X POST "https://curation.alliancegenome.org/api/data/submit" \
 -F "ALLELE_RGD=@CURATION_ALLELES-RAT.json" \
 | tee alleles.log
