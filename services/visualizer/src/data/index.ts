// Browser-side telemetry stream (SSE/Mock)
export {
  getTelemetryStream,
  resetTelemetryStream,
  TelemetryStream,
  type TelemetryStreamOptions,
} from "./telemetry-stream";

// Note: Server-side Pub/Sub client is available via direct import:
// import { PubSubClient } from "@/data/pubsub-client"
// This is kept separate to avoid bundling Node.js dependencies in the browser.
