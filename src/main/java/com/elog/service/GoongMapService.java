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
     *
     * @param origin      "lat,lng"
     * @param destination "lat,lng"
     * @param waypoints   "lat1,lng1|lat2,lng2" (optional)
     * @return GoongDirectionsResponse or null if failed
     */
    public GoongDirectionsResponse getDirections(String origin, String destination, String waypoints) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Goong API Key is not configured.");
            return null;
        }

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

            return restTemplate.getForObject(url, GoongDirectionsResponse.class);
        } catch (Exception e) {
            log.error("Error calling Goong Directions API: {}", e.getMessage(), e);
            return null;
        }
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
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
