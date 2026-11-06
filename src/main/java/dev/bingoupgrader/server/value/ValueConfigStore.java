package dev.bingoupgrader.server.value;

import com.google.gson.*;
import dev.bingoupgrader.common.config.ValueConfig;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;

public final class ValueConfigStore {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("bingoupgrader");
    private ValueConfigStore() { }
    public static ValueConfig load() throws IOException {
        Path path = DIRECTORY.resolve("values.json");
        if (!Files.exists(path)) writeAtomic(path, JSON.toJson(new ValueConfig()));
        if (Files.size(path) > 4 * 1024 * 1024) throw new IOException("values.json exceeds 4 MiB");
        try (var reader = Files.newBufferedReader(path)) {
            ValueConfig config = JSON.fromJson(reader, ValueConfig.class);
            if (config == null) throw new IllegalArgumentException("Empty config");
            config.validate(); return config;
        } catch (RuntimeException ex) { throw new IOException("Invalid values.json: " + ex.getMessage(), ex); }
    }
    public static void writeAtomic(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Path tmp = Files.createTempFile(path.getParent(), "values-", ".tmp");
        try {
            Files.writeString(tmp, content);
            try { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
}
