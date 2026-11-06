package dev.bingoupgrader.server.bingo;
import dev.bingoupgrader.common.config.BingoConfig;
import dev.bingoupgrader.server.value.ValueConfigStore;
import java.io.IOException;
import java.nio.file.Files;
public final class BingoConfigStore {
    private BingoConfigStore() { }
    public static BingoConfig copy(BingoConfig cfg) {return ValueConfigStore.JSON.fromJson(ValueConfigStore.JSON.toJson(cfg),BingoConfig.class);}
    public static BingoConfig load() throws IOException {
        var file=ValueConfigStore.DIRECTORY.resolve("bingo.json");
        if(!Files.exists(file))save(new BingoConfig());
        if(Files.size(file)>1024*1024)throw new IOException("bingo.json exceeds 1 MiB");
        try(var reader=Files.newBufferedReader(file)) {
            return BingoConfig.fromJson(com.google.gson.JsonParser.parseReader(reader).getAsJsonObject());
        } catch(RuntimeException ex){throw new IOException("Invalid bingo.json",ex);}
    }
    public static void save(BingoConfig cfg) throws IOException {cfg.validate();cfg.difficulty=cfg.displayedDifficulty();ValueConfigStore.writeAtomic(ValueConfigStore.DIRECTORY.resolve("bingo.json"),ValueConfigStore.JSON.toJson(cfg));}
}
