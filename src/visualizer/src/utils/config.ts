/**
 * Runtime configuration system
 * Loads config from environment variables and URL params
 */

export interface AppConfig {
  // GCP Pub/Sub
  pubsubProjectId: string;
  pubsubSubscription: string;

  // Auth
  adminToken: string;

  // Map
  mapStyleUrl: string;
  mapCenter: [number, number]; // [lng, lat]
  mapZoom: number;

  // Debug
  debugMode: boolean;
  maxEventLogSize: number;
}

const DEFAULT_CONFIG: AppConfig = {
  pubsubProjectId: "drone-fleet-optimizer-local",
  pubsubSubscription: "telemetry",
  adminToken: "",
  mapStyleUrl: "https://tiles.openfreemap.org/styles/liberty",
  mapCenter: [3.057, 50.629], // Lille, France
  mapZoom: 12,
  debugMode: true,
  maxEventLogSize: 100,
};

/**
 * Get configuration value from multiple sources:
 * 1. URL search params (highest priority)
 * 2. Environment variables (VITE_ prefix for build-time)
 * 3. Runtime environment (loaded from window.__CONFIG__)
 * 4. Default values
 */
function getConfigValue<T>(
  key: string,
  defaultValue: T,
  transform?: (value: string) => T,
): T {
  // Check URL params first
  if (typeof window !== "undefined") {
    const urlParams = new URLSearchParams(window.location.search);
    const urlValue = urlParams.get(key);
    if (urlValue !== null) {
      return transform ? transform(urlValue) : (urlValue as unknown as T);
    }
  }

  // Check Vite env vars (build-time)
  const viteKey = `VITE_${key.toUpperCase()}`;
  const viteValue = import.meta.env[viteKey];
  if (viteValue !== undefined) {
    return transform ? transform(viteValue) : (viteValue as unknown as T);
  }

  // Check runtime config (injected by server or loaded at startup)
  if (
    typeof window !== "undefined" &&
    (window as unknown as { __CONFIG__?: Record<string, unknown> }).__CONFIG__
  ) {
    const runtimeConfig = (
      window as unknown as { __CONFIG__: Record<string, unknown> }
    ).__CONFIG__;
    if (key in runtimeConfig) {
      const value = runtimeConfig[key];
      return transform && typeof value === "string"
        ? transform(value)
        : (value as T);
    }
  }

  return defaultValue;
}

/**
 * Load and return the application configuration
 */
export function loadConfig(): AppConfig {
  return {
    pubsubProjectId: getConfigValue(
      "pubsub_project_id",
      DEFAULT_CONFIG.pubsubProjectId,
    ),
    pubsubSubscription: getConfigValue(
      "pubsub_subscription",
      DEFAULT_CONFIG.pubsubSubscription,
    ),
    adminToken: getConfigValue("admin_token", DEFAULT_CONFIG.adminToken),
    mapStyleUrl: getConfigValue("map_style_url", DEFAULT_CONFIG.mapStyleUrl),
    mapCenter: getConfigValue("map_center", DEFAULT_CONFIG.mapCenter, (v) => {
      const [lng, lat] = v.split(",").map(Number);
      return [lng, lat] as [number, number];
    }),
    mapZoom: getConfigValue("map_zoom", DEFAULT_CONFIG.mapZoom, Number),
    debugMode: getConfigValue("debug", DEFAULT_CONFIG.debugMode, (v) =>
      ["true", "1", "yes"].includes(v.toLowerCase()),
    ),
    maxEventLogSize: getConfigValue(
      "max_event_log",
      DEFAULT_CONFIG.maxEventLogSize,
      Number,
    ),
  };
}

// Singleton config instance
let _config: AppConfig | null = null;

export function getConfig(): AppConfig {
  if (!_config) {
    _config = loadConfig();
  }
  return _config;
}

export function resetConfig(): void {
  _config = null;
}
