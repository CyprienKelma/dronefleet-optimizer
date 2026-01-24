import { type Message, PubSub, type Subscription } from "@google-cloud/pubsub";
import { type DroneTelemetry, safeParseTelemetry } from "@/schemas";
import {
  logEvent,
  recordMessageFailed,
  recordMessageProcessed,
  recordMessageReceived,
  setConnectionStatus,
} from "@/stores";
import { getConfig } from "@/utils/config";

/**
 * GCP Pub/Sub client for receiving drone telemetry
 * This runs on the server-side (Bun runtime) and streams to the browser
 */

export interface PubSubClientOptions {
  projectId?: string;
  subscriptionName?: string;
  onMessage?: (telemetry: DroneTelemetry) => void;
  onError?: (error: Error) => void;
}

export class PubSubClient {
  private pubsub: PubSub;
  private subscription: Subscription | null = null;
  private subscriptionName: string;
  private onMessage: ((telemetry: DroneTelemetry) => void) | null = null;
  private onError: ((error: Error) => void) | null = null;
  private isRunning = false;

  constructor(options: PubSubClientOptions = {}) {
    const config = getConfig();

    this.pubsub = new PubSub({
      projectId: options.projectId ?? config.pubsubProjectId,
    });

    this.subscriptionName =
      options.subscriptionName ?? config.pubsubSubscription;
    this.onMessage = options.onMessage ?? null;
    this.onError = options.onError ?? null;
  }

  /**
   * Start listening for messages
   */
  async start(): Promise<void> {
    if (this.isRunning) {
      logEvent("info", "Pub/Sub client already running");
      return;
    }

    try {
      setConnectionStatus("connecting");
      logEvent("info", `Connecting to subscription: ${this.subscriptionName}`);

      this.subscription = this.pubsub.subscription(this.subscriptionName);
      this.isRunning = true;

      // Set up message handler
      this.subscription.on("message", this.handleMessage.bind(this));

      // Set up error handler
      this.subscription.on("error", this.handleError.bind(this));

      // Set up close handler
      this.subscription.on("close", () => {
        logEvent("info", "Subscription closed");
        setConnectionStatus("disconnected");
        this.isRunning = false;
      });

      setConnectionStatus("connected");
      logEvent("info", "Successfully connected to Pub/Sub");
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      setConnectionStatus("error", errorMessage);
      logEvent("error", `Failed to connect to Pub/Sub: ${errorMessage}`);
      throw error;
    }
  }

  /**
   * Stop listening for messages
   */
  async stop(): Promise<void> {
    if (!this.isRunning || !this.subscription) {
      return;
    }

    logEvent("info", "Stopping Pub/Sub client");
    this.subscription.removeAllListeners();
    await this.subscription.close();
    this.subscription = null;
    this.isRunning = false;
    setConnectionStatus("disconnected");
  }

  /**
   * Handle incoming Pub/Sub message
   */
  private handleMessage(message: Message): void {
    recordMessageReceived();

    try {
      // Parse message data as JSON
      const rawData = JSON.parse(message.data.toString());

      logEvent("telemetry", `Received message ${message.id}`, {
        messageId: message.id,
        publishTime: message.publishTime,
        attributes: message.attributes,
      });

      // Validate and parse telemetry
      const result = safeParseTelemetry(rawData);

      if (!result.success) {
        recordMessageFailed(`Validation failed: ${result.error.message}`);
        logEvent("error", "Telemetry validation failed", {
          errors: result.error.errors,
          rawData,
        });
        // Still acknowledge to avoid redelivery of invalid messages
        message.ack();
        return;
      }

      recordMessageProcessed();

      // Notify listener
      if (this.onMessage) {
        this.onMessage(result.data);
      }

      // Acknowledge the message
      message.ack();
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      recordMessageFailed(errorMessage);
      logEvent("error", `Error processing message: ${errorMessage}`, {
        messageId: message.id,
      });

      // Negative acknowledge to retry later
      message.nack();
    }
  }

  /**
   * Handle subscription errors
   */
  private handleError(error: Error): void {
    logEvent("error", `Subscription error: ${error.message}`);
    setConnectionStatus("error", error.message);

    if (this.onError) {
      this.onError(error);
    }
  }

  /**
   * Check if client is currently running
   */
  isConnected(): boolean {
    return this.isRunning;
  }
}

// Singleton instance
let _client: PubSubClient | null = null;

/**
 * Get or create the Pub/Sub client instance
 */
export function getPubSubClient(options?: PubSubClientOptions): PubSubClient {
  if (!_client) {
    _client = new PubSubClient(options);
  }
  return _client;
}

/**
 * Reset the client (for testing)
 */
export function resetPubSubClient(): void {
  if (_client) {
    _client.stop();
    _client = null;
  }
}
