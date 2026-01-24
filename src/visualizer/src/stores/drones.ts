import { atom, map } from "nanostores";
import type { DroneTelemetry } from "@/schemas";

/**
 * Drone telemetry store using Nano Stores
 * Manages the state of all drones with their latest telemetry data
 */

// Map of drone_id -> latest telemetry
export const $drones = map<Record<string, DroneTelemetry>>({});

// Currently selected/hovered drone
export const $selectedDroneId = atom<string | null>(null);

// Last update timestamp
export const $lastUpdateTime = atom<Date | null>(null);

/**
 * Update telemetry for a single drone
 */
export function updateDroneTelemetry(telemetry: DroneTelemetry): void {
  $drones.setKey(telemetry.drone_id, telemetry);
  $lastUpdateTime.set(new Date());
}

/**
 * Update telemetry for multiple drones (batch update)
 */
export function updateDronesTelemetry(telemetries: DroneTelemetry[]): void {
  const current = $drones.get();
  const updates: Record<string, DroneTelemetry> = { ...current };

  for (const telemetry of telemetries) {
    updates[telemetry.drone_id] = telemetry;
  }

  $drones.set(updates);
  $lastUpdateTime.set(new Date());
}

/**
 * Remove a drone from the store
 */
export function removeDrone(droneId: string): void {
  const current = $drones.get();
  const { [droneId]: _, ...rest } = current;
  $drones.set(rest);
}

/**
 * Clear all drones
 */
export function clearDrones(): void {
  $drones.set({});
  $lastUpdateTime.set(null);
}

/**
 * Get all drone IDs
 */
export function getDroneIds(): string[] {
  return Object.keys($drones.get());
}

/**
 * Get drone count
 */
export function getDroneCount(): number {
  return Object.keys($drones.get()).length;
}

/**
 * Select a drone by ID
 */
export function selectDrone(droneId: string | null): void {
  $selectedDroneId.set(droneId);
}

/**
 * Get a specific drone's telemetry
 */
export function getDroneTelemetry(droneId: string): DroneTelemetry | undefined {
  return $drones.get()[droneId];
}
