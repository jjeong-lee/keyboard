INSERT INTO sales_order (
    order_number,
    order_date,
    region,
    status,
    amount
)
VALUES
    ('DEMO-001', DATE '2026-10-01', 'SEOUL', 'COMPLETED', 100000),
    ('DEMO-002', DATE '2026-10-02', 'SEOUL', 'COMPLETED', 200000),
    ('DEMO-003', DATE '2026-10-02', 'BUSAN', 'COMPLETED', 150000),
    ('DEMO-004', DATE '2026-10-03', 'BUSAN', 'COMPLETED', 50000),
    ('DEMO-005', DATE '2026-10-03', 'SEOUL', 'CANCELLED', 300000),
    ('DEMO-006', DATE '2026-09-30', 'SEOUL', 'COMPLETED', 400000)
ON CONFLICT (order_number) DO NOTHING;
