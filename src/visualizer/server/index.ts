/**
 * Server-side entry point for the visualizer
 * Provides SSE endpoint to bridge GCP Pub/Sub to the browser
 *
 * Run with: bun run server/index.ts
 */

import { type Message, PubSub } from "@google-cloud/pubsub";
import type { Server } from "bun";

// Configuration from environment
const PORT = Number(process.env.PORT) || 3001;
const PROJECT_ID =
  process.env.PUBSUB_PROJECT_ID || "drone-fleet-optimizer-local";
const SUBSCRIPTION_NAME = process.env.PUBSUB_SUBSCRIPTION || "telemetry-sub";
const TELEMETRY_TOPIC = process.env.PUBSUB_TOPIC || "telemetry";
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || "";

// Active SSE connections
const clients = new Set<ReadableStreamDefaultController>();

// Pub/Sub setup
const pubsub = new PubSub({ projectId: PROJECT_ID });
// Log the subscription object to verify connection
const topic = pubsub.topic(TELEMETRY_TOPIC);
topic.exists().then(([exists]) => {
  if (!exists) {
    console.error(
      `[PubSub] Topic "${TELEMETRY_TOPIC}" does not exist in project "${PROJECT_ID}"`,
    );
  } else {
    console.log(
      `[PubSub] Topic "${TELEMETRY_TOPIC}" exists in project "${PROJECT_ID}"`,
    );
  }
});
const subscription = topic.subscription(SUBSCRIPTION_NAME, {});
// console.log("[PubSub] Subscription object:", subscription);
/**
 * Handle incoming Pub/Sub messages
 */
function handleMessage(message: Message): void {
  try {
    const data = message.data.toString();

    // Broadcast to all connected clients
    for (const controller of clients) {
      try {
        controller.enqueue(`data: ${data}\n\n`);
      } catch {
        // Client disconnected, will be cleaned up
        clients.delete(controller);
      }
    }

    message.ack();
    console.log(
      `[PubSub] Message ${message.id} broadcasted to ${clients.size} clients`,
    );
  } catch (error) {
    console.error("[PubSub] Error processing message:", error);
    message.nack();
  }
}

/**
 * Start listening to Pub/Sub
 */
function startPubSubListener(): void {
  subscription.on("message", handleMessage);
  subscription.on("error", (error) => {
    console.error("[PubSub] Subscription error:", error);
  });

  console.log(`[PubSub] Listening to subscription: ${SUBSCRIPTION_NAME}`);
}

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
    return new Response("Unauthorized", { status: 401 });
  }

  const stream = new ReadableStream({
    start(controller) {
      clients.add(controller);
      console.log(`[SSE] Client connected. Total: ${clients.size}`);

      // Send initial connection message
      controller.enqueue(
        `data: {"type":"connected","timestamp":"${new Date().toISOString()}"}\n\n`,
      );
    },
    cancel() {
      // Will be cleaned up on next broadcast
      console.log("[SSE] Client disconnected");
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
  return Response.json({
    status: "ok",
    clients: clients.size,
    subscription: SUBSCRIPTION_NAME,
    timestamp: new Date().toISOString(),
  });
}

/**
 * Main request handler
 */
function handleRequest(request: Request, _server: Server): Response {
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
startPubSubListener();

const server = Bun.serve({
  port: PORT,
  fetch: handleRequest,
});

console.log(
  `[Server] Visualizer SSE bridge running on http://localhost:${server.port}`,
);
console.log(`[Server] Project: ${PROJECT_ID}`);
console.log(`[Server] Subscription: ${SUBSCRIPTION_NAME}`);
console.log(`[Server] Auth: ${ADMIN_TOKEN ? "enabled" : "disabled"}`);
