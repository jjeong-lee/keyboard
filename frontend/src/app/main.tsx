import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AppRouter } from "./router";

const root = document.getElementById("root");

if (root === null) {
  throw new Error("Sales Lens application root is missing.");
}

createRoot(root).render(
  <StrictMode>
    <AppRouter />
  </StrictMode>,
);
