#qc the files to be submitted

echo "QC files"
echo "==="
echo ""

set -e

cd agr_schemas


#refresh local copy of AGR schemas, from https://github.com/alliance-genome/agr_schemas :
#
GITHUB_BRANCH="release-${AGR_VER}"
git fetch
git tag
git checkout $GITHUB_BRANCH
git pull origin $GITHUB_BRANCH


bin/agr_validate.py -d ../data/REFERENCE_RGD.json -s ingest/resourcesAndReferences/referenceMetaData.json
bin/agr_validate.py -d ../data/REF-EXCHANGE_RGD.json -s ingest/resourcesAndReferences/referenceExchangeMetaData.json

bin/agr_validate.py -d ../data/HTPDATASET_RGD.json -s ingest/htp/dataset/datasetMetaDataDefinition.json
bin/agr_validate.py -d ../data/HTPDATASAMPLES_RGD.json -s ingest/htp/datasetSample/datasetSampleMetaDataDefinition.json

#validate variant file against schema
bin/agr_validate.py -d ../data/variants.10116.json -s ingest/allele/variantMetaData.json
echo

#validate alleles file against schema
bin/agr_validate.py -d ../data/alleles.10116.json -s ingest/allele/alleleMetaData.json
echo

#validate expression file against schema
bin/agr_validate.py -d ../data/expression.10116.json -s ingest/expression/wildtypeExpressionMetaDataDefinition.json
echo

#validate affectedGenomicModels (strains) file against schema
bin/agr_validate.py -d ../data/affectedGenomicModels.10116.json -s ingest/affectedGenomicModel/affectedGenomicModelMetaData.json
echo

#validate phenotype files against schema
echo "== phenotype 10116 =="
bin/agr_validate.py -d ../data/phenotypes.10116.json -s ingest/phenotype/phenotypeMetaDataDefinition.json
echo
echo "== phenotype 9606 =="
bin/agr_validate.py -d ../data/phenotypes.9606.json -s ingest/phenotype/phenotypeMetaDataDefinition.json
echo

#validate genes file against schema
echo "== genes 10116 =="
bin/agr_validate.py -d ../data/genes.10116.json -s ingest/gene/geneMetaData.json
grep primaryId ../data/genes.10116.json | sort | uniq -d > ../data/genes.10116.duplicates
if [ -s ../data/genes.10116.duplicates ]
then
  echo "ERROR! duplicate primaryIds in file genes.10116.json! ABORTING..."
  exit 25;
fi
echo
echo "== genes 9606 =="
bin/agr_validate.py -d ../data/genes.9606.json -s ingest/gene/geneMetaData.json
grep primaryId ../data/genes.9606.json | sort | uniq -d > ../data/genes.9606.duplicates
if [ -s ../data/genes.9606.duplicates ]
then
  echo "ERROR! duplicate primaryIds in file genes.10116.json! ABORTING..."
  exit 25;
fi
echo

#validate disease file against schema
echo "== disease 10116 =="
bin/agr_validate.py -d ../data/disease.10116.daf.json -s ingest/disease/diseaseMetaDataDefinition.json
echo
echo "== disease 9606 =="
bin/agr_validate.py -d ../data/disease.9606.daf.json -s ingest/disease/diseaseMetaDataDefinition.json
echo
