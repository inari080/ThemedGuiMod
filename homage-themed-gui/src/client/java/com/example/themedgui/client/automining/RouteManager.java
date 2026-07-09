package com.example.themedgui.client.automining;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages mining routes - saves and loads coordinate lists.
 */
public class RouteManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROUTES_DIR = FabricLoader.getInstance().getConfigDir().resolve("themedgui").resolve("routes");

    private final Map<String, MiningRoute> routes = new HashMap<>();

    public RouteManager() {
        loadAllRoutes();
    }

    public void addRoute(MiningRoute route) {
        routes.put(route.getName(), route);
        saveRoute(route);
    }

    public MiningRoute getRoute(String name) {
        return routes.get(name);
    }

    public Map<String, MiningRoute> getAllRoutes() {
        return new HashMap<>(routes);
    }

    public void removeRoute(String name) {
        routes.remove(name);
        deleteRouteFile(name);
    }

    private void saveRoute(MiningRoute route) {
        try {
            Files.createDirectories(ROUTES_DIR);
            Path routeFile = ROUTES_DIR.resolve(route.getName() + ".json");
            String json = GSON.toJson(route.getPoints());
            Files.writeString(routeFile, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAllRoutes() {
        try {
            if (!Files.exists(ROUTES_DIR)) {
                Files.createDirectories(ROUTES_DIR);
                createSampleRoute();
                return;
            }

            Files.list(ROUTES_DIR)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = Files.readString(path);
                            String name = path.getFileName().toString().replace(".json", "");

                            Type listType = new TypeToken<java.util.List<MiningRoute.RoutePoint>>(){}.getType();
                            java.util.List<MiningRoute.RoutePoint> points = GSON.fromJson(json, listType);

                            MiningRoute route = new MiningRoute(name);
                            for (MiningRoute.RoutePoint point : points) {
                                route.addPoint(point.x, point.y, point.z, point.pitch, point.yaw, point.useAOTV);
                            }

                            routes.put(name, route);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            // If no routes exist, create sample
            if (routes.isEmpty()) {
                createSampleRoute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteRouteFile(String name) {
        try {
            Path routeFile = ROUTES_DIR.resolve(name + ".json");
            if (Files.exists(routeFile)) {
                Files.delete(routeFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSampleRoute() {
        // Create a sample mining route for testing
        MiningRoute sampleRoute = new MiningRoute("sample_route");

        // Add sample points (these are example coordinates)
        // Point 1: Use AOTV to teleport
        sampleRoute.addPoint(100, 70, 100, -30, 45, true);

        // Point 2: Normal movement
        sampleRoute.addPoint(105, 70, 105, -15, 50, false);

        // Point 3: Another AOTV point
        sampleRoute.addPoint(110, 72, 110, -25, 60, true);

        // Point 4: Return to start
        sampleRoute.addPoint(100, 70, 100, 0, 0, true);

        addRoute(sampleRoute);
    }

    public MiningRoute createRoute(String name) {
        MiningRoute route = new MiningRoute(name);
        addRoute(route);
        return route;
    }
}