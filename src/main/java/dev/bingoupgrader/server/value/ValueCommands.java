package dev.bingoupgrader.server.value;

import dev.bingoupgrader.server.network.ValueNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import dev.bingoupgrader.common.BingoUpgraderMod;

public final class ValueCommands {
    private ValueCommands() { }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal("upgrader")
                .then(Commands.literal("values").then(Commands.argument("item", ItemArgument.item(context)).executes(c -> {
                    var stack = ItemArgument.getItem(c,"item").createItemStack(1);
                    var q = ValueService.get(c.getSource().getServer()).quote(stack);
                    c.getSource().sendSuccess(() -> Component.translatable(q.allowed() ? "bingoupgrader.command.value" : "bingoupgrader.value.blocked",
                        stack.getHoverName(), String.format(java.util.Locale.ROOT,"%.2f",q.unitValue()),
                        Component.translatable(q.unknown() ? "bingoupgrader.value.unknown" : "bingoupgrader.value.known")), false);
                    return 1;
                })))
                .then(Commands.literal("reload").requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).executes(c -> {
                    try {
                        var nextUpgrader = dev.bingoupgrader.server.upgrader.UpgraderConfigStore.load();
                        ValueService.get(c.getSource().getServer()).reload();
                        dev.bingoupgrader.server.upgrader.UpgradeService.get(c.getSource().getServer()).applyConfig(nextUpgrader);
                        ValueNetworking.broadcast(c.getSource().getServer());
                        c.getSource().sendSuccess(() -> Component.translatable("bingoupgrader.command.reloaded"), true); return 1;
                    } catch (Exception e) {
                        BingoUpgraderMod.LOGGER.error("Values reload failed",e);
                        c.getSource().sendFailure(Component.translatable("bingoupgrader.command.failed")); return 0;
                    }
                }))
                .then(Commands.literal("dump").requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).executes(c -> {
                    try {
                        var path = ValueService.get(c.getSource().getServer()).dump();
                        c.getSource().sendSuccess(() -> Component.translatable("bingoupgrader.command.dumped",path.toString()), false); return 1;
                    } catch (Exception e) {
                        BingoUpgraderMod.LOGGER.error("Values dump failed",e);
                        c.getSource().sendFailure(Component.translatable("bingoupgrader.command.failed")); return 0;
                    }
                }))));
    }
}
