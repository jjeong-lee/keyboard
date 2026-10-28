package com.example.app.salesdashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines the complete dashboard payload returned from one filtered order snapshot.
 *
 * @param summary aggregate metrics for the selected orders
 * @param dailySales ordered daily revenue values for every date in the period
 * @param regionalSales ordered regional revenue values for the selected regions
 * @param orders completed orders matching the selected filters
 */
public record SalesDashboard(
        Summary summary,
        List<DailySales> dailySales,
        List<RegionalSales> regionalSales,
        List<Order> orders
) {

    /**
     * Defines the summary metric section of the dashboard.
     */
    public record Summary(long totalSales, int orderCount, long averageOrderAmount) {
    }

    /**
     * Defines a date and its completed-order revenue.
     */
    public record DailySales(LocalDate date, long totalSales) {
    }

    /**
     * Defines one displayed region and its completed-order aggregates.
     */
    public record RegionalSales(Region region, long totalSales, int orderCount) {
    }

    /**
     * Defines one row in the dashboard order list.
     */
    public record Order(String orderNumber, LocalDate orderDate, Region region, long amount) {
    }
}
