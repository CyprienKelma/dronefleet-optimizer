import { z } from "zod";
import { DroneStatusSchema } from "./protocol";

/**
 * Drone entity schemas
 * Mirrors: src/shared/schemas/drones.py
 */

export const DroneModelSchema = z.enum([
  "LIGHT_DELIVERY", // Quadcopter, small payload (<2kg), agile
  "HEAVY_LIFT", // Hexacopter, heavy payload (up to 10kg)
  "LONG_RANGE", // Hybrid/VTOL, long distance, medium payload
]);

export type DroneModel = z.infer<typeof DroneModelSchema>;

export const DroneSchema = z.object({
  id: z.string().describe("Unique drone serial number"),
  model: DroneModelSchema,

  // Physical Capabilities
  max_payload_kg: z.number().positive(),
  max_range_km: z.number().positive(),
  cruise_speed_kmh: z.number().positive(),

  // Battery specs
  battery_capacity_mah: z.number().int().positive(),
  current_battery_cycles: z.number().int().default(0),

  // Operational constraints
  requires_maintenance: z.boolean().default(false),
  default_status: DroneStatusSchema.default("IDLE"),
});

export type Drone = z.infer<typeof DroneSchema>;

/**
 * Parse and validate drone data
 */
export function parseDrone(data: unknown): Drone {
  return DroneSchema.parse(data);
}

/**
 * Safe parse that returns result object instead of throwing
 */
export function safeParseDrone(data: unknown) {
  return DroneSchema.safeParse(data);
}
