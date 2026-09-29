package dev.technologia.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.io.IOException;
import org.slf4j.LoggerFactory;

/** Restart-required, server-side balance. Invalid files are preserved for repair. */
public record Balance(int generatorPerTick, int machineEnergyPerTick, int processingTicks,
                      int minerEnergyPerBlock, int minerRadius, int minerDepth, boolean minerEnabled) {
    public static Balance defaults() { return new Balance(40, 20, 100, 1000, 4, 32, true); }
    public Balance bounded() {
        return new Balance(clamp(generatorPerTick, 1, 10000), clamp(machineEnergyPerTick, 1, 10000),
                clamp(processingTicks, 1, 12000), clamp(minerEnergyPerBlock, 1, 1000000),
                clamp(minerRadius, 1, 16), clamp(minerDepth, 1, 128), minerEnabled);
    }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }
    public static Balance load(Path directory) {
        Path path = directory.resolve("technologia.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(directory);
            if (!Files.exists(path)) Files.writeString(path, gson.toJson(defaults()));
            var parsed = gson.fromJson(Files.readString(path), Balance.class);
            if (parsed == null) throw new IOException("Empty config");
            return parsed.bounded();
        } catch (Exception ex) {
            LoggerFactory.getLogger("Technologia").error("Cannot read {}; using defaults without replacing the file", path, ex);
            return defaults();
        }
    }
}
