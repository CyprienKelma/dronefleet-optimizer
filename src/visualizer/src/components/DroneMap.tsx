import { useStore } from "@nanostores/solid";
import maplibregl from "maplibre-gl";
import { type Component, createEffect, onCleanup, onMount } from "solid-js";
import { render } from "solid-js/web";
import type { DroneTelemetry } from "@/schemas";
import { $drones, $selectedDroneId, selectDrone } from "@/stores";
import { getConfig } from "@/utils/config";
import DronePopup, { STATUS_COLORS } from "./DronePopup";

/**
 * Converts drone list to GeoJSON for MapLibre
 */
const createGeoJSON = (
  droneList: DroneTelemetry[],
): GeoJSON.FeatureCollection => {
  return {
    type: "FeatureCollection",
    features: droneList.map((drone) => ({
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
        timestamp:
          drone.timestamp instanceof Date
            ? drone.timestamp.toISOString()
            : drone.timestamp,
      },
    })),
  };
};

const DroneMap: Component = () => {
  let mapContainer!: HTMLDivElement;
  let map: maplibregl.Map | null = null;
  let popup: maplibregl.Popup | null = null;
  let popupCleanup: (() => void) | null = null;

  const drones = useStore($drones);
  const selectedDroneId = useStore($selectedDroneId);

  const showPopup = (
    coordinates: [number, number],
    props: Record<string, string | number | boolean | null | undefined>,
  ) => {
    if (!popup || !map) return;

    // Clean up previous popup render
    popupCleanup?.();

    const container = document.createElement("div");
    popupCleanup = render(
      () => (
        <DronePopup
          drone_id={props.drone_id}
          status={props.status}
          battery_percentage={props.battery_percentage}
          speed_kmh={props.speed_kmh}
          lat={props.lat}
          lon={props.lon}
          current_mission_id={props.current_mission_id}
        />
      ),
      container,
    );

    popup.setLngLat(coordinates).setDOMContent(container).addTo(map);
  };

  onMount(() => {
    const config = getConfig();

    map = new maplibregl.Map({
      container: mapContainer,
      style: config.mapStyleUrl,
      center: config.mapCenter,
      zoom: config.mapZoom,
    });

    map.addControl(new maplibregl.NavigationControl(), "top-right");
    map.addControl(new maplibregl.ScaleControl(), "bottom-left");

    popup = new maplibregl.Popup({
      closeButton: false,
      closeOnClick: false,
    });

    map.on("load", () => {
      if (!map) return;

      map.addSource("drones", {
        type: "geojson",
        data: createGeoJSON(Object.values(drones())),
      });

      map.addLayer({
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

      map.addLayer({
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

      map.on("mouseenter", "drone-markers", (e) => {
        if (!map || !e.features?.length) return;
        map.getCanvas().style.cursor = "pointer";

        const feature = e.features[0];
        const coordinates = (
          feature.geometry as GeoJSON.Point
        ).coordinates.slice() as [number, number];
        const props = feature.properties;
        if (!props) return;

        showPopup(coordinates, props);
      });

      map.on("mouseleave", "drone-markers", () => {
        if (!map) return;
        map.getCanvas().style.cursor = "";
        popup?.remove();
        popupCleanup?.();
        popupCleanup = null;
      });

      map.on("click", "drone-markers", (e) => {
        if (!e.features?.length) return;
        const droneId = e.features[0].properties?.drone_id;
        if (droneId) {
          selectDrone(droneId);
        }
      });

      map.on("click", (e) => {
        const features = map?.queryRenderedFeatures(e.point, {
          layers: ["drone-markers"],
        });
        if (!features?.length) {
          selectDrone(null);
        }
      });
    });

    onCleanup(() => {
      popupCleanup?.();
      map?.remove();
    });
  });

  // React to drone changes
  createEffect(() => {
    const droneList = Object.values(drones());
    if (map?.isStyleLoaded()) {
      const source = map.getSource("drones") as maplibregl.GeoJSONSource;
      if (source) {
        source.setData(createGeoJSON(droneList));
      }
    }
  });

  // React to selection changes
  createEffect(() => {
    const selectedId = selectedDroneId();
    const droneList = drones();

    if (map?.isStyleLoaded()) {
      // Clear all selected states
      for (const id of Object.keys(droneList)) {
        map?.setFeatureState({ source: "drones", id }, { selected: false });
      }

      if (selectedId) {
        map.setFeatureState(
          { source: "drones", id: selectedId },
          { selected: true },
        );

        const drone = droneList[selectedId];
        if (drone) {
          map.flyTo({
            center: [drone.position.lon, drone.position.lat],
            zoom: Math.max(map.getZoom(), 14),
            duration: 500,
          });
        }
      }
    }
  });

  return <div ref={mapContainer} class="w-full h-full" />;
};

export default DroneMap;
