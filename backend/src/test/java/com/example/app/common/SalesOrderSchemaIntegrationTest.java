package com.example.app.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SalesOrderSchemaIntegrationTest {

    private Connection connection;

    @BeforeEach
    void connectToConfiguredPostgres() throws SQLException {
        String databaseUrl = System.getenv("SALES_LENS_TEST_DB_URL");
        Assumptions.assumeTrue(
                databaseUrl != null && !databaseUrl.isBlank(),
                "SALES_LENS_TEST_DB_URL is required for PostgreSQL schema integration tests"
        );
        connection = DriverManager.getConnection(
                databaseUrl,
                System.getenv().getOrDefault("SALES_LENS_TEST_DB_USER", "sales_lens"),
                System.getenv().getOrDefault("SALES_LENS_TEST_DB_PASSWORD", "dev-only-password")
        );
        connection.setAutoCommit(false);
    }

    @AfterEach
    void rollBackDatabaseChanges() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    void acceptsTheAllowedSalesOrderValues() {
        assertDoesNotThrow(() -> insert(
                nextOrderNumber(),
                LocalDate.of(2026, 10, 4),
                "SEOUL",
                "COMPLETED",
                1L
        ));
    }

    @Test
    void rejectsMissingOrderNumber() {
        assertThrows(SQLException.class, () -> insert(
                null,
                LocalDate.of(2026, 10, 4),
                "SEOUL",
                "COMPLETED",
                1L
        ));
    }

    @Test
    void rejectsDuplicateOrderNumber() throws SQLException {
        String orderNumber = nextOrderNumber();
        insert(orderNumber, LocalDate.of(2026, 10, 4), "SEOUL", "COMPLETED", 1L);

        assertThrows(SQLException.class, () -> insert(
                orderNumber,
                LocalDate.of(2026, 10, 5),
                "BUSAN",
                "CANCELLED",
                2L
        ));
    }

    @Test
    void rejectsInvalidOrderDate() {
        assertThrows(SQLException.class, () -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sales_order (
                        order_number,
                        order_date,
                        region,
                        status,
                        amount
                    )
                    VALUES (?, ?::date, ?, ?, ?)
                    """)) {
                statement.setString(1, nextOrderNumber());
                statement.setString(2, "not-a-date");
                statement.setString(3, "SEOUL");
                statement.setString(4, "COMPLETED");
                statement.setLong(5, 1L);
                statement.executeUpdate();
            }
        });
    }

    @Test
    void rejectsUnsupportedRegion() {
        assertThrows(SQLException.class, () -> insert(
                nextOrderNumber(),
                LocalDate.of(2026, 10, 4),
                "DAEGU",
                "COMPLETED",
                1L
        ));
    }

    @Test
    void rejectsUnsupportedStatus() {
        assertThrows(SQLException.class, () -> insert(
                nextOrderNumber(),
                LocalDate.of(2026, 10, 4),
                "SEOUL",
                "PENDING",
                1L
        ));
    }

    @Test
    void rejectsAmountBelowOneWon() {
        assertThrows(SQLException.class, () -> insert(
                nextOrderNumber(),
                LocalDate.of(2026, 10, 4),
                "SEOUL",
                "COMPLETED",
                0L
        ));
    }

    @Test
    void rejectsNonIntegralAmount() {
        assertThrows(SQLException.class, () -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO sales_order (
                        order_number,
                        order_date,
                        region,
                        status,
                        amount
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, nextOrderNumber());
                statement.setObject(2, LocalDate.of(2026, 10, 4));
                statement.setString(3, "SEOUL");
                statement.setString(4, "COMPLETED");
                statement.setBigDecimal(5, new BigDecimal("1.5"));
                statement.executeUpdate();
            }
        });
    }

    private void insert(
            String orderNumber,
            LocalDate orderDate,
            String region,
            String status,
            long amount
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO sales_order (
                    order_number,
                    order_date,
                    region,
                    status,
                    amount
                )
                VALUES (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, orderNumber);
            statement.setObject(2, orderDate);
            statement.setString(3, region);
            statement.setString(4, status);
            statement.setLong(5, amount);
            statement.executeUpdate();
        }
    }

    private String nextOrderNumber() {
        return "SCHEMA-" + UUID.randomUUID();
    }
}
