package dev.bingoupgrader.server.bingo;

import dev.bingoupgrader.common.bingo.*;
import dev.bingoupgrader.common.config.BingoConfig;
import dev.bingoupgrader.common.BingoUpgraderMod;
import dev.bingoupgrader.server.value.ValueService;
import dev.bingoupgrader.server.upgrader.UpgradeService;
import dev.bingoupgrader.server.network.BingoNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.io.IOException;
import java.security.SecureRandom;

/** All mutation and inventory consumption run synchronously on the server thread. */
public final class BingoService {
    private static final Map<MinecraftServer,BingoService> SERVICES=new IdentityHashMap<>();
    private final MinecraftServer server;
    private final BingoSavedData data;
    private BingoConfig config;
    private final SecureRandom random=new SecureRandom();
    private final Set<UUID> checking=new HashSet<>();
    private boolean changed;
    private final Map<UUID,Long> requests=new HashMap<>();
    private BingoService(MinecraftServer server) {
        this.server=server;data=server.overworld().getDataStorage().computeIfAbsent(BingoSavedData.TYPE);
        try{config=BingoConfigStore.load();}catch(IOException ex){BingoUpgraderMod.LOGGER.error("Invalid bingo config; using defaults",ex);config=new BingoConfig();}
    }
    public static void startService(MinecraftServer server){SERVICES.put(server,new BingoService(server));}
    public static void stopService(MinecraftServer server){SERVICES.remove(server);}
    public static BingoService get(MinecraftServer server){var s=SERVICES.get(server);if(s==null)throw new IllegalStateException("Bingo not ready");return s;}
    public static void inventoryChanged(ServerPlayer player){var s=SERVICES.get(player.level().getServer());if(s!=null)s.checkPlayer(player);}
    private BingoState state(){return data.state;}
    private void touch(){state().revision++;data.setDirty();changed=true;}
    public boolean canManage(CommandSourceStack source){return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || source.getPlayer()!=null && config.managers.contains(source.getPlayer().getUUID().toString());}
    private void requireManager(CommandSourceStack source){if(!canManage(source))throw new IllegalArgumentException("permission");}
    private void requireIdle(){if(state().status.equals("running"))throw new IllegalArgumentException("running");}
    public void reload() throws IOException {config=BingoConfigStore.load();state().controlRevision++;touch();
        for(var p:server.getPlayerList().getPlayers())server.getCommands().sendCommands(p);
    }
    public void startMatch(CommandSourceStack source) { startMatch(source,false); }
    public void restart(CommandSourceStack source) { startMatch(source,true); }
    private void startMatch(CommandSourceStack source,boolean replace) {
        requireManager(source);if(!replace)requireIdle();
        var candidates=server.getPlayerList().getPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()).toList();
        if(config.mode.equals("solo")) {
            if(source.getPlayer()==null || !candidates.contains(source.getPlayer()))throw new IllegalArgumentException("players");
            candidates=List.of(source.getPlayer());
        }
        if(candidates.isEmpty() || candidates.size()>256)throw new IllegalArgumentException("players");
        var card=BingoGenerator.generate(config,ValueService.get(server).table(),id->{
            var item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id));var stack=new ItemStack(item);
            return stack.isItemEnabled(server.overworld().enabledFeatures()) && UpgradeService.get(server).isAllowedItem(stack);
        },random);
        // Construct completely before replacing the old match.
        var next=new BingoState();next.revision=state().revision;next.controlRevision=state().controlRevision+1;
        next.rules=BingoConfigStore.copy(config);next.card=new ArrayList<>(card);next.status="running";
        int free=config.freeCenter?BingoRules.CENTER:0;next.sharedMask=free;
        for(var p:candidates)next.players.put(p.getUUID().toString(),new BingoState.Progress(p.getGameProfile().name(),free));
        data.state=next;touch();event("started","",0,0);flush();
    }
    public void stopMatch(CommandSourceStack source){requireManager(source);if(!state().status.equals("running"))throw new IllegalArgumentException("not_running");state().status="stopped";state().controlRevision++;touch();event("stopped","",0,0);flush();}
    public void reset(CommandSourceStack source){requireManager(source);var next=new BingoState();next.revision=state().revision;next.controlRevision=state().controlRevision+1;data.state=next;touch();event("reset","",0,0);flush();}
    public void setting(CommandSourceStack source,String field,String value) throws IOException {
        requireManager(source);requireIdle();var cfg=BingoConfigStore.copy(config);
        switch(field) {
            case "mode"->cfg.mode=value;
            case "difficulty"->cfg.applyDifficulty(value);
            case "goal"->cfg.goal=value;
            case "surrender"->cfg.surrender=bool(value);
            case "freeCenter"->{boolean free=bool(value);if(free!=cfg.freeCenter){var t=cfg.tiers.get(0);cfg.tiers.set(0,new BingoConfig.Tier(t.name(),t.min(),t.max(),t.count()+(free?-1:1)));cfg.freeCenter=free;}}
            case "timeLimitSeconds"->cfg.timeLimitSeconds=Integer.parseInt(value);
            default->throw new IllegalArgumentException("settings");
        }
        cfg.validate();BingoConfigStore.save(cfg);config=cfg;state().controlRevision++;touch();flush();
    }
    private static boolean bool(String value){if(!Set.of("true","false").contains(value))throw new IllegalArgumentException("settings");return Boolean.parseBoolean(value);}
    public void tick() {
        if(state().status.equals("running")) {
            var s=state();
            if(BingoClock.advance(s)){touch();event("timeout","",0,0);}
            else {
                for(var player:server.getPlayerList().getPlayers()){if(!s.status.equals("running"))break;checkPlayer(player);}
                if(s.elapsedTicks%20==0)touch();
            }
        }
        flush();
    }
    private void checkPlayer(ServerPlayer player) {
        var s=state();String uuid=player.getUUID().toString();var progress=s.players.get(uuid);
        if(!server.isSameThread() || !s.status.equals("running") || progress==null || !player.isAlive() || player.isCreative() || player.isSpectator() || !checking.add(player.getUUID()))return;
        try {
            var delta=BingoProgress.claim(s,uuid,player.getGameProfile().name(),player.getInventory());
            if(delta.cells()==0)return;
            touch();event("marked",progress.name,delta.cells(),0);
            if(delta.lines()>0)event("line",progress.name,delta.lines(),0);
            if(s.rules.surrender){player.getInventory().setChanged();player.containerMenu.broadcastChanges();}
            if(delta.won())event(s.rules.mode.equals("coop")?"coop_won":"won",progress.name,0,0);
        } finally {checking.remove(player.getUUID());}
    }
    private void event(String kind,String player,int count,int unused){
        var message=Component.translatable("bingoupgrader.bingo.event."+kind,player,count);
        server.getPlayerList().broadcastSystemMessage(message,false);
        BingoNetworking.event(server,kind,player,count);
    }
    public BingoView view(ServerPlayer player) {
        var s=state();var progress=s.players.get(player.getUUID().toString());
        var scores=BingoScores.rows(s);
        var rules=BingoConfigStore.copy(s.rules);rules.managers.clear();var settings=BingoConfigStore.copy(config);settings.managers.clear();
        return new BingoView(s.matchId,s.revision,s.controlRevision,s.status,rules,settings,s.elapsedTicks,List.copyOf(s.card),
            s.rules.mode.equals("coop")?s.sharedMask:progress==null?0:progress.mask,scores,List.copyOf(s.winners),canManage(player.createCommandSourceStack()),progress!=null);
    }
    public void scoreboard(CommandSourceStack source) {
        for(var score:BingoScores.rows(state()))source.sendSuccess(()->Component.translatable("bingoupgrader.bingo.score",score.name(),score.cells(),score.lines()),false);
    }
    public void flush(){if(changed){changed=false;BingoNetworking.broadcast(server);}}
    public void request(ServerPlayer player,String matchId,long controlRevision,String action,String field,String value) {
        long now=System.nanoTime();Long last=requests.get(player.getUUID());if(last!=null && now-last<150_000_000L)return;requests.put(player.getUUID(),now);
        try {
            // Read-only views never require operator rights.
            if(action.equals("card") || action.equals("settings") || action.equals("scoreboard")){BingoNetworking.sync(player,action);return;}
            requireManager(player.createCommandSourceStack());
            if(!state().matchId.equals(matchId) || state().controlRevision!=controlRevision)throw new IllegalArgumentException("stale");
            var source=player.createCommandSourceStack();
            switch(action){case "start"->startMatch(source);case "stop"->stopMatch(source);case "reset"->reset(source);case "restart"->restart(source);case "set"->setting(source,field,value);default->throw new IllegalArgumentException("settings");}
        } catch(Exception ex){player.sendSystemMessage(Component.translatable("bingoupgrader.bingo.error"));BingoNetworking.sync(player,"");}
    }
}
