package dev.technologia.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.*;
import java.io.IOException;
import org.slf4j.LoggerFactory;

/** Restart-required, server-side balance. Invalid files are preserved for repair. */
public record Balance(int generatorPerTick, int machineEnergyPerTick, int processingTicks,
                      int minerEnergyPerBlock, int minerRadius, int minerDepth, boolean minerEnabled, boolean machineSounds) {
    public static Balance defaults() { return new Balance(40, 20, 100, 1000, 4, 32, true, true); }
    public Balance bounded() {
        return new Balance(clamp(generatorPerTick, 1, 10000), clamp(machineEnergyPerTick, 1, 10000),
                clamp(processingTicks, 1, 12000), clamp(minerEnergyPerBlock, 1, 100000),
                clamp(minerRadius, 1, 16), clamp(minerDepth, 1, 128), minerEnabled, machineSounds);
    }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }

    /** A key missing from an older or partial file keeps its default instead of becoming zero. */
    static Balance parse(String json) throws IOException {
        var parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IOException("Config must be a JSON object");
        JsonObject values = parsed.getAsJsonObject();
        Balance base = defaults();
        return new Balance(number(values, "generatorPerTick", base.generatorPerTick),
                number(values, "machineEnergyPerTick", base.machineEnergyPerTick),
                number(values, "processingTicks", base.processingTicks),
                number(values, "minerEnergyPerBlock", base.minerEnergyPerBlock),
                number(values, "minerRadius", base.minerRadius), number(values, "minerDepth", base.minerDepth),
                flag(values, "minerEnabled", base.minerEnabled), flag(values, "machineSounds", base.machineSounds)).bounded();
    }
    private static int number(JsonObject values, String key, int fallback) {
        return values.has(key) && values.get(key).isJsonPrimitive() ? values.get(key).getAsInt() : fallback;
    }
    private static boolean flag(JsonObject values, String key, boolean fallback) {
        return values.has(key) && values.get(key).isJsonPrimitive() ? values.get(key).getAsBoolean() : fallback;
    }

    public static Balance load(Path directory) {
        Path path = directory.resolve("technologia.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(directory);
            if (!Files.exists(path)) Files.writeString(path, gson.toJson(defaults()));
            return parse(Files.readString(path));
        } catch (Exception ex) {
            LoggerFactory.getLogger("Technologia").error("Cannot read {}; using defaults without replacing the file", path, ex);
            return defaults();
        }
    }
}
