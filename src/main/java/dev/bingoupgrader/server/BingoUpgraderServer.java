package dev.bingoupgrader.server;

import dev.bingoupgrader.server.value.*;
import dev.bingoupgrader.server.network.ValueNetworking;
import dev.bingoupgrader.common.BingoUpgraderMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class BingoUpgraderServer {
    private BingoUpgraderServer() { }
    public static void initialize() {
        ValueNetworking.register();
        ValueCommands.register();
        dev.bingoupgrader.server.network.BingoNetworking.register();
        dev.bingoupgrader.server.bingo.BingoCommands.register();
        dev.bingoupgrader.server.network.UpgradeNetworking.register();
        dev.bingoupgrader.server.upgrader.UpgradeCommands.register();
        ServerLifecycleEvents.SERVER_STARTED.register(dev.bingoupgrader.server.upgrader.UpgradeService::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(dev.bingoupgrader.server.upgrader.UpgradeService::stop);
        ServerLifecycleEvents.SERVER_STARTED.register(ValueService::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(ValueService::stop);
        ServerLifecycleEvents.SERVER_STARTED.register(dev.bingoupgrader.server.bingo.BingoService::startService);
        ServerLifecycleEvents.SERVER_STOPPED.register(dev.bingoupgrader.server.bingo.BingoService::stopService);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> dev.bingoupgrader.server.bingo.BingoService.get(server).tick());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            if (success) server.execute(() -> {
                try { ValueService.get(server).rebuild(); ValueNetworking.broadcast(server); }
                catch (RuntimeException e) { BingoUpgraderMod.LOGGER.error("Could not rebuild values after datapack reload", e); }
            });
        });
    }
}
