package dev.bingoupgrader.server.upgrader;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** World-scoped limits/pity survive reconnect and normal server save/restart. */
public final class UpgradeHistory extends SavedData {
    public record Stats(List<Long> attempts, int failures) {
        static final Codec<Stats> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.listOf().fieldOf("attempts").forGetter(Stats::attempts),
            Codec.intRange(0,1000000).fieldOf("failures").forGetter(Stats::failures)).apply(i,Stats::new));
    }
    private final Map<String,Stats> players;
    private static final Codec<UpgradeHistory> CODEC = Codec.unboundedMap(Codec.STRING,Stats.CODEC)
        .xmap(UpgradeHistory::new,h -> new HashMap<>(h.players));
    public static final SavedDataType<UpgradeHistory> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("bingo_upgrader","upgrade_history"),UpgradeHistory::new,CODEC,null);
    private UpgradeHistory() { players = new HashMap<>(); }
    private UpgradeHistory(Map<String,Stats> map) { players = new HashMap<>(map); }
    public Stats stats(UUID id, long now) {
        var old = players.getOrDefault(id.toString(),new Stats(List.of(),0));
        // A backwards clock keeps recent/future entries, rather than allowing a reset bypass.
        return new Stats(old.attempts().stream().filter(t -> now-t < 60000).toList(),old.failures());
    }
    public void record(UUID id, long now, boolean won) {
        var old = stats(id,now); var attempts = new ArrayList<>(old.attempts()); attempts.add(now);
        players.put(id.toString(),new Stats(List.copyOf(attempts),won ? 0 : Math.min(1000000,old.failures()+1))); setDirty();
    }
}
