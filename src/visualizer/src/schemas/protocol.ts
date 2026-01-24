import { z } from "zod";

/**
 * Define authorised terms to define things and states
 * Mirrors: src/shared/schemas/protocol.py
 */

export const DroneStatusSchema = z.enum([
  "IDLE", // Waiting at base
  "MOVING", // Flying towards a target
  "DELIVERING", // Dropping/loading
  "CHARGING", // Charging
  "MAINTENANCE", // Out of service
]);

export type DroneStatus = z.infer<typeof DroneStatusSchema>;

export const UrgencyLevelSchema = z.enum([
  "STANDARD",
  "HIGH", // Blood, Organs -> Absolute priority
  "CRITICAL", // Vital prognosis (Takes precedence over everything)
]);

export type UrgencyLevel = z.infer<typeof UrgencyLevelSchema>;

export const ActionTypeSchema = z.enum([
  "FLY_TO", // Move
  "PICKUP", // Load a package
  "DROPOFF", // Deliver a package
  "CHARGE", // Recharge
]);

export type ActionType = z.infer<typeof ActionTypeSchema>;
