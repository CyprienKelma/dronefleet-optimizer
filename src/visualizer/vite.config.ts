import { resolve } from "node:path";
import { defineConfig } from "vite";

export default defineConfig({
  resolve: {
    alias: {
      "@": resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
    host: true,
  },
  build: {
    target: "esnext",
    outDir: "dist",
    sourcemap: true,
  },
  // Environment variables prefixed with VITE_ are exposed to the client
  // Runtime config (like ADMIN_TOKEN) should be loaded via a separate mechanism
  envPrefix: "VITE_",
});
