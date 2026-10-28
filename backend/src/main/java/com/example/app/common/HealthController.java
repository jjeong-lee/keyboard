package com.example.app.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the operational health route independently from sales query behavior.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Returns the contract-defined success envelope when the service is available.
     *
     * @return a response with no operation-specific payload
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> getHealth() {
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }
}
