package com.example.app.salesdashboard;

import java.time.LocalDate;

/**
 * Materializes the columns needed to aggregate one completed sales order.
 */
public class SalesOrderRow {

    private String orderNumber;
    private LocalDate orderDate;
    private Region region;
    private long amount;

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}
