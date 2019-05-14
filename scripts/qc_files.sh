#qc the files to be submitted

echo "QC files"
echo "==="
echo ""

set -e

cd agr_schemas


#refresh local copy of AGR schemas, from https://github.com/alliance-genome/agr_schemas :
#
git fetch
git tag
git checkout ${AGR_VER}
git pull origin ${AGR_VER}

#validate variant file against schema
./agr_validate.py -d ../data/variants.10116.json -s allele/variantMetaData.json
echo

#validate alleles file against schema
./agr_validate.py -d ../data/alleles.10116.json -s allele/alleleMetaData.json
echo

#validate expression file against schema
./agr_validate.py -d ../data/expression.10116.json -s expression/wildtypeExpressionMetaDataDefinition.json
echo

#validate phenotype files against schema
echo "== phenotype 10116 =="
./agr_validate.py -d ../data/phenotypes.10116.json -s phenotype/phenotypeMetaDataDefinition.json
echo
echo "== phenotype 9606 =="
./agr_validate.py -d ../data/phenotypes.9606.json -s phenotype/phenotypeMetaDataDefinition.json
echo

#validate bgi file against schema
echo "== bgi 10116 =="
./agr_validate.py -d ../data/bgi.10116.json -s gene/geneMetaData.json
echo
echo "== bgi 9606 =="
./agr_validate.py -d ../data/bgi.9606.json -s gene/geneMetaData.json
echo

#validate disease file against schema
echo "== disease 10116 =="
./agr_validate.py -d ../data/disease.10116.daf.json -s disease/diseaseMetaDataDefinition.json
echo
echo "== disease 9606 =="
./agr_validate.py -d ../data/disease.9606.daf.json -s disease/diseaseMetaDataDefinition.json
echo
