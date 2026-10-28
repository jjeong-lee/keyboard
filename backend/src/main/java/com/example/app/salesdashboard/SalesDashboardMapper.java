package com.example.app.salesdashboard;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Reads completed orders for one dashboard request from PostgreSQL.
 */
@Mapper
public interface SalesDashboardMapper {

    /**
     * Reads all completed orders in an inclusive period when no regional filter is selected.
     *
     * @param startDate inclusive period start
     * @param endDate inclusive period end
     * @return rows ordered for the dashboard order list
     */
    @Select("""
            SELECT
                so.order_number AS "orderNumber",
                so.order_date AS "orderDate",
                so.region AS region,
                so.amount AS amount
            FROM sales_order so
            WHERE so.status = 'COMPLETED'
              AND so.order_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY so.order_date DESC, so.order_number ASC
            """)
    List<SalesOrderRow> findCompletedOrders(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Reads all completed orders in an inclusive period for one selected region.
     *
     * @param startDate inclusive period start
     * @param endDate inclusive period end
     * @param region selected region
     * @return rows ordered for the dashboard order list
     */
    @Select("""
            SELECT
                so.order_number AS "orderNumber",
                so.order_date AS "orderDate",
                so.region AS region,
                so.amount AS amount
            FROM sales_order so
            WHERE so.status = 'COMPLETED'
              AND so.order_date BETWEEN #{startDate} AND #{endDate}
              AND so.region = #{region}
            ORDER BY so.order_date DESC, so.order_number ASC
            """)
    List<SalesOrderRow> findCompletedOrdersByRegion(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("region") Region region
    );
}
