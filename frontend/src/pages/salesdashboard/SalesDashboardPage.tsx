import React, { FormEvent, useEffect, useRef, useState } from "react";
import {
  fetchSalesDashboard,
  Region,
  SalesDashboard,
  SalesDashboardRequestError,
} from "../../api/apiClient";
import "./SalesDashboardPage.css";

type DraftRegion = "" | Region;

type Filters = {
  startDate: string;
  endDate: string;
  region: DraftRegion;
};

type FieldErrors = Partial<Record<keyof Filters, string>>;

const defaultFilters: Filters = {
  startDate: "2026-10-01",
  endDate: "2026-10-31",
  region: "",
};

const wonFormatter = new Intl.NumberFormat("ko-KR");

function isValidDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return false;
  }

  const [year, month, day] = value.split("-").map(Number);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  return (
    parsed.getUTCFullYear() === year &&
    parsed.getUTCMonth() === month - 1 &&
    parsed.getUTCDate() === day
  );
}

function validateFilters(filters: Filters): FieldErrors {
  const errors: FieldErrors = {};

  if (!isValidDate(filters.startDate)) {
    errors.startDate = "시작일을 YYYY-MM-DD 형식의 유효한 날짜로 입력하세요.";
  }
  if (!isValidDate(filters.endDate)) {
    errors.endDate = "종료일을 YYYY-MM-DD 형식의 유효한 날짜로 입력하세요.";
  }
  if (errors.startDate !== undefined || errors.endDate !== undefined) {
    return errors;
  }

  const startDate = new Date(`${filters.startDate}T00:00:00Z`);
  const endDate = new Date(`${filters.endDate}T00:00:00Z`);
  if (startDate > endDate) {
    errors.endDate = "종료일은 시작일보다 빠를 수 없습니다.";
  } else if ((endDate.getTime() - startDate.getTime()) / 86_400_000 + 1 > 366) {
    errors.endDate = "조회 기간은 양끝 포함 최대 366일입니다.";
  }

  return errors;
}

function formatWon(value: number): string {
  return `${wonFormatter.format(value)}원`;
}

function regionLabel(region: Region): string {
  return region === "SEOUL" ? "서울" : "부산";
}

function DailySalesChart({
  dailySales,
}: {
  dailySales: SalesDashboard["dailySales"];
}) {
  const width = 760;
  const height = 210;
  const padding = 24;
  const maximum = Math.max(
    ...dailySales.map(({ totalSales }) => totalSales),
    1,
  );
  const points = dailySales
    .map(({ totalSales }, index) => {
      const x =
        dailySales.length === 1
          ? width / 2
          : padding + (index * (width - padding * 2)) / (dailySales.length - 1);
      const y =
        height - padding - (totalSales / maximum) * (height - padding * 2);
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <section
      className="sales-card sales-chart-card"
      aria-labelledby="daily-sales-title"
    >
      <div className="sales-card-heading">
        <div>
          <h2 id="daily-sales-title">일별 매출 추이</h2>
          <p>날짜 오름차순 · 주문 없는 날은 0원</p>
        </div>
      </div>
      <div className="sales-chart-wrap">
        <svg
          aria-label="일별 매출 선 그래프"
          viewBox={`0 0 ${width} ${height}`}
          role="img"
        >
          <line
            className="sales-chart-axis"
            x1={padding}
            x2={width - padding}
            y1={height - padding}
            y2={height - padding}
          />
          <polyline className="sales-chart-line" points={points} />
          {dailySales.map(({ date, totalSales }, index) => {
            const [x, y] = points.split(" ")[index].split(",");
            return (
              <circle
                className="sales-chart-point"
                cx={x}
                cy={y}
                key={date}
                r="3"
              >
                <title>{`${date} ${formatWon(totalSales)}`}</title>
              </circle>
            );
          })}
        </svg>
      </div>
    </section>
  );
}

/**
 * Displays the Sales Lens dashboard with independently editable and applied filters.
 */
export function SalesDashboardPage() {
  const [draftFilters, setDraftFilters] = useState<Filters>(defaultFilters);
  const [appliedFilters, setAppliedFilters] = useState<Filters | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [dashboard, setDashboard] = useState<SalesDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const initialized = useRef(false);

  async function load(filters: Filters) {
    const errors = validateFilters(filters);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    setFieldErrors({});
    setLoading(true);
    setRequestError(null);
    setPermissionDenied(false);
    try {
      const result = await fetchSalesDashboard({
        startDate: filters.startDate,
        endDate: filters.endDate,
        ...(filters.region === "" ? {} : { region: filters.region }),
      });
      setDashboard(result);
      setAppliedFilters(filters);
    } catch (error) {
      if (
        error instanceof SalesDashboardRequestError &&
        error.fieldErrors.length > 0
      ) {
        setFieldErrors(
          Object.fromEntries(
            error.fieldErrors.map(({ field, reason }) => [field, reason]),
          ),
        );
        return;
      }

      setDashboard(null);
      setAppliedFilters(null);
      if (
        error instanceof SalesDashboardRequestError &&
        [401, 403].includes(error.status)
      ) {
        setPermissionDenied(true);
      } else {
        setRequestError(
          "매출 데이터를 불러오지 못했습니다. 잠시 후 다시 조회하세요.",
        );
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (initialized.current) {
      return;
    }
    initialized.current = true;
    void load(defaultFilters);
  }, []);

  function updateFilter(field: keyof Filters, value: string) {
    setDraftFilters((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!loading) {
      void load(draftFilters);
    }
  }

  const isEmpty = dashboard !== null && dashboard.orders.length === 0;

  return (
    <main className="sales-dashboard" aria-label="Sales Lens 매출 대시보드">
      <header className="sales-site-header">
        <span className="sales-brand">
          <span aria-hidden="true">S</span>
          Sales Lens
        </span>
        <span>매출 대시보드</span>
      </header>
      <div className="sales-content">
        <header className="sales-page-header">
          <div>
            <p className="sales-eyebrow">INTERNAL SALES</p>
            <h1>Sales Lens</h1>
            <p className="sales-subtitle">
              기간과 지역별 매출 현황을 한눈에 확인하세요.
            </p>
          </div>
          {appliedFilters !== null && (
            <p className="sales-applied-condition" aria-live="polite">
              적용 조건: {appliedFilters.startDate} ~ {appliedFilters.endDate}
              {appliedFilters.region === ""
                ? " · 전체 지역"
                : ` · ${regionLabel(appliedFilters.region)}`}
            </p>
          )}
        </header>

        <form className="sales-filter-card" onSubmit={submit} noValidate>
          <label>
            시작일
            <input
              aria-invalid={fieldErrors.startDate !== undefined}
              aria-describedby={
                fieldErrors.startDate === undefined
                  ? undefined
                  : "start-date-error"
              }
              type="date"
              value={draftFilters.startDate}
              onChange={(event) =>
                updateFilter("startDate", event.target.value)
              }
            />
            {fieldErrors.startDate !== undefined && (
              <span id="start-date-error" className="sales-field-error">
                {fieldErrors.startDate}
              </span>
            )}
          </label>
          <label>
            종료일
            <input
              aria-invalid={fieldErrors.endDate !== undefined}
              aria-describedby={
                fieldErrors.endDate === undefined ? undefined : "end-date-error"
              }
              type="date"
              value={draftFilters.endDate}
              onChange={(event) => updateFilter("endDate", event.target.value)}
            />
            {fieldErrors.endDate !== undefined && (
              <span id="end-date-error" className="sales-field-error">
                {fieldErrors.endDate}
              </span>
            )}
          </label>
          <label>
            지역
            <select
              aria-invalid={fieldErrors.region !== undefined}
              aria-describedby={
                fieldErrors.region === undefined ? undefined : "region-error"
              }
              value={draftFilters.region}
              onChange={(event) => updateFilter("region", event.target.value)}
            >
              <option value="">전체</option>
              <option value="SEOUL">서울</option>
              <option value="BUSAN">부산</option>
            </select>
            {fieldErrors.region !== undefined && (
              <span id="region-error" className="sales-field-error">
                {fieldErrors.region}
              </span>
            )}
          </label>
          <button type="submit" disabled={loading}>
            {loading ? "조회 중..." : "조회"}
          </button>
        </form>

        {requestError !== null && (
          <section className="sales-request-error" role="alert">
            <span>{requestError}</span>
            <button
              type="button"
              onClick={() => !loading && void load(draftFilters)}
              disabled={loading}
            >
              재시도
            </button>
          </section>
        )}

        {permissionDenied && !loading && (
          <section className="sales-state-card" role="alert">
            <h2>이 매출 정보에 접근할 수 없습니다.</h2>
            <p>
              내부 접근 권한이 확인되지 않았습니다. 관리자에게 접근 방법을
              확인하세요.
            </p>
          </section>
        )}

        {loading && (
          <div
            className="sales-loading"
            role="status"
            aria-label="매출 데이터를 조회하고 있습니다."
          >
            <span />
            <span />
            <span />
          </div>
        )}

        {dashboard !== null && !loading && (
          <>
            <section className="sales-summary-grid" aria-label="요약 지표">
              <article className="sales-card sales-metric-card">
                <p>총매출</p>
                <strong>{formatWon(dashboard.summary.totalSales)}</strong>
              </article>
              <article className="sales-card sales-metric-card">
                <p>주문건수</p>
                <strong>
                  {wonFormatter.format(dashboard.summary.orderCount)}건
                </strong>
              </article>
              <article className="sales-card sales-metric-card">
                <p>평균 주문금액</p>
                <strong>
                  {formatWon(dashboard.summary.averageOrderAmount)}
                </strong>
              </article>
            </section>

            <section className="sales-analysis-grid">
              <DailySalesChart dailySales={dashboard.dailySales} />
              <section
                className="sales-card sales-region-card"
                aria-labelledby="regional-sales-title"
              >
                <div className="sales-card-heading">
                  <div>
                    <h2 id="regional-sales-title">지역별 실적</h2>
                    <p>완료 주문 기준 집계</p>
                  </div>
                </div>
                <table>
                  <thead>
                    <tr>
                      <th>지역</th>
                      <th>총매출</th>
                      <th>주문건수</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboard.regionalSales.map((item) => (
                      <tr key={item.region}>
                        <td>{regionLabel(item.region)}</td>
                        <td>{formatWon(item.totalSales)}</td>
                        <td>{wonFormatter.format(item.orderCount)}건</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>
            </section>

            <section
              className="sales-card sales-orders-card"
              aria-labelledby="orders-title"
            >
              <div className="sales-card-heading">
                <div>
                  <h2 id="orders-title">주문 목록</h2>
                  <p>주문일자 내림차순 · 동일 날짜 주문번호 오름차순</p>
                </div>
              </div>
              {isEmpty ? (
                <p className="sales-empty">조회 결과가 없습니다</p>
              ) : (
                <div className="sales-table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>주문번호</th>
                        <th>주문일자</th>
                        <th>지역명</th>
                        <th>주문금액</th>
                      </tr>
                    </thead>
                    <tbody>
                      {dashboard.orders.map((order) => (
                        <tr key={order.orderNumber}>
                          <td>{order.orderNumber}</td>
                          <td>{order.orderDate}</td>
                          <td>{regionLabel(order.region)}</td>
                          <td>{formatWon(order.amount)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </main>
  );
}
