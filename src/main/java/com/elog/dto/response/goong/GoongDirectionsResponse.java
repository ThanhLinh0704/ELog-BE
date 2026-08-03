package com.elog.dto.response.goong;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GoongDirectionsResponse {

    @JsonProperty("routes")
    private List<Route> routes;

    @JsonProperty("geocoded_waypoints")
    private List<Object> geocodedWaypoints;

    @JsonProperty("status")
    private String status;

    @Data
    public static class Route {
        @JsonProperty("overview_polyline")
        private Polyline overviewPolyline;

        @JsonProperty("legs")
        private List<Leg> legs;
    }

    @Data
    public static class Polyline {
        @JsonProperty("points")
        private String points;
    }

    @Data
    public static class Leg {
        @JsonProperty("distance")
        private ValueText distance;

        @JsonProperty("duration")
        private ValueText duration;

        @JsonProperty("start_address")
        private String startAddress;

        @JsonProperty("end_address")
        private String endAddress;
    }

    @Data
    public static class ValueText {
        @JsonProperty("text")
        private String text;

        @JsonProperty("value")
        private Long value; // distance in meters, duration in seconds
    }
}
