import { atom, map } from "nanostores";
import { getConfig } from "@/utils/config";

/**
 * Debug/diagnostics store for monitoring and debugging
 */

export type ConnectionStatus =
  | "disconnected"
  | "connecting"
  | "connected"
  | "error";

export interface DebugEvent {
  id: string;
  timestamp: Date;
  type: "telemetry" | "error" | "connection" | "info";
  message: string;
  data?: unknown;
}

export interface DebugMetrics {
  messagesReceived: number;
  messagesProcessed: number;
  messagesFailed: number;
  lastMessageTime: Date | null;
  connectionAttempts: number;
  uptime: number; // ms since start
}

// Connection status
export const $connectionStatus = atom<ConnectionStatus>("disconnected");
export const $connectionError = atom<string | null>(null);

// Event log (circular buffer)
export const $eventLog = atom<DebugEvent[]>([]);

// Metrics
export const $metrics = map<DebugMetrics>({
  messagesReceived: 0,
  messagesProcessed: 0,
  messagesFailed: 0,
  lastMessageTime: null,
  connectionAttempts: 0,
  uptime: 0,
});

// Start time for uptime calculation
const startTime = Date.now();

/**
 * Update connection status
 */
export function setConnectionStatus(
  status: ConnectionStatus,
  error?: string,
): void {
  $connectionStatus.set(status);
  $connectionError.set(error ?? null);

  if (status === "connecting") {
    $metrics.setKey(
      "connectionAttempts",
      $metrics.get().connectionAttempts + 1,
    );
  }

  logEvent(
    "connection",
    `Connection status: ${status}${error ? ` (${error})` : ""}`,
  );
}

/**
 * Log a debug event
 */
export function logEvent(
  type: DebugEvent["type"],
  message: string,
  data?: unknown,
): void {
  const config = getConfig();
  const event: DebugEvent = {
    id: crypto.randomUUID(),
    timestamp: new Date(),
    type,
    message,
    data,
  };

  const currentLog = $eventLog.get();
  const newLog = [event, ...currentLog].slice(0, config.maxEventLogSize);
  $eventLog.set(newLog);
}

/**
 * Record a message received
 */
export function recordMessageReceived(): void {
  const metrics = $metrics.get();
  $metrics.set({
    ...metrics,
    messagesReceived: metrics.messagesReceived + 1,
    lastMessageTime: new Date(),
  });
}

/**
 * Record a message processed successfully
 */
export function recordMessageProcessed(): void {
  $metrics.setKey("messagesProcessed", $metrics.get().messagesProcessed + 1);
}

/**
 * Record a message processing failure
 */
export function recordMessageFailed(error: string): void {
  $metrics.setKey("messagesFailed", $metrics.get().messagesFailed + 1);
  logEvent("error", `Message processing failed: ${error}`);
}

/**
 * Get current uptime in milliseconds
 */
export function getUptime(): number {
  return Date.now() - startTime;
}

/**
 * Update uptime metric
 */
export function updateUptime(): void {
  $metrics.setKey("uptime", getUptime());
}

/**
 * Clear all debug data
 */
export function clearDebugData(): void {
  $eventLog.set([]);
  $metrics.set({
    messagesReceived: 0,
    messagesProcessed: 0,
    messagesFailed: 0,
    lastMessageTime: null,
    connectionAttempts: 0,
    uptime: 0,
  });
}

/**
 * Get formatted metrics for display
 */
export function getFormattedMetrics(
  metrics: DebugMetrics,
): Record<string, string> {
  const uptime = Date.now() - startTime;

  return {
    "Messages Received": metrics.messagesReceived.toString(),
    "Messages Processed": metrics.messagesProcessed.toString(),
    "Messages Failed": metrics.messagesFailed.toString(),
    "Last Message": metrics.lastMessageTime
      ? formatTimeAgo(metrics.lastMessageTime)
      : "Never",
    "Connection Attempts": metrics.connectionAttempts.toString(),
    Uptime: formatDuration(uptime),
  };
}

function formatTimeAgo(date: Date): string {
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ago`;
}

function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  }
  return `${seconds}s`;
}
