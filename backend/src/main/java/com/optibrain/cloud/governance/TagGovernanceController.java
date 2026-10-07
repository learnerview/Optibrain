package com.optibrain.cloud.governance;

import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tag compliance reporting.
 *
 * <p>Exposes the audit-only report described in {@link TagGovernanceService}. No
 * endpoint mutates tags, because enforcement belongs to AWS tag policies rather than to
 * this application.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagGovernanceController {

    private final TagGovernanceService tagGovernance;

    /**
     * Tag coverage report.
     *
     * <p>{@code keys} is optional. Absent or empty, the report uses the current
     * tenant's configured required keys, falling back to the platform defaults
     * ({@code Environment}, {@code Owner}).
     *
     * <p>Declared once rather than as an overload pair: two handlers on the same path
     * would be an ambiguous mapping and fail at startup.
     */
    @GetMapping("/coverage")
    public ResponseEntity<ApiResponse<TagCoverageReport>> coverage(
            @RequestParam(name = "keys", required = false) List<String> keys) {
        return ResponseEntity.ok(ApiResponse.success(tagGovernance.report(keys),
                "Tag coverage retrieved"));
    }
}