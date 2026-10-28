export type Region = "SEOUL" | "BUSAN";

export type SalesDashboardQuery = {
  startDate: string;
  endDate: string;
  region?: Region;
};

export type SalesDashboardFieldError = {
  field: keyof SalesDashboardQuery;
  reason: string;
};

export type SalesDashboard = {
  summary: {
    totalSales: number;
    orderCount: number;
    averageOrderAmount: number;
  };
  dailySales: Array<{
    date: string;
    totalSales: number;
  }>;
  regionalSales: Array<{
    region: Region;
    totalSales: number;
    orderCount: number;
  }>;
  orders: Array<{
    orderNumber: string;
    orderDate: string;
    region: Region;
    amount: number;
  }>;
};

type ApiResponse<T> = {
  success: true;
  meta: Record<string, unknown>;
  data: T;
};

export class SalesDashboardRequestError extends Error {
  readonly status: number;
  readonly fieldErrors: SalesDashboardFieldError[];

  constructor(status: number, fieldErrors: SalesDashboardFieldError[] = []) {
    super("매출 데이터를 불러오지 못했습니다.");
    this.name = "SalesDashboardRequestError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

function parseFieldErrors(body: unknown): SalesDashboardFieldError[] {
  if (typeof body !== "object" || body === null || !("fieldErrors" in body)) {
    return [];
  }

  const { fieldErrors } = body as { fieldErrors?: unknown };
  if (!Array.isArray(fieldErrors)) {
    return [];
  }

  return fieldErrors.flatMap((item) => {
    if (typeof item !== "object" || item === null) {
      return [];
    }

    const { field, reason } = item as { field?: unknown; reason?: unknown };
    if (
      (field !== "startDate" && field !== "endDate" && field !== "region") ||
      typeof reason !== "string"
    ) {
      return [];
    }

    return [{ field, reason }];
  });
}

/**
 * Requests every dashboard result area through the approved relative API endpoint.
 */
export async function fetchSalesDashboard(
  query: SalesDashboardQuery,
): Promise<SalesDashboard> {
  const parameters = new URLSearchParams({
    startDate: query.startDate,
    endDate: query.endDate,
  });

  if (query.region !== undefined) {
    parameters.set("region", query.region);
  }

  const response = await fetch(`/api/sales-dashboard?${parameters.toString()}`);
  if (!response.ok) {
    let fieldErrors: SalesDashboardFieldError[] = [];
    if (response.status === 400) {
      try {
        fieldErrors = parseFieldErrors(await response.json());
      } catch {
        fieldErrors = [];
      }
    }

    throw new SalesDashboardRequestError(response.status, fieldErrors);
  }

  const body = (await response.json()) as ApiResponse<SalesDashboard>;
  if (!body.success || body.data === undefined) {
    throw new Error("매출 데이터를 불러오지 못했습니다.");
  }

  return body.data;
}
