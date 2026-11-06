package dev.bingoupgrader.server.network;

import dev.bingoupgrader.common.network.ValueTablePayload;
import dev.bingoupgrader.server.value.ValueService;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ValueNetworking {
    private ValueNetworking() { }
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(ValueTablePayload.TYPE, ValueTablePayload.CODEC, 16 * 1024 * 1024);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
    }
    public static void sync(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, ValueTablePayload.TYPE))
            ServerPlayNetworking.send(player, new ValueTablePayload(ValueService.get(player.level().getServer()).table()));
    }
    public static void broadcast(MinecraftServer server) { server.getPlayerList().getPlayers().forEach(ValueNetworking::sync); }
}
