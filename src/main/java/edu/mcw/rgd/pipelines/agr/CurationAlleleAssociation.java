package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.*;
import java.util.*;

public class CurationAlleleAssociation extends CurationObject {

    public List<AlleleGeneAssociationModel> allele_gene_association_ingest_set = new ArrayList<>();

    public int add(Gene a, Dao dao) throws Exception {

        int count = 0;

        // get allele-gene associations from RGD
        List<Gene> associatedGenes = dao.getGenesForAllele(a.getRgdId());
        for( Gene associatedGene: associatedGenes ) {
            AlleleGeneAssociationModel m = new AlleleGeneAssociationModel();
            m.allele_identifier = "RGD:"+a.getRgdId();
            m.gene_identifier = "RGD:"+associatedGene.getRgdId();
            allele_gene_association_ingest_set.add(m);
        }

        return count;
    }

    class AlleleGeneAssociationModel {
        public String allele_identifier;
        public String gene_identifier;
        public boolean internal = false;
        public String relation_name = "is_allele_of";
    }


    public void sort() {

        sort(allele_gene_association_ingest_set);
    }

    // first sort by allele RGD id, then by gene RGD id
    void sort(List<AlleleGeneAssociationModel> list) {

        Collections.sort(list, new Comparator<AlleleGeneAssociationModel>() {
            @Override
            public int compare(AlleleGeneAssociationModel a1, AlleleGeneAssociationModel a2) {
                int r = a1.allele_identifier.compareToIgnoreCase(a2.allele_identifier);
                if( r!=0 ) {
                    r = a1.gene_identifier.compareToIgnoreCase(a2.gene_identifier);
                }
                return r;
            }
        });
    }
}
