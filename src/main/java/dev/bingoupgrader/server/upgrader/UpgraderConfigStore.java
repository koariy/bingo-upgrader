package dev.bingoupgrader.server.upgrader;
import dev.bingoupgrader.common.config.UpgraderConfig;
import dev.bingoupgrader.server.value.ValueConfigStore;
import java.nio.file.Files;
import java.io.IOException;
public final class UpgraderConfigStore {
    public static UpgraderConfig load() throws IOException {
        var path = ValueConfigStore.DIRECTORY.resolve("upgrader.json");
        if (!Files.exists(path)) ValueConfigStore.writeAtomic(path,ValueConfigStore.JSON.toJson(new UpgraderConfig()));
        if (Files.size(path) > 1024*1024) throw new IOException("upgrader.json exceeds 1 MiB");
        try (var reader = Files.newBufferedReader(path)) {
            var cfg = ValueConfigStore.JSON.fromJson(reader,UpgraderConfig.class);
            if (cfg == null) throw new IllegalArgumentException("Empty config"); cfg.validate(); return cfg;
        } catch (RuntimeException e) { throw new IOException("Invalid upgrader.json",e); }
    }
}
