package com.example.app.salesdashboard;

import com.example.app.common.ApiResponse;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the read-only sales dashboard route defined by the internal API contract.
 */
@RestController
@RequestMapping("/api")
public class SalesDashboardController {

    private final SalesDashboardService salesDashboardService;

    public SalesDashboardController(SalesDashboardService salesDashboardService) {
        this.salesDashboardService = salesDashboardService;
    }

    /**
     * Returns all dashboard sections for one validated, inclusive period and optional region.
     *
     * @param startDate required period start in ISO date format
     * @param endDate required period end in ISO date format
     * @param region optional supported region; omission queries all regions
     * @return the documented success envelope or field-level validation errors
     */
    @GetMapping("/sales-dashboard")
    public ResponseEntity<?> getSalesDashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String region
    ) {
        List<FieldError> fieldErrors = new ArrayList<>();
        LocalDate parsedStartDate = parseDate("startDate", startDate, fieldErrors);
        LocalDate parsedEndDate = parseDate("endDate", endDate, fieldErrors);
        Region parsedRegion = parseRegion(region, fieldErrors);

        if (parsedStartDate != null && parsedEndDate != null) {
            if (parsedStartDate.isAfter(parsedEndDate)) {
                fieldErrors.add(new FieldError("endDate", "종료일은 시작일보다 빠를 수 없습니다."));
            } else if (parsedStartDate.plusDays(365).isBefore(parsedEndDate)) {
                fieldErrors.add(new FieldError("endDate", "조회 기간은 양끝 포함 최대 366일입니다."));
            }
        }

        if (!fieldErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError(false, Map.of(), fieldErrors));
        }

        SalesDashboard dashboard = salesDashboardService.getDashboard(
                parsedStartDate,
                parsedEndDate,
                parsedRegion
        );
        return ResponseEntity.ok(new ApiResponse<>(true, Map.of(), dashboard));
    }

    private LocalDate parseDate(String field, String value, List<FieldError> fieldErrors) {
        if (value == null || value.isBlank()) {
            fieldErrors.add(new FieldError(field, "필수 입력 항목입니다."));
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            fieldErrors.add(new FieldError(field, "YYYY-MM-DD 형식의 유효한 날짜여야 합니다."));
            return null;
        }
    }

    private Region parseRegion(String value, List<FieldError> fieldErrors) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Region.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            fieldErrors.add(new FieldError("region", "SEOUL 또는 BUSAN만 허용됩니다."));
            return null;
        }
    }

    private record ApiError(boolean success, Map<String, Object> meta, List<FieldError> fieldErrors) {
    }

    private record FieldError(String field, String reason) {
    }
}
