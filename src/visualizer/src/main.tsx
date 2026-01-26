import { type Component, onMount, Show } from "solid-js";
import { render } from "solid-js/web";
import ConfigPanel from "./components/ConfigPanel";
import DebugPanel from "./components/DebugPanel";
import DroneMap from "./components/DroneMap";
import { getTelemetryStream } from "./data";
import { logEvent, updateDroneTelemetry } from "./stores";
import { getConfig } from "./utils/config";
import "./index.css";

const App: Component = () => {
  const config = getConfig();

  onMount(() => {
    logEvent("info", "DroneFleet Visualizer starting...", {
      config: {
        debugMode: config.debugMode,
        mapCenter: config.mapCenter,
        pubsubSubscription: config.pubsubSubscription,
      },
    });

    // Start the telemetry stream
    const stream = getTelemetryStream({
      onMessage: (telemetry) => {
        updateDroneTelemetry(telemetry);
      },
      onError: (error) => {
        console.error("Telemetry stream error:", error);
      },
      useMockData: config.useMockData,
    });

    stream.start();
    logEvent("info", "Application initialized");
  });

  return (
    <div class="flex flex-col h-full bg-[#121212] text-[#e0e0e0] font-sans">
      <header class="bg-[#1a1a1a] border-b border-[#2a2a2a] p-2 px-4 flex items-center justify-between">
        <div>
          <span class="text-base font-bold text-[#4ecdc4]">DroneFleet</span>
          <span class="text-xs text-[#888] ml-2">Visualizer</span>
        </div>
      </header>

      <main class="flex-1 relative overflow-hidden">
        <ConfigPanel />
        <DroneMap />
        <Show when={config.debugMode}>
          <DebugPanel />
        </Show>
      </main>
    </div>
  );
};

const appElement = document.getElementById("app");
if (appElement) {
  render(() => <App />, appElement);
}
