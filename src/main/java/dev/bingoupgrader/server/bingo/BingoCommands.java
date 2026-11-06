package dev.bingoupgrader.server.bingo;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.bingoupgrader.server.network.BingoNetworking;

public final class BingoCommands {
    private BingoCommands() { }
    @FunctionalInterface private interface Run {void run(CommandSourceStack source,BingoService service) throws Exception;}
    private static int call(CommandContext<CommandSourceStack> c,Run run){try{run.run(c.getSource(),BingoService.get(c.getSource().getServer()));return 1;}catch(Exception ex){c.getSource().sendFailure(Component.translatable("bingoupgrader.bingo.error"));dev.bingoupgrader.common.BingoUpgraderMod.LOGGER.warn("Bingo command failed: {}",ex.getMessage());return 0;}}
    private static boolean manager(CommandSourceStack s){try{return BingoService.get(s.getServer()).canManage(s);}catch(IllegalStateException ex){return s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);}}
    public static void register(){CommandRegistrationCallback.EVENT.register((d,ctx,e)->d.register(Commands.literal("bingo")
        .executes(c->call(c,(s,b)->BingoNetworking.sync(s.getPlayerOrException(),"card")))
        .then(Commands.literal("start").requires(BingoCommands::manager).executes(c->call(c,(s,b)->b.startMatch(s))))
        .then(Commands.literal("stop").requires(BingoCommands::manager).executes(c->call(c,(s,b)->b.stopMatch(s))))
        .then(Commands.literal("reset").requires(BingoCommands::manager).executes(c->call(c,(s,b)->b.reset(s))))
        .then(Commands.literal("restart").requires(BingoCommands::manager).executes(c->call(c,(s,b)->b.restart(s))))
        .then(Commands.literal("card").executes(c->call(c,(s,b)->BingoNetworking.sync(s.getPlayerOrException(),"card"))))
        .then(Commands.literal("scoreboard").executes(c->call(c,(s,b)->{b.scoreboard(s);if(s.getPlayer()!=null)BingoNetworking.sync(s.getPlayer(),"scoreboard");})))
        .then(Commands.literal("settings").executes(c->call(c,(s,b)->BingoNetworking.sync(s.getPlayerOrException(),"settings")))
            .then(Commands.literal("reload").requires(BingoCommands::manager).executes(c->call(c,(s,b)->{b.reload();b.flush();})))
            .then(Commands.argument("field",StringArgumentType.word()).requires(BingoCommands::manager)
                .suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"mode","difficulty","goal","freeCenter","surrender","timeLimitSeconds"},b))
                .then(Commands.argument("value",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(dev.bingoupgrader.common.config.BingoConfig.difficulties(),b)).executes(c->call(c,(s,b)->b.setting(s,StringArgumentType.getString(c,"field"),StringArgumentType.getString(c,"value")))))))));}
}
