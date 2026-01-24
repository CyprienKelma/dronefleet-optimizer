import { getTelemetryStream } from "@/data";
import { logEvent, updateDroneTelemetry } from "@/stores";
import { getConfig } from "@/utils/config";

// Import web components (registers them)
import "@/components/drone-map";
import "@/components/debug-panel";

/**
 * Main application entry point
 */

function initApp(): void {
  const config = getConfig();

  logEvent("info", "DroneFleet Visualizer starting...", {
    config: {
      debugMode: config.debugMode,
      mapCenter: config.mapCenter,
      pubsubSubscription: config.pubsubSubscription,
    },
  });

  // Create the app layout
  const app = document.getElementById("app");
  if (!app) {
    console.error("App container not found");
    return;
  }

  app.innerHTML = `
    <style>
      .app-container {
        display: flex;
        flex-direction: column;
        height: 100%;
      }

      .header {
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
        padding: 8px 16px;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .logo {
        font-size: 16px;
        font-weight: bold;
        color: var(--color-accent);
      }

      .subtitle {
        font-size: 12px;
        color: var(--color-text-muted);
        margin-left: 8px;
      }

      .main-content {
        flex: 1;
        position: relative;
        overflow: hidden;
      }

      drone-map {
        width: 100%;
        height: 100%;
      }
    </style>

    <div class="app-container">
      <header class="header">
        <div>
          <span class="logo">DroneFleet</span>
          <span class="subtitle">Visualizer</span>
        </div>
      </header>

      <main class="main-content">
        <drone-map></drone-map>
        ${config.debugMode ? "<debug-panel></debug-panel>" : ""}
      </main>
    </div>
  `;

  // Start the telemetry stream
  const stream = getTelemetryStream({
    onMessage: (telemetry) => {
      updateDroneTelemetry(telemetry);
    },
    onError: (error) => {
      console.error("Telemetry stream error:", error);
    },
    useMockData: config.debugMode, // Use mock data in debug mode
  });

  stream.start();

  logEvent("info", "Application initialized");
}

// Initialize when DOM is ready
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initApp);
} else {
  initApp();
}
