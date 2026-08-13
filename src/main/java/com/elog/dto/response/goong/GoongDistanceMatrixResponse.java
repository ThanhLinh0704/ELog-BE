package com.elog.dto.response.goong;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoongDistanceMatrixResponse {

    @JsonProperty("rows")
    private List<Row> rows;

    @JsonProperty("status")
    private String status;

    @Data
    public static class Row {
        @JsonProperty("elements")
        private List<Element> elements;
    }

    @Data
    public static class Element {
        @JsonProperty("status")
        private String status;

        @JsonProperty("distance")
        private ValueText distance;

        @JsonProperty("duration")
        private ValueText duration;
    }

    @Data
    public static class ValueText {
        @JsonProperty("text")
        private String text;

        @JsonProperty("value")
        private Long value; // in meters for distance, in seconds for duration
    }
}
