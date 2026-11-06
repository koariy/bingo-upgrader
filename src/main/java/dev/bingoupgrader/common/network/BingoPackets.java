package dev.bingoupgrader.common.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Full personalized view, control requests and small notification events. */
public final class BingoPackets {
    private BingoPackets() { }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String path){return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bingo_upgrader",path));}
    public record Snapshot(String json,String screen) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=id("bingo_snapshot");
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,v)->{b.writeUtf(v.json,262144);b.writeUtf(v.screen,16);},b->new Snapshot(b.readUtf(262144),b.readUtf(16)));
        public Type<Snapshot> type(){return TYPE;}
    }
    public record Control(String matchId,long controlRevision,String action,String field,String value) implements CustomPacketPayload {
        public static final Type<Control> TYPE=id("bingo_control");
        public static final StreamCodec<RegistryFriendlyByteBuf,Control> CODEC=StreamCodec.of((b,v)->{b.writeUtf(v.matchId,36);b.writeLong(v.controlRevision);b.writeUtf(v.action,16);b.writeUtf(v.field,32);b.writeUtf(v.value,64);},b->new Control(b.readUtf(36),b.readLong(),b.readUtf(16),b.readUtf(32),b.readUtf(64)));
        public Type<Control> type(){return TYPE;}
    }
    public record Notice(String kind,String player,int count) implements CustomPacketPayload {
        public static final Type<Notice> TYPE=id("bingo_notice");
        public static final StreamCodec<RegistryFriendlyByteBuf,Notice> CODEC=StreamCodec.of((b,v)->{b.writeUtf(v.kind,16);b.writeUtf(v.player,64);b.writeVarInt(v.count);},b->new Notice(b.readUtf(16),b.readUtf(64),b.readVarInt()));
        public Type<Notice> type(){return TYPE;}
    }
}
