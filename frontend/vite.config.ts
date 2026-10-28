import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Without the React plugin Vite compiles JSX to React.createElement, which fails at runtime
// in files that do not import React (main.tsx, router.tsx) and leaves a blank page.
export default defineConfig({
  plugins: [react()],
});
