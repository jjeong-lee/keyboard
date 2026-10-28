CREATE TABLE IF NOT EXISTS sales_order (
    order_number VARCHAR(64) PRIMARY KEY,
    order_date DATE NOT NULL,
    region VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    CONSTRAINT sales_order_region_check CHECK (region IN ('SEOUL', 'BUSAN')),
    CONSTRAINT sales_order_status_check CHECK (status IN ('COMPLETED', 'CANCELLED')),
    CONSTRAINT sales_order_amount_check CHECK (amount >= 1)
);

COMMENT ON TABLE sales_order IS 'Sales Lens 내부 테스트용 가상 주문의 최종 금액과 조회 조건을 저장한다.';
COMMENT ON COLUMN sales_order.status IS 'COMPLETED:완료|CANCELLED:취소';

CREATE INDEX IF NOT EXISTS sales_order_completed_lookup_index
    ON sales_order (order_date, region, order_number)
    WHERE status = 'COMPLETED';
