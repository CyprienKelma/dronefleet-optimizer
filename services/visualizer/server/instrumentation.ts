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
      resultCallback({ code: 0 });
    }
    shutdown(): Promise<void> {
      return Promise.resolve();
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
