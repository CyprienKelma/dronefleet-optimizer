import { z } from "zod";
import { DroneStatusSchema } from "./protocol";

/**
 * Telemetry data schemas
 * Mirrors: src/shared/schemas/telemetry.py
 */

export const GeoPointSchema = z.object({
  lat: z.number().min(-90).max(90),
  lon: z.number().min(-180).max(180),
});

export type GeoPoint = z.infer<typeof GeoPointSchema>;

export const DroneTelemetrySchema = z.object({
  drone_id: z.string(),
  timestamp: z.coerce.date(), // Accepts ISO string or Date
  position: GeoPointSchema,
  battery_percentage: z.number().min(0).max(100),
  speed_kmh: z.number(),
  status: DroneStatusSchema,
  current_mission_id: z.string().nullable().optional(),
});

export type DroneTelemetry = z.infer<typeof DroneTelemetrySchema>;

/**
 * Parse and validate telemetry from raw JSON (e.g., from Pub/Sub)
 */
export function parseTelemetry(data: unknown): DroneTelemetry {
  return DroneTelemetrySchema.parse(data);
}

/**
 * Safe parse that returns result object instead of throwing
 */
export function safeParseTelemetry(data: unknown) {
  return DroneTelemetrySchema.safeParse(data);
}
