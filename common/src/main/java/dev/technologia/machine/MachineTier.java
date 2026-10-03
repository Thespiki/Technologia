package dev.technologia.machine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tier statistics are data: {@code /technologia/tiers.json} in the mod jar. The same file drives
 * the registered tier kits and the generated resources, so adding a tier is a data change.
 */
public record MachineTier(int index, String id, String name, int lanes, double speed, double capacity,
                          double efficiency, double generation, double transfer) {
    public static final int MAX_LANES = 8;
    private static final List<MachineTier> TIERS = load();

    public static List<MachineTier> all() { return TIERS; }
    public static int count() { return TIERS.size(); }
    /** Out-of-range values clamp, so data from a jar with more tiers still loads. */
    public static MachineTier get(int index) { return TIERS.get(Math.clamp(index, 0, TIERS.size() - 1)); }
    public boolean isBase() { return index == 0; }

    private static List<MachineTier> load() {
        List<MachineTier> result = new ArrayList<>();
        try (var stream = MachineTier.class.getResourceAsStream("/technologia/tiers.json")) {
            if (stream == null) throw new IllegalStateException("Missing /technologia/tiers.json");
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var element : root.getAsJsonArray("tiers")) {
                JsonObject tier = element.getAsJsonObject();
                result.add(new MachineTier(result.size(), tier.get("id").getAsString(), tier.get("name").getAsString(),
                        Math.clamp(tier.get("lanes").getAsInt(), 1, MAX_LANES),
                        bounded(tier, "speed", 0.1, 64), bounded(tier, "capacity", 0.1, 400),
                        bounded(tier, "efficiency", 0.05, 4), bounded(tier, "generation", 0.1, 256),
                        bounded(tier, "transfer", 0.1, 4096)));
            }
        } catch (Exception ex) {
            // Registered kit items depend on this file; a silent fallback would desync client and server.
            throw new IllegalStateException("Technologia cannot read /technologia/tiers.json", ex);
        }
        if (result.isEmpty()) throw new IllegalStateException("Technologia tiers.json defines no tier");
        return List.copyOf(result);
    }

    private static double bounded(JsonObject tier, String key, double min, double max) {
        return Math.max(min, Math.min(max, tier.get(key).getAsDouble()));
    }
}
