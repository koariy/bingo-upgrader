package dev.bingoupgrader.server.upgrader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** Temporary text controls using exactly the same validated service as future GUI requests. */
public final class UpgradeCommands {
    private UpgradeCommands() { }
    @FunctionalInterface private interface Action { void run(net.minecraft.server.level.ServerPlayer p,UpgradeService s) throws Exception; }
    private static int call(CommandContext<CommandSourceStack> c,Action action) {
        try { var p=c.getSource().getPlayerOrException(); action.run(p,UpgradeService.get(c.getSource().getServer())); return 1; }
        catch(Exception ex) { c.getSource().sendFailure(Component.translatable("bingoupgrader.upgrade.rejected.session")); return 0; }
    }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((d,ctx,env)->d.register(Commands.literal("upgrader")
            .executes(c->call(c,(p,s)->s.open(p)))
            .then(Commands.literal("clear").executes(c->call(c,(p,s)->s.clearSelection(p))))
            .then(Commands.literal("offer").then(Commands.argument("slot",IntegerArgumentType.integer(0,35))
                .then(Commands.argument("count",IntegerArgumentType.integer(1,99)).executes(c->call(c,(p,s)->s.select(p,IntegerArgumentType.getInteger(c,"slot"),IntegerArgumentType.getInteger(c,"count")))))))
            .then(Commands.literal("preview").then(Commands.argument("item",StringArgumentType.word())
                .suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggestResource(net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet(),b))
                .then(Commands.argument("count",IntegerArgumentType.integer(1,99)).executes(c->call(c,(p,s)->s.commandPreview(p,StringArgumentType.getString(c,"item"),IntegerArgumentType.getInteger(c,"count"),"manual"))))))
            .then(Commands.literal("quick").then(Commands.argument("mode",StringArgumentType.word())
                .suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"x2","x4","x8","p30","p50","p70"},b))
                .executes(c->call(c,(p,s)->s.commandPreview(p,"",1,StringArgumentType.getString(c,"mode"))))))
            .then(Commands.literal("roll").executes(c->call(c,(p,s)->s.commandRoll(p))))));
    }
}
