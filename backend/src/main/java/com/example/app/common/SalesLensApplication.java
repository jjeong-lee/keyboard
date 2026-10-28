package com.example.app.common;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the internal Sales Lens application and its database-backed HTTP API.
 */
@SpringBootApplication(scanBasePackages = "com.example.app")
@MapperScan("com.example.app.salesdashboard")
public class SalesLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesLensApplication.class, args);
    }
}
