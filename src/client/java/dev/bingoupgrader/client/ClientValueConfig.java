package dev.bingoupgrader.client;

import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import dev.bingoupgrader.common.BingoUpgraderMod;
import java.nio.file.*;

/** Local preference only; never synchronized to or trusted by a server. */
public final class ClientValueConfig {
    public boolean showItemValues = true;
    public static ClientValueConfig load() {
        var json = new GsonBuilder().setPrettyPrinting().create();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("bingoupgrader/client.json");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.writeString(file, json.toJson(new ClientValueConfig()));
            try (var reader = Files.newBufferedReader(file)) {
                var config = json.fromJson(reader, ClientValueConfig.class);
                if (config == null) throw new IllegalArgumentException("Empty client config");
                return config;
            }
        } catch (Exception e) {
            BingoUpgraderMod.LOGGER.warn("Cannot read client value preferences; using defaults",e);
            return new ClientValueConfig();
        }
    }
}
