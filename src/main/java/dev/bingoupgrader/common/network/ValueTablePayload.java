package dev.bingoupgrader.common.network;

import dev.bingoupgrader.common.value.ValueTable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.*;

/** Bounded full snapshot; Fabric large-payload transport handles tables from modpacks. */
public record ValueTablePayload(ValueTable table) implements CustomPacketPayload {
    public static final Type<ValueTablePayload> TYPE = new Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("bingo_upgrader", "value_table"));
    public static final int MAX_ENTRIES = 100_000;
    public static final StreamCodec<RegistryFriendlyByteBuf, ValueTablePayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            var t = payload.table();
            if (t.entries().size() > MAX_ENTRIES) throw new IllegalArgumentException("Value table too large");
            buf.writeLong(t.revision());
            var m = t.modifiers();
            buf.writeDouble(m.enchantmentPerLevel()); buf.writeDouble(m.potionPerMinute());
            buf.writeDouble(m.instantEffectValue()); buf.writeDouble(m.minimumDurabilityFactor());
            buf.writeVarInt(t.entries().size());
            t.entries().forEach((id,e) -> {
                buf.writeUtf(id, 256); buf.writeDouble(e.value()); buf.writeUtf(e.source(), 32); buf.writeBoolean(e.blocked());
            });
        }, buf -> {
            long revision = buf.readLong();
            var m = new ValueTable.Modifiers(buf.readDouble(),buf.readDouble(),buf.readDouble(),buf.readDouble());
            int count = buf.readVarInt();
            if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid table size");
            Map<String, ValueTable.Entry> entries = new HashMap<>();
            for (int i=0;i<count;i++) {
                String id = buf.readUtf(256);
                if (net.minecraft.resources.Identifier.tryParse(id) == null) throw new IllegalArgumentException("Invalid ID");
                var e = new ValueTable.Entry(buf.readDouble(),buf.readUtf(32),buf.readBoolean());
                if (entries.put(id,e) != null) throw new IllegalArgumentException("Duplicate ID");
            }
            return new ValueTablePayload(new ValueTable(revision, entries, m));
        });
    @Override public Type<ValueTablePayload> type() { return TYPE; }
}
