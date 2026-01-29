/**
 * Server-side entry point for the visualizer
 * Provides SSE endpoint to bridge GCP Pub/Sub to the browser
 *
 * Run with: bun run server/index.ts
 */

import type { DroneTelemetry } from "@dronefleet/shared";
import { startInstrumentation } from "./instrumentation";
import { logger } from "./logger";
import { getPubSubClient } from "./pubsub-client";

// Start tracing immediately
startInstrumentation();

// Configuration from environment
const PORT = Number(process.env.PORT) || 3001;
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || "";

// Active SSE connections
const clients = new Set<ReadableStreamDefaultController>();

// Initialize Pub/Sub Client
const pubsubClient = getPubSubClient({
  onMessage: (telemetry: DroneTelemetry) => {
    // Broadcast to all connected clients
    const data = JSON.stringify(telemetry);
    const message = `data: ${data}\n\n`;

    for (const controller of clients) {
      try {
        controller.enqueue(message);
      } catch {
        // Client disconnected, will be cleaned up
        clients.delete(controller);
      }
    }
  },
  onError: (error: Error) => {
    logger.error(error, "[Server] Pub/Sub error");
  },
});

/**
 * Verify admin token
 */
function verifyToken(request: Request): boolean {
  if (!ADMIN_TOKEN) return true; // No auth required if token not set

  const url = new URL(request.url);
  const token =
    url.searchParams.get("token") ||
    request.headers.get("Authorization")?.replace("Bearer ", "");

  return token === ADMIN_TOKEN;
}

/**
 * Handle SSE connection
 */
function handleSSE(request: Request): Response {
  if (!verifyToken(request)) {
    logger.warn(
      { ip: request.headers.get("x-forwarded-for") },
      "[Server] Unauthorized connection attempt",
    );
    return new Response("Unauthorized", { status: 401 });
  }

  let controllerRef: ReadableStreamDefaultController;
  const stream = new ReadableStream({
    start(controller) {
      controllerRef = controller;
      clients.add(controller);
      logger.info({ totalClients: clients.size }, "[SSE] Client connected");

      // Send initial connection message
      controller.enqueue(
        `data: {"type":"connected","timestamp":"${new Date().toISOString()}"}\n\n`,
      );
    },
    cancel() {
      clients.delete(controllerRef);
      logger.info({ totalClients: clients.size }, "[SSE] Client disconnected");
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
      Connection: "keep-alive",
      "Access-Control-Allow-Origin": "*",
    },
  });
}

/**
 * Health check endpoint
 */
function handleHealth(): Response {
  const metrics = pubsubClient.getMetrics();

  return Response.json({
    status: "ok",
    clients: clients.size,
    subscription: {
      connected: pubsubClient.isConnected(),
      ...metrics,
    },
    timestamp: new Date().toISOString(),
  });
}

/**
 * Main request handler
 */
function handleRequest(request: Request, _server: any): Response {
  const url = new URL(request.url);

  // Handle CORS preflight
  if (request.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, OPTIONS",
        "Access-Control-Allow-Headers": "Authorization",
      },
    });
  }

  switch (url.pathname) {
    case "/api/telemetry/stream":
      return handleSSE(request);
    case "/health":
      return handleHealth();
    default:
      return new Response("Not Found", { status: 404 });
  }
}

// Start the server
pubsubClient.start().catch((err) => {
  logger.error(err, "Failed to start Pub/Sub client");
  // Continue running server even if Pub/Sub fails (so health check works)
});

const server: any = Bun.serve({
  port: PORT,
  fetch: (request) => handleRequest(request, server),
});

logger.info(
  {
    port: server.port,
    authEnabled: !!ADMIN_TOKEN,
  },
  "[Server] Visualizer SSE bridge running",
);
