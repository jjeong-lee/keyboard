package com.example.app.salesdashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds every dashboard area from one completed-order result set so all aggregates share a snapshot.
 */
@Service
public class SalesDashboardService {

    private final SalesDashboardMapper salesDashboardMapper;

    public SalesDashboardService(SalesDashboardMapper salesDashboardMapper) {
        this.salesDashboardMapper = salesDashboardMapper;
    }

    /**
     * Loads one inclusive period and derives the summary, daily, regional, and order-list sections.
     *
     * @param startDate inclusive period start
     * @param endDate inclusive period end
     * @param region optional selected region; {@code null} represents all regions
     * @return dashboard values derived from one filtered order list
     */
    @Transactional(readOnly = true)
    public SalesDashboard getDashboard(LocalDate startDate, LocalDate endDate, Region region) {
        List<SalesOrderRow> rows = region == null
                ? salesDashboardMapper.findCompletedOrders(startDate, endDate)
                : salesDashboardMapper.findCompletedOrdersByRegion(startDate, endDate, region);
        return aggregate(rows, startDate, endDate, region);
    }

    private SalesDashboard aggregate(
            List<SalesOrderRow> rows,
            LocalDate startDate,
            LocalDate endDate,
            Region selectedRegion
    ) {
        Map<LocalDate, Long> salesByDate = new java.util.HashMap<>();
        Map<Region, RegionalTotals> totalsByRegion = new EnumMap<>(Region.class);
        List<SalesDashboard.Order> orders = new ArrayList<>();
        long totalSales = 0;

        for (SalesOrderRow row : rows) {
            long amount = row.getAmount();
            totalSales += amount;
            salesByDate.merge(row.getOrderDate(), amount, Long::sum);
            totalsByRegion.computeIfAbsent(row.getRegion(), ignored -> new RegionalTotals()).add(amount);
            orders.add(new SalesDashboard.Order(
                    row.getOrderNumber(),
                    row.getOrderDate(),
                    row.getRegion(),
                    amount
            ));
        }

        List<SalesDashboard.DailySales> dailySales = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailySales.add(new SalesDashboard.DailySales(date, salesByDate.getOrDefault(date, 0L)));
        }

        List<Region> regions = selectedRegion == null ? List.of(Region.SEOUL, Region.BUSAN) : List.of(selectedRegion);
        List<SalesDashboard.RegionalSales> regionalSales = regions.stream()
                .map(region -> {
                    RegionalTotals totals = totalsByRegion.getOrDefault(region, new RegionalTotals());
                    return new SalesDashboard.RegionalSales(region, totals.totalSales, totals.orderCount);
                })
                .toList();
        int orderCount = rows.size();
        long averageOrderAmount = orderCount == 0 ? 0 : Math.round((double) totalSales / orderCount);
        return new SalesDashboard(
                new SalesDashboard.Summary(totalSales, orderCount, averageOrderAmount),
                dailySales,
                regionalSales,
                orders
        );
    }

    private static final class RegionalTotals {
        private long totalSales;
        private int orderCount;

        private void add(long amount) {
            totalSales += amount;
            orderCount++;
        }
    }
}
