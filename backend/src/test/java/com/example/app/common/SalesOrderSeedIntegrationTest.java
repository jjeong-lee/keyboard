package com.example.app.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class SalesOrderSeedIntegrationTest {

    @Test
    void initializesEachConfiguredOrderExactlyOnceAfterRepeatedStartup() throws SQLException {
        String databaseUrl = System.getenv("SALES_LENS_TEST_DB_URL");
        Assumptions.assumeTrue(
                databaseUrl != null && !databaseUrl.isBlank(),
                "SALES_LENS_TEST_DB_URL is required for PostgreSQL seed integration tests"
        );

        try (Connection connection = DriverManager.getConnection(
                databaseUrl,
                System.getenv().getOrDefault("SALES_LENS_TEST_DB_USER", "sales_lens"),
                System.getenv().getOrDefault("SALES_LENS_TEST_DB_PASSWORD", "dev-only-password")
        )) {
            Map<String, SeedOrder> expected = expectedOrders();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT
                        order_number,
                        order_date,
                        region,
                        status,
                        amount
                    FROM sales_order
                    WHERE order_number LIKE 'DEMO-%'
                    ORDER BY order_number
                    """)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    int count = 0;
                    while (resultSet.next()) {
                        count++;
                        String orderNumber = resultSet.getString("order_number");
                        assertEquals(expected.remove(orderNumber), new SeedOrder(
                                resultSet.getObject("order_date", LocalDate.class),
                                resultSet.getString("region"),
                                resultSet.getString("status"),
                                resultSet.getLong("amount")
                        ));
                    }
                    assertEquals(6, count);
                    assertEquals(Map.of(), expected);
                }
            }
        }
    }

    private Map<String, SeedOrder> expectedOrders() {
        Map<String, SeedOrder> orders = new LinkedHashMap<>();
        orders.put("DEMO-001", new SeedOrder(LocalDate.of(2026, 10, 1), "SEOUL", "COMPLETED", 100000L));
        orders.put("DEMO-002", new SeedOrder(LocalDate.of(2026, 10, 2), "SEOUL", "COMPLETED", 200000L));
        orders.put("DEMO-003", new SeedOrder(LocalDate.of(2026, 10, 2), "BUSAN", "COMPLETED", 150000L));
        orders.put("DEMO-004", new SeedOrder(LocalDate.of(2026, 10, 3), "BUSAN", "COMPLETED", 50000L));
        orders.put("DEMO-005", new SeedOrder(LocalDate.of(2026, 10, 3), "SEOUL", "CANCELLED", 300000L));
        orders.put("DEMO-006", new SeedOrder(LocalDate.of(2026, 9, 30), "SEOUL", "COMPLETED", 400000L));
        return orders;
    }

    private record SeedOrder(LocalDate orderDate, String region, String status, long amount) {
    }
}
