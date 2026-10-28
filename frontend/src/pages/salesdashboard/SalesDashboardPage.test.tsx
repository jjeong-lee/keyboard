// @vitest-environment jsdom
import React from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SalesDashboardPage } from "./SalesDashboardPage";

const dashboardResponse = {
  success: true,
  data: {
    summary: {
      totalSales: 500000,
      orderCount: 4,
      averageOrderAmount: 125000,
    },
    dailySales: [
      { date: "2026-10-01", totalSales: 100000 },
      { date: "2026-10-02", totalSales: 350000 },
    ],
    regionalSales: [
      { region: "SEOUL", totalSales: 300000, orderCount: 2 },
      { region: "BUSAN", totalSales: 200000, orderCount: 2 },
    ],
    orders: [
      {
        orderNumber: "DEMO-004",
        orderDate: "2026-10-03",
        region: "BUSAN",
        amount: 50000,
      },
    ],
  },
};

describe("SalesDashboardPage", () => {
  const fetchStub = vi.fn();

  beforeEach(() => {
    fetchStub.mockResolvedValue({
      ok: true,
      json: async () => dashboardResponse,
    });
    vi.stubGlobal("fetch", fetchStub);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    fetchStub.mockReset();
  });

  it("loads the default dashboard query and presents all result areas", async () => {
    render(<SalesDashboardPage />);

    await screen.findByText("500,000원");

    expect(fetchStub).toHaveBeenCalledWith(
      "/api/sales-dashboard?startDate=2026-10-01&endDate=2026-10-31",
    );
    expect(screen.getByText("일별 매출 추이")).toBeTruthy();
    expect(screen.getByText("지역별 실적")).toBeTruthy();
    expect(screen.getByText("주문 목록")).toBeTruthy();
    expect(screen.getByText("DEMO-004")).toBeTruthy();
  });

  it("keeps the applied result when an invalid draft period is submitted", async () => {
    render(<SalesDashboardPage />);

    await screen.findByText("500,000원");
    fireEvent.change(screen.getByLabelText("시작일"), {
      target: { value: "2026-11-01" },
    });
    fireEvent.change(screen.getByLabelText("종료일"), {
      target: { value: "2026-10-01" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(
      await screen.findByText("종료일은 시작일보다 빠를 수 없습니다."),
    ).toBeTruthy();
    expect(screen.getByText("500,000원")).toBeTruthy();
    await waitFor(() => expect(fetchStub).toHaveBeenCalledTimes(1));
  });

  it("shows API field errors without replacing the last applied dashboard", async () => {
    render(<SalesDashboardPage />);

    await screen.findByText("500,000원");
    fetchStub.mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({
        success: false,
        meta: {},
        fieldErrors: [
          {
            field: "startDate",
            reason: "서버 기준 시작일이 아닙니다.",
          },
        ],
      }),
    });

    fireEvent.change(screen.getByLabelText("시작일"), {
      target: { value: "2026-10-02" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(
      await screen.findByText("서버 기준 시작일이 아닙니다."),
    ).toBeTruthy();
    expect(screen.getByText("500,000원")).toBeTruthy();
    expect(
      screen.getByText("적용 조건: 2026-10-01 ~ 2026-10-31 · 전체 지역"),
    ).toBeTruthy();
  });

  it("shows API region errors next to the region selector", async () => {
    render(<SalesDashboardPage />);

    await screen.findByText("500,000원");
    fetchStub.mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({
        success: false,
        meta: {},
        fieldErrors: [
          {
            field: "region",
            reason: "선택한 지역을 조회할 수 없습니다.",
          },
        ],
      }),
    });

    fireEvent.change(screen.getByLabelText("지역"), {
      target: { value: "SEOUL" },
    });
    fireEvent.click(screen.getByRole("button", { name: "조회" }));

    expect(
      await screen.findByText("선택한 지역을 조회할 수 없습니다."),
    ).toBeTruthy();
    expect(screen.getByRole("combobox").getAttribute("aria-invalid")).toBe(
      "true",
    );
  });
});
