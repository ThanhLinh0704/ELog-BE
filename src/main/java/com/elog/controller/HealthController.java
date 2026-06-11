package com.elog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint — no authentication required.
 * Used by CI pipeline and load balancer health checks.
 *
 * GET /api/v1/health → 200 { "status": "UP", "version": "v1" }
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Health check — no auth required")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns UP when service is running")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "elog-backend",
                "version", "v1"
        ));
    }
}
