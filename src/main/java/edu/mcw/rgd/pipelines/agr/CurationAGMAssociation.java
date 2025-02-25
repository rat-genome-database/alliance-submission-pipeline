package edu.mcw.rgd.pipelines.agr;

import edu.mcw.rgd.datamodel.Strain;

import java.util.*;

public class CurationAGMAssociation extends CurationObject {

    public List<AGMAlleleAssociationModel> agm_allele_association_ingest_set = new ArrayList<>();

    public void add(Strain s, Dao dao) throws Exception {

        // get agm-allele associations from RGD
        List<Integer> associatedAlleles = dao.getGeneAllelesForStrain(s.getRgdId());
        for( int associatedAllele: associatedAlleles ) {
            AGMAlleleAssociationModel m = new AGMAlleleAssociationModel();
            m.agm_subject_identifier = "RGD:"+s.getRgdId();
            m.allele_identifier = "RGD:"+associatedAllele;
            agm_allele_association_ingest_set.add(m);
        }
    }

    class AGMAlleleAssociationModel {
        public String agm_subject_identifier;
        public String allele_identifier;
        public boolean internal = false;
        public String relation_name = "contains";
        public String zygosity_curie;
    }


    public void sort() {

        sort(agm_allele_association_ingest_set);
    }

    // first sort by agm RGD id, then by gene allele RGD id
    void sort(List<AGMAlleleAssociationModel> list) {

        Collections.sort(list, new Comparator<AGMAlleleAssociationModel>() {
            @Override
            public int compare(AGMAlleleAssociationModel a1, AGMAlleleAssociationModel a2) {
                int r = a1.agm_subject_identifier.compareToIgnoreCase(a2.agm_subject_identifier);
                if( r!=0 ) {
                    r = a1.allele_identifier.compareToIgnoreCase(a2.allele_identifier);
                }
                return r;
            }
        });
    }
}
