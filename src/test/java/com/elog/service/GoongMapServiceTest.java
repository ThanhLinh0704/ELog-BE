package com.elog.service;

import com.elog.dto.response.goong.GoongDirectionsResponse;
import com.elog.dto.response.goong.GoongDistanceMatrixResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoongMapServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GoongMapService goongMapService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(goongMapService, "apiKey", "test_key");
        ReflectionTestUtils.setField(goongMapService, "baseUrl", "https://api.goong.io");
        ReflectionTestUtils.setField(goongMapService, "vehicle", "truck");
        ReflectionTestUtils.setField(goongMapService, "restTemplate", restTemplate);
    }

    @Test
    void isConfigured_alwaysTrue() {
        assertTrue(goongMapService.isConfigured());
    }

    @Test
    void getDirections_goongSuccess_returnsResponse() {
        GoongDirectionsResponse mockResp = new GoongDirectionsResponse();
        GoongDirectionsResponse.Route route = new GoongDirectionsResponse.Route();
        route.setLegs(List.of(new GoongDirectionsResponse.Leg()));
        mockResp.setRoutes(List.of(route));

        when(restTemplate.getForObject(contains("https://api.goong.io/Direction"), eq(GoongDirectionsResponse.class)))
                .thenReturn(mockResp);

        GoongDirectionsResponse resp = goongMapService.getDirections("21.0285,105.8542", "21.0300,105.8600", null);
        assertNotNull(resp);
        assertEquals(1, resp.getRoutes().size());
    }

    @Test
    void getDistanceMatrix_success() {
        GoongDistanceMatrixResponse mockResp = new GoongDistanceMatrixResponse();
        when(restTemplate.getForObject(contains("/DistanceMatrix"), eq(GoongDistanceMatrixResponse.class)))
                .thenReturn(mockResp);

        GoongDistanceMatrixResponse resp = goongMapService.getDistanceMatrix("21.0285,105.8542", "21.0300,105.8600");
        assertNotNull(resp);
    }

    @Test
    void getDistanceMatrix_nullApiKey_returnsNull() {
        ReflectionTestUtils.setField(goongMapService, "apiKey", null);
        assertNull(goongMapService.getDistanceMatrix("21.0285,105.8542", "21.0300,105.8600"));
    }

    @Test
    void getDirections_nullInputs_returnsNull() {
        ReflectionTestUtils.setField(goongMapService, "apiKey", null);
        assertNull(goongMapService.getDirections(null, null, null));
    }
}
