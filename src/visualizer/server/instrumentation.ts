import { getNodeAutoInstrumentations } from "@opentelemetry/auto-instrumentations-node";
import { NodeSDK } from "@opentelemetry/sdk-node";
import { logger } from "./logger";

// Initialize OpenTelemetry
const sdk = new NodeSDK({
  traceExporter: new (class {
    export(
      _spans: unknown,
      resultCallback: (result: { code: number }) => void,
    ) {
      // For now, we'll just log traces to debug if needed,
      // or implement a real exporter like OTLP later.
      // This placeholder prevents errors if no exporter is configured.
      resultCallback({ code: 0 });
    }
  })(),
  instrumentations: [getNodeAutoInstrumentations()],
});

export function startInstrumentation() {
  try {
    sdk.start();
    logger.info("[OTEL] Instrumentation started");
  } catch (error) {
    logger.error(error, "[OTEL] Failed to start instrumentation");
  }
}

// Graceful shutdown
process.on("SIGTERM", () => {
  sdk
    .shutdown()
    .then(() => logger.info("[OTEL] Tracing terminated"))
    .catch((error) => logger.error(error, "[OTEL] Error terminating tracing"))
    .finally(() => process.exit(0));
});
