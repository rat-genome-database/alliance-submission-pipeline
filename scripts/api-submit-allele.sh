#####
## EXPERIMENTAL: submission via API is still under development
#
MYFILESPEC="1.0.0.6_ALLELE_10116"
MYFILELOC="data/RGD_1.0.0.6_1/RGD_1.0.0.6_allele.10116.json"
MYFILE="$MYFILESPEC=@$MYFILELOC"
APITOKEN=`cat api.token`

echo "MYFILE: $MYFILE"

curl \
 -H "api_access_token: $APITOKEN" \
 -X POST "https://www.alliancegenome.org/api/data/submit" \
 -F "$MYFILE"


