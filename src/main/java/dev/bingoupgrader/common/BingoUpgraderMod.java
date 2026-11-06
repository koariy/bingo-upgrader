package dev.bingoupgrader.common;

import dev.bingoupgrader.server.BingoUpgraderServer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint: runs on both a dedicated server and an integrated server/client. */
public final class BingoUpgraderMod implements ModInitializer {
    public static final String MOD_ID = "bingo_upgrader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register server-safe services here; never reference client classes.
        dev.bingoupgrader.common.upgrader.UpgraderBlock.register();
        BingoUpgraderServer.initialize();
        LOGGER.info("Bingo Upgrader initialized (bingo, values and upgrader modules)");
    }
}
