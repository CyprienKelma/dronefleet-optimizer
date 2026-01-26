import type { DroneStatus } from "@shared/schemas";
import { type Component, Show } from "solid-js";

export const STATUS_COLORS: Record<DroneStatus, string> = {
  IDLE: "#4ecdc4",
  MOVING: "#45b7d1",
  DELIVERING: "#96ceb4",
  CHARGING: "#ffeaa7",
  MAINTENANCE: "#ff6b6b",
};

interface DronePopupProps {
  drone_id: string;
  status: DroneStatus;
  battery_percentage: number;
  speed_kmh: number;
  lat: number;
  lon: number;
  current_mission_id?: string | null;
}

const DronePopup: Component<DronePopupProps> = (props) => {
  const statusColor = () => STATUS_COLORS[props.status] || "#888";

  return (
    <div class="font-mono text-xs min-w-[200px] p-3 bg-[#1a1a1a] color-[#e0e0e0] border border-[#2a2a2a] rounded-lg">
      <div class="font-bold text-sm mb-2 text-[#4ecdc4]">{props.drone_id}</div>

      <div class="flex justify-between py-0.5">
        <span class="text-[#888]">Status</span>
        <span
          class="px-1.5 py-0.5 rounded text-[10px] font-bold"
          style={{
            background: `${statusColor()}20`,
            color: statusColor(),
          }}
        >
          {props.status}
        </span>
      </div>

      <div class="flex justify-between py-0.5">
        <span class="text-[#888]">Battery</span>
        <span class="text-[#e0e0e0]">
          {props.battery_percentage.toFixed(1)}%
        </span>
      </div>

      <div class="flex justify-between py-0.5">
        <span class="text-[#888]">Speed</span>
        <span class="text-[#e0e0e0]">{props.speed_kmh.toFixed(1)} km/h</span>
      </div>

      <div class="flex justify-between py-0.5">
        <span class="text-[#888]">Position</span>
        <span class="text-[#e0e0e0]">
          {props.lat.toFixed(4)}, {props.lon.toFixed(4)}
        </span>
      </div>

      <Show when={props.current_mission_id}>
        <div class="flex justify-between py-0.5">
          <span class="text-[#888]">Mission</span>
          <span class="text-[#e0e0e0]">{props.current_mission_id}</span>
        </div>
      </Show>
    </div>
  );
};

export default DronePopup;
