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
./agr_validate.py -d ../data/variants.10116.json -s ingest/allele/variantMetaData.json
echo

#validate alleles file against schema
./agr_validate.py -d ../data/alleles.10116.json -s ingest/allele/alleleMetaData.json
echo

#validate expression file against schema
./agr_validate.py -d ../data/expression.10116.json -s ingest/expression/wildtypeExpressionMetaDataDefinition.json
echo

#validate affectedGenomicModels (strains) file against schema
./agr_validate.py -d ../data/affectedGenomicModels.10116.json -s ingest/affectedGenomicModel/affectedGenomicModelMetaData.json
echo

#validate phenotype files against schema
echo "== phenotype 10116 =="
./agr_validate.py -d ../data/phenotypes.10116.json -s ingest/phenotype/phenotypeMetaDataDefinition.json
echo
echo "== phenotype 9606 =="
./agr_validate.py -d ../data/phenotypes.9606.json -s ingest/phenotype/phenotypeMetaDataDefinition.json
echo

#validate genes file against schema
echo "== genes 10116 =="
./agr_validate.py -d ../data/genes.10116.json -s ingest/gene/geneMetaData.json
echo
echo "== genes 9606 =="
./agr_validate.py -d ../data/genes.9606.json -s ingest/gene/geneMetaData.json
echo

#validate disease file against schema
echo "== disease 10116 =="
./agr_validate.py -d ../data/disease.10116.daf.json -s ingest/disease/diseaseMetaDataDefinition.json
echo
echo "== disease 9606 =="
./agr_validate.py -d ../data/disease.9606.daf.json -s ingest/disease/diseaseMetaDataDefinition.json
echo
