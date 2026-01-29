import { type DroneTelemetry, safeParseTelemetry } from "@dronefleet/shared";
import {
  $userConfig,
  logEvent,
  recordMessageFailed,
  recordMessageProcessed,
  recordMessageReceived,
  setConnectionStatus,
} from "@/stores";
import { getConfig } from "@/utils/config";

/**
 * Browser-side telemetry stream client
 * Connects to a server-side endpoint that bridges Pub/Sub to SSE/WebSocket
 *
 * For development/demo, also supports mock data generation
 */

export interface TelemetryStreamOptions {
  endpoint?: string;
  onMessage?: (telemetry: DroneTelemetry) => void;
  onError?: (error: Error) => void;
  useMockData?: boolean;
}

export class TelemetryStream {
  private eventSource: EventSource | null = null;
  private onMessage: ((telemetry: DroneTelemetry) => void) | null = null;
  private onError: ((error: Error) => void) | null = null;
  private endpoint: string;
  private useMockData: boolean;
  private mockInterval: ReturnType<typeof setInterval> | null = null;
  private reconnectAttempts = 0;

  constructor(options: TelemetryStreamOptions = {}) {
    const config = getConfig();

    this.endpoint = options.endpoint ?? "/api/telemetry/stream";
    this.onMessage = options.onMessage ?? null;
    this.onError = options.onError ?? null;
    this.useMockData = options.useMockData ?? config.useMockData;
  }

  /**
   * Start the telemetry stream
   */
  start(): void {
    if (this.useMockData) {
      this.startMockStream();
      return;
    }

    this.connectSSE();
  }

  /**
   * Connect to SSE endpoint
   */
  private connectSSE(): void {
    setConnectionStatus("connecting");
    logEvent("info", `Connecting to telemetry stream: ${this.endpoint}`);

    const config = getConfig();
    const ssebridgeHost = config.sseBridgeHost || window.location.host;

    // Ensure host has a protocol for URL constructor, defaulting to current location's protocol
    const base = ssebridgeHost.includes("://")
      ? ssebridgeHost
      : `${window.location.protocol}//${ssebridgeHost}`;

    const url = new URL(this.endpoint, base);

    // Add auth token if available
    if (config.adminToken) {
      url.searchParams.set("token", config.adminToken);
    }

    this.eventSource = new EventSource(url.toString());

    this.eventSource.onopen = () => {
      setConnectionStatus("connected");
      logEvent("info", "Connected to telemetry stream");
      this.reconnectAttempts = 0;
    };

    this.eventSource.onmessage = (event) => {
      this.handleMessage(event.data);
    };

    this.eventSource.onerror = () => {
      this.handleConnectionError();
    };
  }

  /**
   * Handle incoming message data
   */
  private handleMessage(data: string): void {
    recordMessageReceived();

    try {
      const rawData = JSON.parse(data);

      // Handle system messages (non-telemetry)
      if (
        typeof rawData === "object" &&
        rawData !== null &&
        "type" in rawData &&
        rawData.type === "connected"
      ) {
        logEvent("info", "Stream connected (Server handshake received)");
        recordMessageProcessed();
        return;
      }

      const result = safeParseTelemetry(rawData);

      if (!result.success) {
        recordMessageFailed(`Validation failed: ${result.error.message}`);
        logEvent("error", "Telemetry validation failed", {
          errors: result.error.issues,
        });
        return;
      }

      recordMessageProcessed();

      if (this.onMessage) {
        this.onMessage(result.data);
      }
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      recordMessageFailed(errorMessage);
      logEvent("error", `Error processing message: ${errorMessage}`);
    }
  }

  /**
   * Handle connection errors with reconnection logic
   */
  private handleConnectionError(): void {
    setConnectionStatus("error", "Connection lost");

    const config = $userConfig.get();
    if (this.reconnectAttempts < config.reconnectMaxAttempts) {
      this.reconnectAttempts++;
      const delay =
        config.reconnectBaseDelay * 2 ** (this.reconnectAttempts - 1);

      logEvent(
        "info",
        `Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${config.reconnectMaxAttempts})`,
      );

      setTimeout(() => {
        this.stop();
        this.connectSSE();
      }, delay);
    } else {
      logEvent("error", "Max reconnection attempts reached");
      if (this.onError) {
        this.onError(new Error("Max reconnection attempts reached"));
      }
    }
  }

  /**
   * Start mock data stream for development
   */
  private startMockStream(): void {
    setConnectionStatus("connected");
    logEvent("info", "Starting mock telemetry stream");

    // Generate initial set of drones
    const droneIds = [
      "DRONE-ALPHA-01",
      "DRONE-ALPHA-02",
      "DRONE-BETA-01",
      "DRONE-GAMMA-01",
      "DRONE-GAMMA-02",
    ];

    const config = getConfig();
    const [baseLng, baseLat] = config.mapCenter;

    // Initial positions around the center
    const dronePositions: Record<
      string,
      { lat: number; lng: number; heading: number }
    > = {};
    for (const id of droneIds) {
      dronePositions[id] = {
        lat: baseLat + (Math.random() - 0.5) * 0.05,
        lng: baseLng + (Math.random() - 0.5) * 0.05,
        heading: Math.random() * 360,
      };
    }

    const statuses: Array<
      "IDLE" | "MOVING" | "DELIVERING" | "CHARGING" | "MAINTENANCE"
    > = ["IDLE", "MOVING", "DELIVERING", "CHARGING", "MAINTENANCE"];

    // Emit telemetry updates every second
    this.mockInterval = setInterval(() => {
      const droneId = droneIds[Math.floor(Math.random() * droneIds.length)];
      const pos = dronePositions[droneId];

      // Move the drone slightly
      const speed = 0.0001 + Math.random() * 0.0002;
      const headingRad = (pos.heading * Math.PI) / 180;
      pos.lat += Math.cos(headingRad) * speed;
      pos.lng += Math.sin(headingRad) * speed;

      // Occasionally change heading
      if (Math.random() < 0.1) {
        pos.heading = (pos.heading + (Math.random() - 0.5) * 60) % 360;
      }

      const telemetry: DroneTelemetry = {
        drone_id: droneId,
        timestamp: new Date(),
        position: {
          lat: pos.lat,
          lon: pos.lng,
        },
        battery_percentage: 50 + Math.random() * 50,
        speed_kmh: Math.random() * 60,
        status: statuses[Math.floor(Math.random() * statuses.length)],
        current_mission_id:
          Math.random() > 0.5
            ? `MISSION-${Math.floor(Math.random() * 1000)}`
            : null,
      };

      recordMessageReceived();
      recordMessageProcessed();

      if (this.onMessage) {
        this.onMessage(telemetry);
      }
    }, 1000);
  }

  /**
   * Stop the telemetry stream
   */
  stop(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    if (this.mockInterval) {
      clearInterval(this.mockInterval);
      this.mockInterval = null;
    }

    setConnectionStatus("disconnected");
    logEvent("info", "Telemetry stream stopped");
  }

  /**
   * Check if stream is connected
   */
  isConnected(): boolean {
    if (this.useMockData) {
      return this.mockInterval !== null;
    }
    return this.eventSource?.readyState === EventSource.OPEN;
  }
}

// Singleton instance
let _stream: TelemetryStream | null = null;

/**
 * Get or create the telemetry stream instance
 */
export function getTelemetryStream(
  options?: TelemetryStreamOptions,
): TelemetryStream {
  if (!_stream) {
    _stream = new TelemetryStream(options);
  }
  return _stream;
}

/**
 * Reset the stream (for testing)
 */
export function resetTelemetryStream(): void {
  if (_stream) {
    _stream.stop();
    _stream = null;
  }
}
