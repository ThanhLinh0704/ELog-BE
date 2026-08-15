package com.elog.service;

import com.elog.dto.response.goong.GoongDirectionsResponse;
import com.elog.dto.response.goong.GoongDistanceMatrixResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class GoongMapService {

    @Value("${elog.goong.api-key:66VHUZVYkINm140kfOtXvdNVlO9UzZByASkqMh5q}")
    private String apiKey;

    @Value("${elog.goong.base-url:https://api.goong.io}")
    private String baseUrl;

    @Value("${elog.goong.vehicle:truck}")
    private String vehicle;

    private final RestTemplate restTemplate;

    public GoongMapService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(7000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Get Directions between origin, waypoints, and destination from Goong Directions API.
     * Fallbacks to OSRM Public Server if Goong is unavailable or rate-limited.
     *
     * @param origin      "lat,lng"
     * @param destination "lat,lng"
     * @param waypoints   "lat1,lng1|lat2,lng2" (optional)
     * @return GoongDirectionsResponse or null if failed
     */
    public GoongDirectionsResponse getDirections(String origin, String destination, String waypoints) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append(baseUrl).append("/Direction")
                        .append("?origin=").append(origin)
                        .append("&destination=").append(destination)
                        .append("&vehicle=").append(vehicle)
                        .append("&api_key=").append(apiKey);

                if (waypoints != null && !waypoints.trim().isEmpty()) {
                    urlBuilder.append("&waypoints=").append(waypoints);
                }

                String url = urlBuilder.toString();
                log.info("Calling Goong Directions API URL: {}", url);

                GoongDirectionsResponse res = restTemplate.getForObject(url, GoongDirectionsResponse.class);
                if (res != null && res.getRoutes() != null && !res.getRoutes().isEmpty()) {
                    int expectedLegs = 1;
                    if (waypoints != null && !waypoints.trim().isEmpty()) {
                        expectedLegs += waypoints.split("\\|").length;
                    }
                    int actualLegs = (res.getRoutes().get(0).getLegs() != null) ? res.getRoutes().get(0).getLegs().size() : 0;

                    if (actualLegs >= expectedLegs) {
                        return res;
                    }
                    log.warn("Goong Directions API ignored waypoints (returned {} legs, expected {}). Triggering OSRM fallback...", actualLegs, expectedLegs);
                } else {
                    log.warn("Goong Directions API returned empty routes or rate limit error. Triggering OSRM fallback...");
                }
            } catch (Exception e) {
                log.error("Error calling Goong Directions API: {}. Triggering OSRM fallback...", e.getMessage());
            }
        } else {
            log.warn("Goong API Key is not configured. Triggering OSRM fallback...");
        }

        return getDirectionsFromOsrmFallback(origin, destination, waypoints);
    }

    /**
     * Fallback to OSRM Public Server when Goong is unavailable or rate-limited.
     * OSRM API expects longitude,latitude format separated by semicolons.
     */
    private GoongDirectionsResponse getDirectionsFromOsrmFallback(String origin, String destination, String waypoints) {
        if (origin == null || destination == null) {
            return null;
        }
        try {
            java.util.List<String> osrmPoints = new java.util.ArrayList<>();
            osrmPoints.add(formatToLngLat(origin));

            if (waypoints != null && !waypoints.trim().isEmpty()) {
                String[] pts = waypoints.split("\\|");
                for (String pt : pts) {
                    if (!pt.trim().isEmpty()) {
                        osrmPoints.add(formatToLngLat(pt.trim()));
                    }
                }
            }

            osrmPoints.add(formatToLngLat(destination));

            String osrmCoordsStr = String.join(";", osrmPoints);
            String url = "https://router.project-osrm.org/route/v1/driving/" + osrmCoordsStr + "?overview=full&geometries=polyline";

            log.info("Calling OSRM Fallback API URL: {}", url);

            OsrmRouteResponse osrmRes = restTemplate.getForObject(url, OsrmRouteResponse.class);
            if (osrmRes != null && osrmRes.getRoutes() != null && !osrmRes.getRoutes().isEmpty()) {
                OsrmRouteResponse.OsrmRoute osrmRoute = osrmRes.getRoutes().get(0);

                GoongDirectionsResponse.Polyline polyline = new GoongDirectionsResponse.Polyline();
                polyline.setPoints(osrmRoute.getGeometry());

                java.util.List<GoongDirectionsResponse.Leg> legs = new java.util.ArrayList<>();
                if (osrmRoute.getLegs() != null) {
                    for (OsrmRouteResponse.OsrmLeg oLeg : osrmRoute.getLegs()) {
                        GoongDirectionsResponse.Leg leg = new GoongDirectionsResponse.Leg();

                        GoongDirectionsResponse.ValueText dist = new GoongDirectionsResponse.ValueText();
                        long distMeters = Math.round(oLeg.getDistance());
                        dist.setValue(distMeters);
                        dist.setText(String.format("%.1f km", distMeters / 1000.0));
                        leg.setDistance(dist);

                        GoongDirectionsResponse.ValueText dur = new GoongDirectionsResponse.ValueText();
                        long durSecs = Math.round(oLeg.getDuration());
                        dur.setValue(durSecs);
                        dur.setText(String.format("%d phút", Math.round(durSecs / 60.0)));
                        leg.setDuration(dur);

                        legs.add(leg);
                    }
                }

                GoongDirectionsResponse.Route gRoute = new GoongDirectionsResponse.Route();
                gRoute.setOverviewPolyline(polyline);
                gRoute.setLegs(legs);

                GoongDirectionsResponse response = new GoongDirectionsResponse();
                response.setRoutes(java.util.List.of(gRoute));
                response.setStatus("OK");

                log.info("Successfully fetched route from OSRM fallback with {} legs", legs.size());
                return response;
            }
        } catch (Exception e) {
            log.error("Error calling OSRM Fallback API: {}", e.getMessage(), e);
        }
        return null;
    }

    private String formatToLngLat(String latLngStr) {
        String[] parts = latLngStr.split(",");
        if (parts.length == 2) {
            return parts[1].trim() + "," + parts[0].trim();
        }
        return latLngStr;
    }

    /**
     * Get Distance Matrix between origins and destinations.
     */
    public GoongDistanceMatrixResponse getDistanceMatrix(String origins, String destinations) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Goong API Key is not configured.");
            return null;
        }

        try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(baseUrl).append("/DistanceMatrix")
                    .append("?origins=").append(origins)
                    .append("&destinations=").append(destinations)
                    .append("&vehicle=").append(vehicle)
                    .append("&api_key=").append(apiKey);

            String url = urlBuilder.toString();
            log.info("Calling Goong Distance Matrix API URL: {}", url);
            return restTemplate.getForObject(url, GoongDistanceMatrixResponse.class);
        } catch (Exception e) {
            log.error("Error calling Goong Distance Matrix API: {}", e.getMessage(), e);
            return null;
        }
    }


    public boolean isConfigured() {
        return true; // Always return true as OSRM Public fallback is available
    }

    @lombok.Data
    private static class OsrmRouteResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("code")
        private String code;

        @com.fasterxml.jackson.annotation.JsonProperty("routes")
        private java.util.List<OsrmRoute> routes;

        @lombok.Data
        public static class OsrmRoute {
            @com.fasterxml.jackson.annotation.JsonProperty("geometry")
            private String geometry;

            @com.fasterxml.jackson.annotation.JsonProperty("distance")
            private double distance;

            @com.fasterxml.jackson.annotation.JsonProperty("duration")
            private double duration;

            @com.fasterxml.jackson.annotation.JsonProperty("legs")
            private java.util.List<OsrmLeg> legs;
        }

        @lombok.Data
        public static class OsrmLeg {
            @com.fasterxml.jackson.annotation.JsonProperty("distance")
            private double distance;

            @com.fasterxml.jackson.annotation.JsonProperty("duration")
            private double duration;
        }
    }
}
