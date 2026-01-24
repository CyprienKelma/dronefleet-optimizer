import {
  $connectionError,
  $connectionStatus,
  $eventLog,
  $metrics,
  type ConnectionStatus,
  type DebugEvent,
  getFormattedMetrics,
} from "@/stores";
import { $drones } from "@/stores/drones";

/**
 * DebugPanel Web Component
 * Shows connection status, metrics, and event log for debugging
 */

const STATUS_INDICATORS: Record<
  ConnectionStatus,
  { color: string; label: string }
> = {
  disconnected: { color: "#ff6b6b", label: "Disconnected" },
  connecting: { color: "#ffa500", label: "Connecting..." },
  connected: { color: "#4ecdc4", label: "Connected" },
  error: { color: "#ff6b6b", label: "Error" },
};

export class DebugPanel extends HTMLElement {
  private unsubscribes: Array<() => void> = [];
  private isExpanded = true;

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
  }

  connectedCallback(): void {
    this.render();
    this.subscribeToStores();
    this.setupEventListeners();
  }

  disconnectedCallback(): void {
    for (const unsub of this.unsubscribes) {
      unsub();
    }
    this.unsubscribes = [];
  }

  private render(): void {
    if (!this.shadowRoot) return;

    this.shadowRoot.innerHTML = `
      <style>
        :host {
          display: block;
          position: absolute;
          top: 10px;
          left: 10px;
          z-index: 1000;
          font-family: 'JetBrains Mono', monospace;
          font-size: 11px;
        }

        .panel {
          background: rgba(26, 26, 26, 0.95);
          border: 1px solid #2a2a2a;
          border-radius: 8px;
          overflow: hidden;
          min-width: 280px;
          max-width: 350px;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }

        .header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 8px 12px;
          background: #1f1f1f;
          border-bottom: 1px solid #2a2a2a;
          cursor: pointer;
          user-select: none;
        }

        .header:hover {
          background: #252525;
        }

        .title {
          font-weight: bold;
          color: #4ecdc4;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .status-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          animation: pulse 2s infinite;
        }

        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }

        .toggle-icon {
          color: #888;
          transition: transform 0.2s;
        }

        .toggle-icon.collapsed {
          transform: rotate(-90deg);
        }

        .content {
          max-height: 400px;
          overflow: hidden;
          transition: max-height 0.3s ease-out;
        }

        .content.collapsed {
          max-height: 0;
        }

        .section {
          padding: 10px 12px;
          border-bottom: 1px solid #2a2a2a;
        }

        .section:last-child {
          border-bottom: none;
        }

        .section-title {
          font-size: 10px;
          text-transform: uppercase;
          color: #666;
          margin-bottom: 8px;
          letter-spacing: 0.5px;
        }

        .metric-row {
          display: flex;
          justify-content: space-between;
          padding: 3px 0;
          color: #e0e0e0;
        }

        .metric-label {
          color: #888;
        }

        .metric-value {
          color: #e0e0e0;
        }

        .drone-count {
          font-size: 24px;
          font-weight: bold;
          color: #4ecdc4;
          text-align: center;
          padding: 10px 0;
        }

        .event-log {
          max-height: 150px;
          overflow-y: auto;
        }

        .event-item {
          padding: 4px 0;
          border-bottom: 1px solid #222;
          display: flex;
          gap: 8px;
        }

        .event-item:last-child {
          border-bottom: none;
        }

        .event-time {
          color: #666;
          flex-shrink: 0;
        }

        .event-type {
          padding: 1px 4px;
          border-radius: 3px;
          font-size: 9px;
          text-transform: uppercase;
          flex-shrink: 0;
        }

        .event-type.telemetry {
          background: #4ecdc420;
          color: #4ecdc4;
        }

        .event-type.error {
          background: #ff6b6b20;
          color: #ff6b6b;
        }

        .event-type.connection {
          background: #ffa50020;
          color: #ffa500;
        }

        .event-type.info {
          background: #88888820;
          color: #888;
        }

        .event-message {
          color: #ccc;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .error-message {
          color: #ff6b6b;
          padding: 4px 8px;
          background: #ff6b6b10;
          border-radius: 4px;
          margin-top: 8px;
        }

        ::-webkit-scrollbar {
          width: 6px;
        }

        ::-webkit-scrollbar-track {
          background: #1a1a1a;
        }

        ::-webkit-scrollbar-thumb {
          background: #333;
          border-radius: 3px;
        }

        ::-webkit-scrollbar-thumb:hover {
          background: #444;
        }
      </style>

      <div class="panel">
        <div class="header" id="header">
          <div class="title">
            <span class="status-dot" id="status-dot"></span>
            <span>Debug Panel</span>
          </div>
          <span class="toggle-icon" id="toggle-icon">▼</span>
        </div>

        <div class="content" id="content">
          <div class="section">
            <div class="section-title">Connection</div>
            <div class="metric-row">
              <span class="metric-label">Status</span>
              <span class="metric-value" id="connection-status">-</span>
            </div>
            <div id="error-container"></div>
          </div>

          <div class="section">
            <div class="section-title">Active Drones</div>
            <div class="drone-count" id="drone-count">0</div>
          </div>

          <div class="section">
            <div class="section-title">Metrics</div>
            <div id="metrics-container"></div>
          </div>

          <div class="section">
            <div class="section-title">Event Log</div>
            <div class="event-log" id="event-log"></div>
          </div>
        </div>
      </div>
    `;
  }

  private setupEventListeners(): void {
    const header = this.shadowRoot?.getElementById("header");
    header?.addEventListener("click", () => this.togglePanel());
  }

  private togglePanel(): void {
    this.isExpanded = !this.isExpanded;

    const content = this.shadowRoot?.getElementById("content");
    const toggleIcon = this.shadowRoot?.getElementById("toggle-icon");

    if (content) {
      content.classList.toggle("collapsed", !this.isExpanded);
    }

    if (toggleIcon) {
      toggleIcon.classList.toggle("collapsed", !this.isExpanded);
    }
  }

  private subscribeToStores(): void {
    // Connection status
    this.unsubscribes.push(
      $connectionStatus.subscribe((status) => {
        this.updateConnectionStatus(status);
      }),
    );

    // Connection error
    this.unsubscribes.push(
      $connectionError.subscribe((error) => {
        this.updateConnectionError(error);
      }),
    );

    // Drone count
    this.unsubscribes.push(
      $drones.subscribe((drones) => {
        this.updateDroneCount(Object.keys(drones).length);
      }),
    );

    // Metrics
    this.unsubscribes.push(
      $metrics.subscribe(() => {
        this.updateMetrics();
      }),
    );

    // Event log
    this.unsubscribes.push(
      $eventLog.subscribe((events) => {
        this.updateEventLog(events);
      }),
    );
  }

  private updateConnectionStatus(status: ConnectionStatus): void {
    const statusDot = this.shadowRoot?.getElementById("status-dot");
    const statusText = this.shadowRoot?.getElementById("connection-status");

    const indicator = STATUS_INDICATORS[status];

    if (statusDot) {
      statusDot.style.backgroundColor = indicator.color;
    }

    if (statusText) {
      statusText.textContent = indicator.label;
      statusText.style.color = indicator.color;
    }
  }

  private updateConnectionError(error: string | null): void {
    const container = this.shadowRoot?.getElementById("error-container");
    if (!container) return;

    if (error) {
      container.innerHTML = `<div class="error-message">${error}</div>`;
    } else {
      container.innerHTML = "";
    }
  }

  private updateDroneCount(count: number): void {
    const el = this.shadowRoot?.getElementById("drone-count");
    if (el) {
      el.textContent = count.toString();
    }
  }

  private updateMetrics(): void {
    const container = this.shadowRoot?.getElementById("metrics-container");
    if (!container) return;

    const metrics = getFormattedMetrics();
    container.innerHTML = Object.entries(metrics)
      .map(
        ([label, value]) => `
        <div class="metric-row">
          <span class="metric-label">${label}</span>
          <span class="metric-value">${value}</span>
        </div>
      `,
      )
      .join("");
  }

  private updateEventLog(events: readonly DebugEvent[]): void {
    const container = this.shadowRoot?.getElementById("event-log");
    if (!container) return;

    container.innerHTML = events
      .slice(0, 20)
      .map((event) => {
        const time = event.timestamp.toLocaleTimeString();
        return `
          <div class="event-item">
            <span class="event-time">${time}</span>
            <span class="event-type ${event.type}">${event.type}</span>
            <span class="event-message" title="${event.message}">${event.message}</span>
          </div>
        `;
      })
      .join("");
  }
}

// Register the custom element
customElements.define("debug-panel", DebugPanel);
