### EXPERIMENTAL: API submission is under construction
#
MYFILESPEC="1.0.0.6_GAF_9606"
MYFILELOC="data/RGD_1.0.0.6_1/RGD_1.0.0.6_go.9606.gaf"
MYFILE="$MYFILESPEC=@$MYFILELOC"
APITOKEN=`cat api.token`

echo "MYFILE: $MYFILE"

curl \
 -H "api_access_token: $APITOKEN" \
 -X POST "https://www.alliancegenome.org/api/data/submit" \
 -F "$MYFILE"


