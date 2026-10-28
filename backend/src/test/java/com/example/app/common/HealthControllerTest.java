package com.example.app.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@ContextConfiguration(classes = HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scansTheApplicationAndMyBatisMapperPackagesNeededByTheReadApi() {
        SpringBootApplication application = SalesLensApplication.class.getAnnotation(SpringBootApplication.class);
        MapperScan mapperScan = SalesLensApplication.class.getAnnotation(MapperScan.class);

        assertTrue(java.util.List.of(application.scanBasePackages()).contains("com.example.app"));
        assertTrue(java.util.List.of(mapperScan.value()).contains("com.example.app.salesdashboard"));
    }

    @Test
    void includesTheFlywayPostgreSqlDatabasePluginRequiredForPostgreSql16Runtime() {
        assertDoesNotThrow(() -> Class.forName("org.flywaydb.database.postgresql.PostgreSQLDatabaseType"));
    }

    @Test
    void returnsTheContractSuccessEnvelopeWithoutSalesData() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");

        org.junit.jupiter.api.Assertions.assertTrue(contract.exists());
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta").isMap())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
