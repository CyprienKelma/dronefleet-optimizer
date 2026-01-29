import { map } from "nanostores";

export type UserConfig = {
  // Map Settings
  mapFitPadding: number;
  mapMaxZoom: number;
  mapFlyDuration: number;

  // Drone Marker Settings
  droneIconRadius: number;
  droneSelectedIconRadius: number;

  // Stream Settings
  reconnectMaxAttempts: number;
  reconnectBaseDelay: number; // ms
};

export const $userConfig = map<UserConfig>({
  mapFitPadding: 50,
  mapMaxZoom: 14,
  mapFlyDuration: 500,

  droneIconRadius: 8,
  droneSelectedIconRadius: 12,

  reconnectMaxAttempts: 5,
  reconnectBaseDelay: 1000,
});

export const updateUserConfig = (key: keyof UserConfig, value: number) => {
  $userConfig.setKey(key, value);
};
