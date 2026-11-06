package dev.bingoupgrader.server.network;
import dev.bingoupgrader.common.network.UpgradePackets.*;
import dev.bingoupgrader.server.upgrader.UpgradeService;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class UpgradeNetworking {
    private UpgradeNetworking() { }
    public static void register() {
        var c2s=PayloadTypeRegistry.serverboundPlay(); var s2c=PayloadTypeRegistry.clientboundPlay();
        c2s.register(OpenRequest.TYPE,OpenRequest.CODEC); c2s.register(PreviewRequest.TYPE,PreviewRequest.CODEC); c2s.register(StartRequest.TYPE,StartRequest.CODEC);
        s2c.registerLarge(dev.bingoupgrader.common.network.UpgradeCatalog.TYPE,dev.bingoupgrader.common.network.UpgradeCatalog.CODEC,16*1024*1024);
        s2c.register(Opened.TYPE,Opened.CODEC); s2c.register(Quoted.TYPE,Quoted.CODEC);
        s2c.register(Result.TYPE,Result.CODEC); s2c.register(Rejected.TYPE,Rejected.CODEC);
        // Fabric handlers execute on the server thread; never defer a mutation after validation.
        ServerPlayNetworking.registerGlobalReceiver(OpenRequest.TYPE,(v,c)->UpgradeService.get(c.server()).open(c.player()));
        ServerPlayNetworking.registerGlobalReceiver(PreviewRequest.TYPE,(v,c)->UpgradeService.get(c.server()).preview(c.player(),v.session(),v.offers(),v.target(),v.count(),v.mode(),v.requestId()));
        ServerPlayNetworking.registerGlobalReceiver(StartRequest.TYPE,(v,c)->UpgradeService.get(c.server()).roll(c.player(),v.session(),v.token()));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->UpgradeService.get(s).disconnect(h.getPlayer().getUUID()));
    }
    public static void send(ServerPlayer p, CustomPacketPayload packet) {
        if(ServerPlayNetworking.canSend(p,packet.type())) ServerPlayNetworking.send(p,packet);
    }
}
