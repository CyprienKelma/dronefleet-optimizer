import { useStore } from "@nanostores/solid";
import { type Component, createSignal, Show } from "solid-js";
import { $userConfig, type UserConfig, updateUserConfig } from "../stores";

const ConfigInput: Component<{
  label: string;
  key: keyof UserConfig;
  min: number;
  max: number;
  step?: number;
}> = (props) => {
  const config = useStore($userConfig);
  const id = `config-${props.key}`;

  return (
    <div class="mb-3">
      <div class="flex justify-between mb-1">
        <label for={id} class="text-xs text-gray-400">
          {props.label}
        </label>
        <span class="text-xs text-[#4ecdc4]">{config()[props.key]}</span>
      </div>
      <input
        id={id}
        type="range"
        min={props.min}
        max={props.max}
        step={props.step || 1}
        value={config()[props.key]}
        onInput={(e) =>
          updateUserConfig(props.key, Number.parseFloat(e.currentTarget.value))
        }
        class="w-full h-1 bg-gray-700 rounded-lg appearance-none cursor-pointer accent-[#4ecdc4]"
      />
    </div>
  );
};

const ConfigPanel: Component = () => {
  const [isOpen, setIsOpen] = createSignal(false);

  return (
    <div class="absolute top-4 left-4 z-10">
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen())}
        class="bg-[#1a1a1a] hover:bg-[#2a2a2a] text-[#e0e0e0] border border-[#2a2a2a] rounded px-3 py-2 text-sm shadow-lg transition-colors flex items-center gap-2"
      >
        <span>⚙️ Settings</span>
      </button>

      <Show when={isOpen()}>
        <div class="mt-2 w-64 bg-[#1a1a1a]/95 backdrop-blur border border-[#2a2a2a] rounded-lg shadow-xl p-4 text-[#e0e0e0]">
          <h3 class="text-sm font-bold mb-4 border-b border-[#2a2a2a] pb-2 text-[#4ecdc4]">
            Map Configuration
          </h3>
          <ConfigInput
            label="Map Padding"
            key="mapFitPadding"
            min={0}
            max={200}
            step={10}
          />
          <ConfigInput label="Max Zoom" key="mapMaxZoom" min={10} max={20} />
          <ConfigInput
            label="Fly Duration (ms)"
            key="mapFlyDuration"
            min={0}
            max={2000}
            step={100}
          />

          <h3 class="text-sm font-bold mb-4 mt-4 border-b border-[#2a2a2a] pb-2 text-[#4ecdc4]">
            Visuals
          </h3>
          <ConfigInput
            label="Drone Radius"
            key="droneIconRadius"
            min={2}
            max={20}
          />
          <ConfigInput
            label="Selected Radius"
            key="droneSelectedIconRadius"
            min={5}
            max={30}
          />

          <h3 class="text-sm font-bold mb-4 mt-4 border-b border-[#2a2a2a] pb-2 text-[#4ecdc4]">
            Connection
          </h3>
          <ConfigInput
            label="Max Retries"
            key="reconnectMaxAttempts"
            min={1}
            max={20}
          />
          <ConfigInput
            label="Retry Delay (ms)"
            key="reconnectBaseDelay"
            min={100}
            max={5000}
            step={100}
          />
        </div>
      </Show>
    </div>
  );
};

export default ConfigPanel;
