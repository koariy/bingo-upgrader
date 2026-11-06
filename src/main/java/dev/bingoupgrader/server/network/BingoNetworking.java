package dev.bingoupgrader.server.network;
import dev.bingoupgrader.common.network.BingoPackets.*;
import dev.bingoupgrader.common.bingo.BingoJson;
import dev.bingoupgrader.server.bingo.BingoService;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BingoNetworking {
    private BingoNetworking() { }
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(Snapshot.TYPE,Snapshot.CODEC,1024*1024);
        PayloadTypeRegistry.clientboundPlay().register(Notice.TYPE,Notice.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Control.TYPE,Control.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Control.TYPE,(v,c)->BingoService.get(c.server()).request(c.player(),v.matchId(),v.controlRevision(),v.action(),v.field(),v.value()));
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->sync(h.getPlayer(),""));
    }
    public static void sync(ServerPlayer player,String screen) {
        if(ServerPlayNetworking.canSend(player,Snapshot.TYPE))ServerPlayNetworking.send(player,new Snapshot(BingoJson.JSON.toJson(BingoService.get(player.level().getServer()).view(player)),screen));
    }
    public static void broadcast(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())sync(p,"");}
    public static void event(MinecraftServer server,String kind,String player,int count){for(var p:server.getPlayerList().getPlayers())if(ServerPlayNetworking.canSend(p,Notice.TYPE))ServerPlayNetworking.send(p,new Notice(kind,player,count));}
}
