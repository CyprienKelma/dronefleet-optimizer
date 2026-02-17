import { DroneTelemetry } from "@dronefleet/shared";
import { type Message, PubSub, type Subscription } from "@google-cloud/pubsub";
import { logger } from "./logger";

/**
 * GCP Pub/Sub client for receiving drone telemetry
 * Server-side implementation
 */

export interface PubSubClientOptions {
  projectId?: string;
  subscriptionName?: string;
  onMessage?: (telemetry: DroneTelemetry) => void;
  onError?: (error: Error) => void;
}

export interface PubSubMetrics {
  messagesReceived: number;
  messagesProcessed: number;
  messagesFailed: number;
  lastMessageTime: Date | null;
}

export class PubSubClient {
  private pubsub: PubSub;
  private subscription: Subscription | null = null;
  private subscriptionName: string;
  private onMessage: ((telemetry: DroneTelemetry) => void) | null = null;
  private onError: ((error: Error) => void) | null = null;
  private isRunning = false;

  // Metrics
  private metrics: PubSubMetrics = {
    messagesReceived: 0,
    messagesProcessed: 0,
    messagesFailed: 0,
    lastMessageTime: null,
  };

  constructor(options: PubSubClientOptions = {}) {
    const projectId =
      options.projectId ||
      process.env.PUBSUB_PROJECT_ID ||
      process.env.PROJECT_ID ||
      "drone-fleet-optimizer-local";

    const clientConfig: {
      projectId: string;
      apiEndpoint?: string;
      credentials?: { client_email: string; private_key: string };
    } = { projectId };

    // Explicitly configure emulator if host is provided
    if (process.env.PUBSUB_EMULATOR_HOST) {
      clientConfig.apiEndpoint = process.env.PUBSUB_EMULATOR_HOST;
      // Also disable auth for emulator to avoid metadata lookup warnings
      clientConfig.credentials = {
        client_email: "dummy@example.com",
        private_key: "dummy", // pragma: allowlist secret
      };
    }

    this.pubsub = new PubSub(clientConfig);

    this.subscriptionName =
      options.subscriptionName ||
      process.env.PUBSUB_SUBSCRIPTION ||
      "telemetry-sub";
    this.onMessage = options.onMessage ?? null;
    this.onError = options.onError ?? null;
  }

  /**
   * Start listening for messages
   */
  async start(): Promise<void> {
    if (this.isRunning) {
      logger.info("[PubSub] Client already running");
      return;
    }

    try {
      logger.info(
        `[PubSub] Connecting to subscription: ${this.subscriptionName}`,
      );

      this.subscription = this.pubsub.subscription(this.subscriptionName);
      this.isRunning = true;

      // Set up message handler
      this.subscription.on("message", this.handleMessage.bind(this));

      // Set up error handler
      this.subscription.on("error", this.handleError.bind(this));

      // Set up close handler
      this.subscription.on("close", () => {
        logger.info("[PubSub] Subscription closed");
        this.isRunning = false;
      });

      logger.info("[PubSub] Successfully connected");
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      logger.error(`[PubSub] Failed to connect: ${errorMessage}`);
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

    logger.info("[PubSub] Stopping client");
    this.subscription.removeAllListeners();
    await this.subscription.close();
    this.subscription = null;
    this.isRunning = false;
  }

  /**
   * Handle incoming Pub/Sub message
   */
  private handleMessage(message: Message): void {
    this.metrics.messagesReceived++;
    this.metrics.lastMessageTime = new Date();

    try {
      // Parse message data as JSON
      const rawData = JSON.parse(message.data.toString());

      // Validate and parse telemetry
      let telemetry: DroneTelemetry;
      try {
        telemetry = DroneTelemetry.fromJSON(rawData);
      } catch (err) {
        this.metrics.messagesFailed++;
        logger.error(
          `[PubSub] Validation failed: ${err instanceof Error ? err.message : String(err)}`,
        );

        // Still acknowledge to avoid redelivery of invalid messages
        message.ack();
        return;
      }

      this.metrics.messagesProcessed++;

      // Notify listener
      if (this.onMessage) {
        this.onMessage(telemetry);
      }

      // Acknowledge the message
      message.ack();
    } catch (error) {
      this.metrics.messagesFailed++;
      const errorMessage =
        error instanceof Error ? error.message : "Unknown error";
      logger.error(`[PubSub] Error processing message: ${errorMessage}`);

      // Negative acknowledge to retry later
      message.nack();
    }
  }

  /**
   * Handle subscription errors
   */
  private handleError(error: Error): void {
    logger.error(`[PubSub] Subscription error: ${error.message}`);

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

  /**
   * Get current metrics
   */
  getMetrics(): PubSubMetrics {
    return { ...this.metrics };
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
