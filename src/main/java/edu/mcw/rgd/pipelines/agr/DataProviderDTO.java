package edu.mcw.rgd.pipelines.agr;

import java.util.HashMap;
import java.util.Map;

public class DataProviderDTO {

    public boolean internal = false;

    public String source_organization_abbreviation = "RGD";

    public Map cross_reference_dto;

    public void setCrossReferenceDTO( String curie, String pageArea, String prefix ) {
        cross_reference_dto = new HashMap();
        cross_reference_dto.put("internal", false);
        cross_reference_dto.put("prefix", prefix);
        cross_reference_dto.put("referenced_curie", curie);
        cross_reference_dto.put("display_name", curie);
        cross_reference_dto.put("page_area", pageArea);
    }
}
