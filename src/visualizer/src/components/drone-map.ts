import maplibregl from "maplibre-gl";
import type { DroneStatus, DroneTelemetry } from "@/schemas";
import { $drones, $selectedDroneId, selectDrone } from "@/stores";
import { getConfig } from "@/utils/config";

/**
 * DroneMap Web Component
 * Renders a MapLibre GL map with drone markers
 */

const STATUS_COLORS: Record<DroneStatus, string> = {
  IDLE: "#4ecdc4",
  MOVING: "#45b7d1",
  DELIVERING: "#96ceb4",
  CHARGING: "#ffeaa7",
  MAINTENANCE: "#ff6b6b",
};

export class DroneMap extends HTMLElement {
  private map: maplibregl.Map | null = null;
  private popup: maplibregl.Popup | null = null;
  private unsubscribeDrones: (() => void) | null = null;
  private unsubscribeSelected: (() => void) | null = null;

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
  }

  connectedCallback(): void {
    this.render();
    this.initMap();
    this.subscribeToStores();
  }

  disconnectedCallback(): void {
    this.cleanup();
  }

  private render(): void {
    if (!this.shadowRoot) return;

    this.shadowRoot.innerHTML = `
      <style>
        :host {
          display: block;
          width: 100%;
          height: 100%;
          position: relative;
        }

        #map {
          width: 100%;
          height: 100%;
        }

        .maplibregl-popup-content {
          background: #1a1a1a;
          color: #e0e0e0;
          border: 1px solid #2a2a2a;
          border-radius: 8px;
          padding: 12px;
          font-family: 'JetBrains Mono', monospace;
          font-size: 12px;
          min-width: 200px;
        }

        .maplibregl-popup-tip {
          border-top-color: #1a1a1a;
        }

        .drone-popup-title {
          font-weight: bold;
          font-size: 14px;
          margin-bottom: 8px;
          color: #4ecdc4;
        }

        .drone-popup-row {
          display: flex;
          justify-content: space-between;
          padding: 2px 0;
        }

        .drone-popup-label {
          color: #888;
        }

        .drone-popup-value {
          color: #e0e0e0;
        }

        .status-badge {
          padding: 2px 6px;
          border-radius: 4px;
          font-size: 10px;
          font-weight: bold;
        }
      </style>
      <div id="map"></div>
    `;
  }

  private initMap(): void {
    const config = getConfig();
    const mapContainer = this.shadowRoot?.getElementById("map");

    if (!mapContainer) return;

    this.map = new maplibregl.Map({
      container: mapContainer,
      style: config.mapStyleUrl,
      center: config.mapCenter,
      zoom: config.mapZoom,
    });

    this.map.addControl(new maplibregl.NavigationControl(), "top-right");
    this.map.addControl(new maplibregl.ScaleControl(), "bottom-left");

    this.popup = new maplibregl.Popup({
      closeButton: false,
      closeOnClick: false,
    });

    this.map.on("load", () => {
      this.setupDroneSource();
      this.setupDroneLayer();
      this.setupEventHandlers();
    });
  }

  private setupDroneSource(): void {
    if (!this.map) return;

    this.map.addSource("drones", {
      type: "geojson",
      data: this.createGeoJSON([]),
    });
  }

  private setupDroneLayer(): void {
    if (!this.map) return;

    // Circle layer for drone markers
    this.map.addLayer({
      id: "drone-markers",
      type: "circle",
      source: "drones",
      paint: {
        "circle-radius": [
          "case",
          ["boolean", ["feature-state", "selected"], false],
          12,
          8,
        ],
        "circle-color": ["get", "color"],
        "circle-stroke-width": 2,
        "circle-stroke-color": "#ffffff",
        "circle-opacity": 0.9,
      },
    });

    // Label layer for drone IDs
    this.map.addLayer({
      id: "drone-labels",
      type: "symbol",
      source: "drones",
      layout: {
        "text-field": ["get", "drone_id"],
        "text-size": 10,
        "text-offset": [0, 1.5],
        "text-anchor": "top",
      },
      paint: {
        "text-color": "#e0e0e0",
        "text-halo-color": "#1a1a1a",
        "text-halo-width": 1,
      },
    });
  }

  private setupEventHandlers(): void {
    if (!this.map) return;

    // Hover effect
    this.map.on("mouseenter", "drone-markers", (e) => {
      if (!this.map || !e.features?.length) return;

      this.map.getCanvas().style.cursor = "pointer";

      const feature = e.features[0];
      const coordinates = (
        feature.geometry as GeoJSON.Point
      ).coordinates.slice() as [number, number];
      const props = feature.properties;

      if (!props) return;

      this.showPopup(coordinates, props);
    });

    this.map.on("mouseleave", "drone-markers", () => {
      if (!this.map) return;
      this.map.getCanvas().style.cursor = "";
      this.popup?.remove();
    });

    // Click to select
    this.map.on("click", "drone-markers", (e) => {
      if (!e.features?.length) return;

      const droneId = e.features[0].properties?.drone_id;
      if (droneId) {
        selectDrone(droneId);
      }
    });

    // Click elsewhere to deselect
    this.map.on("click", (e) => {
      const features = this.map?.queryRenderedFeatures(e.point, {
        layers: ["drone-markers"],
      });

      if (!features?.length) {
        selectDrone(null);
      }
    });
  }

  private showPopup(
    coordinates: [number, number],
    props: Record<string, unknown>,
  ): void {
    if (!this.popup || !this.map) return;

    const status = props.status as DroneStatus;
    const statusColor = STATUS_COLORS[status] || "#888";

    const html = `
      <div class="drone-popup-title">${props.drone_id}</div>
      <div class="drone-popup-row">
        <span class="drone-popup-label">Status</span>
        <span class="status-badge" style="background: ${statusColor}20; color: ${statusColor};">
          ${status}
        </span>
      </div>
      <div class="drone-popup-row">
        <span class="drone-popup-label">Battery</span>
        <span class="drone-popup-value">${Number(props.battery_percentage).toFixed(1)}%</span>
      </div>
      <div class="drone-popup-row">
        <span class="drone-popup-label">Speed</span>
        <span class="drone-popup-value">${Number(props.speed_kmh).toFixed(1)} km/h</span>
      </div>
      <div class="drone-popup-row">
        <span class="drone-popup-label">Position</span>
        <span class="drone-popup-value">${Number(props.lat).toFixed(4)}, ${Number(props.lon).toFixed(4)}</span>
      </div>
      ${
        props.current_mission_id
          ? `
        <div class="drone-popup-row">
          <span class="drone-popup-label">Mission</span>
          <span class="drone-popup-value">${props.current_mission_id}</span>
        </div>
      `
          : ""
      }
    `;

    this.popup.setLngLat(coordinates).setHTML(html).addTo(this.map);
  }

  private subscribeToStores(): void {
    // Subscribe to drone updates
    this.unsubscribeDrones = $drones.subscribe((drones) => {
      this.updateDroneMarkers(Object.values(drones));
    });

    // Subscribe to selection changes
    this.unsubscribeSelected = $selectedDroneId.subscribe((droneId) => {
      this.updateSelectedState(droneId);
    });
  }

  private updateDroneMarkers(drones: DroneTelemetry[]): void {
    if (!this.map) return;

    const source = this.map.getSource("drones") as
      | maplibregl.GeoJSONSource
      | undefined;
    if (source) {
      source.setData(this.createGeoJSON(drones));
    }
  }

  private updateSelectedState(selectedId: string | null): void {
    if (!this.map) return;

    const drones = $drones.get();

    // Clear all selected states
    for (const droneId of Object.keys(drones)) {
      this.map.setFeatureState(
        { source: "drones", id: droneId },
        { selected: false },
      );
    }

    // Set selected state for the selected drone
    if (selectedId) {
      this.map.setFeatureState(
        { source: "drones", id: selectedId },
        { selected: true },
      );

      // Optionally fly to the selected drone
      const drone = drones[selectedId];
      if (drone) {
        this.map.flyTo({
          center: [drone.position.lon, drone.position.lat],
          zoom: Math.max(this.map.getZoom(), 14),
          duration: 500,
        });
      }
    }
  }

  private createGeoJSON(drones: DroneTelemetry[]): GeoJSON.FeatureCollection {
    return {
      type: "FeatureCollection",
      features: drones.map((drone) => ({
        type: "Feature" as const,
        id: drone.drone_id,
        geometry: {
          type: "Point" as const,
          coordinates: [drone.position.lon, drone.position.lat],
        },
        properties: {
          drone_id: drone.drone_id,
          status: drone.status,
          battery_percentage: drone.battery_percentage,
          speed_kmh: drone.speed_kmh,
          current_mission_id: drone.current_mission_id,
          lat: drone.position.lat,
          lon: drone.position.lon,
          color: STATUS_COLORS[drone.status] || "#888",
          timestamp: drone.timestamp.toISOString(),
        },
      })),
    };
  }

  private cleanup(): void {
    this.unsubscribeDrones?.();
    this.unsubscribeSelected?.();
    this.map?.remove();
    this.map = null;
    this.popup = null;
  }
}

// Register the custom element
customElements.define("drone-map", DroneMap);
