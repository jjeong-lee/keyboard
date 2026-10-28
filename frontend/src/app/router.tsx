import { SalesDashboardPage } from "../pages/salesdashboard/SalesDashboardPage";

/**
 * Resolves the direct entry route defined for the Sales Lens dashboard.
 */
export function AppRouter() {
  if (window.location.pathname === "/") {
    return <SalesDashboardPage />;
  }

  return (
    <main aria-label="페이지를 찾을 수 없음">
      요청한 페이지를 찾을 수 없습니다.
    </main>
  );
}
