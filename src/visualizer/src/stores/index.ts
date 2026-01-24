// Drone telemetry store

// Debug/diagnostics store
export {
  $connectionError,
  $connectionStatus,
  $eventLog,
  $metrics,
  type ConnectionStatus,
  clearDebugData,
  type DebugEvent,
  type DebugMetrics,
  getFormattedMetrics,
  getUptime,
  logEvent,
  recordMessageFailed,
  recordMessageProcessed,
  recordMessageReceived,
  setConnectionStatus,
  updateUptime,
} from "./debug";
export {
  $drones,
  $lastUpdateTime,
  $selectedDroneId,
  clearDrones,
  getDroneCount,
  getDroneIds,
  getDroneTelemetry,
  removeDrone,
  selectDrone,
  updateDronesTelemetry,
  updateDroneTelemetry,
} from "./drones";
