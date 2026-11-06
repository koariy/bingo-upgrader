package dev.bingoupgrader.client.network;

import dev.bingoupgrader.common.network.ValueTablePayload;
import dev.bingoupgrader.common.value.*;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.world.item.ItemStack;

/** Read-only server snapshot, cleared between worlds/servers. Never used for authority. */
public final class ClientValues {
    private static ValueTable table;
    private ClientValues() { }
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ValueTablePayload.TYPE, (payload, context) -> {
            if (table == null || payload.table().revision() >= table.revision()) table = payload.table();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> table = null);
        ClientPlayConnectionEvents.INIT.register((handler, client) -> table = null);
    }
    public static ValueTable table(){return table;}
    public static boolean ready() { return table != null; }
    public static StackValues.Quote quote(ItemStack stack) { return StackValues.quote(table, stack); }
}
