package com.example.app.salesdashboard;

import com.example.app.common.SalesLensApplication;
import com.example.app.salesdashboard.Region;
import com.example.app.salesdashboard.SalesDashboard;
import com.example.app.salesdashboard.SalesDashboardController;
import com.example.app.salesdashboard.SalesDashboardService;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SalesDashboardController.class)
@Import(SalesDashboardController.class)
@ContextConfiguration(classes = SalesDashboardController.class)
class SalesDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SalesDashboardService salesDashboardService;

    @Test
    void returnsDashboardEnvelopeForTheApprovedQuery() throws Exception {
        ClassPathResource contract = new ClassPathResource("contracts/openapi.yaml");
        Assertions.assertTrue(contract.exists());
        SalesDashboard dashboard = new SalesDashboard(
                new SalesDashboard.Summary(500000, 4, 125000),
                List.of(new SalesDashboard.DailySales(LocalDate.of(2026, 10, 1), 100000)),
                List.of(
                        new SalesDashboard.RegionalSales(Region.SEOUL, 300000, 2),
                        new SalesDashboard.RegionalSales(Region.BUSAN, 200000, 2)
                ),
                List.of(new SalesDashboard.Order(
                        "DEMO-004",
                        LocalDate.of(2026, 10, 3),
                        Region.BUSAN,
                        50000
                ))
        );
        when(salesDashboardService.getDashboard(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                null
        )).thenReturn(dashboard);

        mockMvc.perform(get("/api/sales-dashboard")
                        .queryParam("startDate", "2026-10-01")
                        .queryParam("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta").isMap())
                .andExpect(jsonPath("$.data.summary.totalSales").value(500000))
                .andExpect(jsonPath("$.data.summary.orderCount").value(4))
                .andExpect(jsonPath("$.data.dailySales[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.data.regionalSales[0].region").value("SEOUL"))
                .andExpect(jsonPath("$.data.orders[0].orderNumber").value("DEMO-004"));

        verify(salesDashboardService).getDashboard(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                null
        );
    }

    @Test
    void returnsFieldErrorsForAnInvalidPeriodWithoutQueryingSales() throws Exception {
        mockMvc.perform(get("/api/sales-dashboard")
                        .queryParam("startDate", "2026-10-31")
                        .queryParam("endDate", "2026-10-01")
                        .queryParam("region", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.meta").isMap())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("region"));

        verifyNoInteractions(salesDashboardService);
    }
}
