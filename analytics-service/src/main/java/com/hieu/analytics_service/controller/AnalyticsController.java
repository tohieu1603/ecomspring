package com.hieu.analytics_service.controller;

import com.hieu.analytics_service.service.AnalyticsQueryService;
import com.hieu.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Thin admin endpoints — Kibana is the primary UI; these are for sanity-check / scripts. */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Sanity-check queries — Kibana is the main dashboard UI")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Aggregate counts + revenue between [from, to)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(queryService.summary(from, to)));
    }
}
