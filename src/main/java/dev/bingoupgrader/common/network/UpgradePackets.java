package dev.bingoupgrader.common.network;
import dev.bingoupgrader.common.upgrader.Offer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class UpgradePackets {
    private UpgradePackets() { }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("bingo_upgrader",name));
    }
    public record OpenRequest() implements CustomPacketPayload {
        public static final Type<OpenRequest> TYPE=UpgradePackets.type("open_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,OpenRequest> CODEC=StreamCodec.of((b,v)->{  },b->new OpenRequest());
        @Override public Type<OpenRequest> type() { return TYPE; }
    }
    public record PreviewRequest(UUID session, List<Offer> offers, String target, int count, String mode, long requestId) implements CustomPacketPayload {
        public PreviewRequest(UUID session,List<Offer> offers,String target,int count,String mode){this(session,offers,target,count,mode,0);}
        public static final Type<PreviewRequest> TYPE=UpgradePackets.type("preview_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,PreviewRequest> CODEC=StreamCodec.of((b,v)->{ b.writeUUID(v.session); if(v.offers.size()>36) throw new IllegalArgumentException("offers"); b.writeVarInt(v.offers.size()); for(var o:v.offers){ b.writeVarInt(o.slot()); b.writeVarInt(o.count()); } b.writeUtf(v.target,256); b.writeVarInt(v.count); b.writeUtf(v.mode,8); b.writeLong(v.requestId); },b->readPreview(b));
        @Override public Type<PreviewRequest> type() { return TYPE; }
    }
    public record StartRequest(UUID session, UUID token) implements CustomPacketPayload {
        public static final Type<StartRequest> TYPE=UpgradePackets.type("start_request");
        public static final StreamCodec<RegistryFriendlyByteBuf,StartRequest> CODEC=StreamCodec.of((b,v)->{ b.writeUUID(v.session); b.writeUUID(v.token); },b->new StartRequest(b.readUUID(),b.readUUID()));
        @Override public Type<StartRequest> type() { return TYPE; }
    }
    public record Opened(UUID session, long revision) implements CustomPacketPayload {
        public static final Type<Opened> TYPE=UpgradePackets.type("opened");
        public static final StreamCodec<RegistryFriendlyByteBuf,Opened> CODEC=StreamCodec.of((b,v)->{ b.writeUUID(v.session); b.writeLong(v.revision); },b->new Opened(b.readUUID(),b.readLong()));
        @Override public Type<Opened> type() { return TYPE; }
    }
    public record Quoted(UUID session, UUID token, String target, int count, double input, double output, double chance, boolean low, long requestId) implements CustomPacketPayload {
        public Quoted { if(count<1||count>99||!Double.isFinite(input)||!Double.isFinite(output)||input<=0||output<=input||!Double.isFinite(chance)||chance<=0||chance>=1)throw new IllegalArgumentException("Invalid quote"); }
        public static final Type<Quoted> TYPE=UpgradePackets.type("quoted");
        public static final StreamCodec<RegistryFriendlyByteBuf,Quoted> CODEC=StreamCodec.of((b,v)->{ b.writeUUID(v.session); b.writeUUID(v.token); b.writeUtf(v.target,256); b.writeVarInt(v.count); b.writeDouble(v.input); b.writeDouble(v.output); b.writeDouble(v.chance); b.writeBoolean(v.low); b.writeLong(v.requestId); },b->new Quoted(b.readUUID(),b.readUUID(),b.readUtf(256),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readBoolean(),b.readLong()));
        @Override public Type<Quoted> type() { return TYPE; }
    }
    public record Result(UUID session, UUID token, boolean won, String target, int count, double chance, double landing) implements CustomPacketPayload {
        public Result { if(count<1||count>99||!Double.isFinite(chance)||chance<=0||chance>=1||!Double.isFinite(landing)||landing<0||landing>=1||won!=(landing<chance))throw new IllegalArgumentException("Invalid result"); }
        public static final Type<Result> TYPE=UpgradePackets.type("result");
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=StreamCodec.of((b,v)->{ b.writeUUID(v.session); b.writeUUID(v.token); b.writeBoolean(v.won); b.writeUtf(v.target,256); b.writeVarInt(v.count); b.writeDouble(v.chance); b.writeDouble(v.landing); },b->new Result(b.readUUID(),b.readUUID(),b.readBoolean(),b.readUtf(256),b.readVarInt(),b.readDouble(),b.readDouble()));
        @Override public Type<Result> type() { return TYPE; }
    }
    public record Rejected(String reason, long requestId) implements CustomPacketPayload {
        public Rejected(String reason){this(reason,0);}
        public static final Type<Rejected> TYPE=UpgradePackets.type("rejected");
        public static final StreamCodec<RegistryFriendlyByteBuf,Rejected> CODEC=StreamCodec.of((b,v)->{ b.writeUtf(v.reason,32); b.writeLong(v.requestId); },b->new Rejected(b.readUtf(32),b.readLong()));
        @Override public Type<Rejected> type() { return TYPE; }
    }
    private static PreviewRequest readPreview(RegistryFriendlyByteBuf b) {
        UUID session=b.readUUID(); int size=b.readVarInt();
        if(size<1 || size>36) throw new IllegalArgumentException("Invalid offer size");
        var offers=new ArrayList<Offer>();
        for(int i=0;i<size;i++) offers.add(new Offer(b.readVarInt(),b.readVarInt()));
        return new PreviewRequest(session,List.copyOf(offers),b.readUtf(256),b.readVarInt(),b.readUtf(8),b.readLong());
    }
}
