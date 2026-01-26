import { useStore } from "@nanostores/solid";
import { type Component, createSignal, For, Show } from "solid-js";
import {
  $connectionError,
  $connectionStatus,
  $eventLog,
  getFormattedMetrics,
} from "@/stores";
import { $drones } from "@/stores/drones";

const STATUS_INDICATORS = {
  disconnected: { color: "#ff6b6b", label: "Disconnected" },
  connecting: { color: "#ffa500", label: "Connecting..." },
  connected: { color: "#4ecdc4", label: "Connected" },
  error: { color: "#ff6b6b", label: "Error" },
};

const DebugPanel: Component = () => {
  const [isExpanded, setIsExpanded] = createSignal(true);
  const connectionStatus = useStore($connectionStatus);
  const connectionError = useStore($connectionError);
  const drones = useStore($drones);
  const eventLog = useStore($eventLog);

  const droneCount = () => Object.keys(drones()).length;
  const formattedMetrics = () => getFormattedMetrics();

  return (
    <div class="fixed top-[10px] left-[10px] z-[1000] font-mono text-[11px]">
      <div class="bg-[#1a1a1a]/95 border border-[#2a2a2a] rounded-lg overflow-hidden min-width-[280px] max-width-[350px] shadow-lg">
        <button
          type="button"
          class="w-full flex items-center justify-between p-2 px-3 bg-[#1f1f1f] border-b border-[#2a2a2a] cursor-pointer select-none hover:bg-[#252525] focus:outline-none focus:ring-1 focus:ring-[#4ecdc4]"
          onClick={() => setIsExpanded(!isExpanded())}
        >
          <div class="font-bold text-[#4ecdc4] flex items-center gap-2 text-left">
            <span
              class="w-2 h-2 rounded-full animate-pulse"
              style={{
                "background-color": STATUS_INDICATORS[connectionStatus()].color,
              }}
            />
            <span>Debug Panel</span>
          </div>
          <span
            class="text-[#888] transition-transform duration-200"
            style={{
              transform: isExpanded() ? "rotate(0deg)" : "rotate(-90deg)",
            }}
          >
            ▼
          </span>
        </button>

        <div
          class={`overflow-hidden transition-[max-height] duration-300 ease-out ${
            isExpanded() ? "max-h-[400px]" : "max-h-0"
          }`}
        >
          <div class="p-2.5 px-3 border-b border-[#2a2a2a]">
            <div class="text-[10px] uppercase text-[#666] mb-2 tracking-wider">
              Connection
            </div>
            <div class="flex justify-between py-0.5 text-[#e0e0e0]">
              <span class="text-[#888]">Status</span>
              <span
                style={{ color: STATUS_INDICATORS[connectionStatus()].color }}
              >
                {STATUS_INDICATORS[connectionStatus()].label}
              </span>
            </div>
            <Show when={connectionError()}>
              <div class="text-[#ff6b6b] p-1 px-2 bg-[#ff6b6b]/10 rounded mt-2">
                {connectionError()}
              </div>
            </Show>
          </div>

          <div class="p-2.5 px-3 border-b border-[#2a2a2a]">
            <div class="text-[10px] uppercase text-[#666] mb-2 tracking-wider">
              Active Drones
            </div>
            <div class="text-2xl font-bold text-[#4ecdc4] text-center py-2.5">
              {droneCount()}
            </div>
          </div>

          <div class="p-2.5 px-3 border-b border-[#2a2a2a]">
            <div class="text-[10px] uppercase text-[#666] mb-2 tracking-wider">
              Metrics
            </div>
            <For each={Object.entries(formattedMetrics())}>
              {([label, value]) => (
                <div class="flex justify-between py-0.5 text-[#e0e0e0]">
                  <span class="text-[#888]">{label}</span>
                  <span class="text-[#e0e0e0]">{value}</span>
                </div>
              )}
            </For>
          </div>

          <div class="p-2.5 px-3">
            <div class="text-[10px] uppercase text-[#666] mb-2 tracking-wider">
              Event Log
            </div>
            <div class="max-h-[150px] overflow-y-auto scrollbar-thin scrollbar-thumb-[#333] scrollbar-track-[#1a1a1a]">
              <For each={eventLog().slice(0, 20)}>
                {(event) => (
                  <div class="py-1 border-b border-[#222] flex gap-2 last:border-0">
                    <span class="text-[#666] shrink-0">
                      {event.timestamp.toLocaleTimeString()}
                    </span>
                    <span
                      class={`px-1 rounded text-[9px] uppercase shrink-0 ${
                        event.type === "telemetry"
                          ? "bg-[#4ecdc4]/10 text-[#4ecdc4]"
                          : event.type === "error"
                            ? "bg-[#ff6b6b]/10 text-[#ff6b6b]"
                            : event.type === "connection"
                              ? "bg-[#ffa500]/10 text-[#ffa500]"
                              : "bg-[#888888]/10 text-[#888]"
                      }`}
                    >
                      {event.type}
                    </span>
                    <span
                      class="text-[#ccc] overflow-hidden text-ellipsis whitespace-nowrap"
                      title={event.message}
                    >
                      {event.message}
                    </span>
                  </div>
                )}
              </For>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DebugPanel;
