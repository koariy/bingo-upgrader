package dev.bingoupgrader.server.upgrader;

import dev.bingoupgrader.common.config.UpgraderConfig;
import dev.bingoupgrader.common.upgrader.*;
import dev.bingoupgrader.server.value.ValueService;
import dev.bingoupgrader.server.network.UpgradeNetworking;
import dev.bingoupgrader.common.network.UpgradePackets.*;
import dev.bingoupgrader.common.BingoUpgraderMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.*;
import java.io.IOException;
import java.security.SecureRandom;

/** All operations run on the server thread. No input escrow: real inventory remains authoritative. */
public final class UpgradeService {
    private static final Map<MinecraftServer,UpgradeService> SERVICES = new IdentityHashMap<>();
    private final MinecraftServer server;
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID,Session> sessions = new HashMap<>();
    private final Map<UUID,Window> requestLimits = new HashMap<>();
    private UpgraderConfig config;
    private final UpgradeHistory history;
    private static final class Window { long start; int count; }
    private static final class Session {
        UUID id = UUID.randomUUID(); long expires; Preview preview;
        List<Offer> selection = new ArrayList<>();
    }
    private record Preview(UUID token, long expires, long revision, List<Offer> offers,
                           List<ItemStack> snapshot, ItemStack target, double input, double output, double chance) { }
    private UpgradeService(MinecraftServer server) {
        this.server = server; history = server.overworld().getDataStorage().computeIfAbsent(UpgradeHistory.TYPE);
        try { config = UpgraderConfigStore.load(); }
        catch (IOException e) { BingoUpgraderMod.LOGGER.error("Invalid upgrader config; using defaults",e); config = new UpgraderConfig(); }
    }
    public static void start(MinecraftServer server) { SERVICES.put(server,new UpgradeService(server)); }
    public static void stop(MinecraftServer server) { SERVICES.remove(server); }
    public static UpgradeService get(MinecraftServer server) {
        var s = SERVICES.get(server); if (s == null) throw new IllegalStateException("Upgrader not ready"); return s;
    }
    public boolean isAllowedItem(ItemStack stack) { return UpgradePolicy.allowedTarget(stack,ValueService.get(server),config); }
    public void reload() throws IOException { applyConfig(UpgraderConfigStore.load()); }
    public void applyConfig(UpgraderConfig next) { next.validate(); config = next; sessions.clear(); }
    public void disconnect(UUID player) { sessions.remove(player); } // Rate history deliberately retained.
    private boolean request(ServerPlayer p) {
        if (!server.isSameThread()) throw new IllegalStateException("Server thread required");
        if (!p.isAlive() || p.isSpectator() || p.isCreative() || p.containerMenu != p.inventoryMenu) return false;
        long now = System.nanoTime(); Window w = requestLimits.computeIfAbsent(p.getUUID(),id -> new Window());
        if (now-w.start >= 1_000_000_000L) { w.start = now; w.count = 0; }
        return ++w.count <= config.requestsPerSecond;
    }
    private Session session(ServerPlayer p, UUID id) {
        Session s = sessions.get(p.getUUID());
        if (s == null || !s.id.equals(id) || s.expires < System.nanoTime()) throw new IllegalArgumentException("session");
        return s;
    }
    public void open(ServerPlayer p) {
        if (!request(p)) return;
        Session s = new Session(); s.expires = System.nanoTime()+config.sessionSeconds*1_000_000_000L;
        int slot = p.getInventory().getSelectedSlot();
        if (!p.getInventory().getItem(slot).isEmpty()) s.selection.add(new Offer(slot,p.getInventory().getItem(slot).getCount()));
        sessions.put(p.getUUID(),s);
        UpgradeNetworking.send(p,new Opened(s.id,ValueService.get(server).table().revision()));
        var catalog=ValueService.get(server).table().entries().keySet().stream().filter(id->{var stack=target(id,1);return isAllowedItem(stack)&&stack.isItemEnabled(p.level().enabledFeatures());}).toList();
        UpgradeNetworking.send(p,new dev.bingoupgrader.common.network.UpgradeCatalog(catalog,config.houseEdge,config.minChance,config.maxChance,config.maxTargetValue,config.previewSeconds));
        // Graphical client displays the session; text subcommands remain available.
    }
    public UUID currentSession(ServerPlayer p) {
        var s = sessions.get(p.getUUID()); if (s == null) throw new IllegalArgumentException("session"); return s.id;
    }
    public void clearSelection(ServerPlayer p) {
        if (!request(p)) return; var s = session(p,currentSession(p)); s.selection.clear(); s.preview=null;
    }
    public void select(ServerPlayer p, int slot, int count) {
        if (!request(p)) return; var s = session(p,currentSession(p));
        var o = new Offer(slot,count); s.selection.removeIf(old -> old.slot()==slot); s.selection.add(o); s.preview=null;
    }
    public void commandPreview(ServerPlayer p, String target, int count, String mode) {
        var id = currentSession(p); preview(p,id,session(p,id).selection,target,count,mode);
    }
    public void preview(ServerPlayer p, UUID sessionId, List<Offer> offers, String targetId, int targetCount, String mode) {
        preview(p,sessionId,offers,targetId,targetCount,mode,0);
    }
    public void preview(ServerPlayer p, UUID sessionId, List<Offer> offers, String targetId, int targetCount, String mode, long requestId) {
        if (!request(p)) return;
        try {
            var s = session(p,sessionId); s.preview = null;
            List<Offer> safe = List.copyOf(offers);
            if (safe.isEmpty() || safe.size()>36 || safe.stream().map(Offer::slot).distinct().count()!=safe.size()) throw new IllegalArgumentException("offer");
            var values = ValueService.get(server); var snapshots = new ArrayList<ItemStack>(); double input=0;
            for (var o : safe) {
                var stack = p.getInventory().getItem(o.slot());
                if (stack.getCount()<o.count() || !UpgradePolicy.allowed(stack,values,config)) throw new IllegalArgumentException("item");
                snapshots.add(stack.copy()); input += values.getValue(stack.copyWithCount(o.count()));
            }
            ItemStack target = mode.equals("manual") ? target(targetId,targetCount) : choose(input,mode,values,p);
            if (!UpgradePolicy.allowedTarget(target,values,config) || !target.isItemEnabled(p.level().enabledFeatures())) throw new IllegalArgumentException("item");
            double output = values.getValue(target);
            var odds = UpgradeMath.calculate(input,output,history.stats(p.getUUID(),System.currentTimeMillis()).failures(),config);
            if (!odds.available()) throw new IllegalArgumentException("chance");
            var preview = new Preview(UUID.randomUUID(),System.nanoTime()+config.previewSeconds*1_000_000_000L,
                values.table().revision(),safe,List.copyOf(snapshots),target.copy(),input,output,odds.chance());
            s.preview=preview;
            UpgradeNetworking.send(p,new Quoted(s.id,preview.token(),targetId(target),target.getCount(),input,output,odds.chance(),odds.veryLow(),requestId));
            if(requestId==0){p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("bingoupgrader.upgrade.preview",target.getHoverName(),target.getCount(),String.format(Locale.ROOT,"%.4f",odds.chance()*100)));if(odds.veryLow())message(p,"low");}
        } catch (IllegalArgumentException ex) { UpgradeNetworking.send(p,new Rejected(ex.getMessage(),requestId));if(requestId==0)message(p,"rejected."+ex.getMessage()); }
    }
    private static String targetId(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    private ItemStack target(String id, int count) {
        var key = Identifier.tryParse(id);
        if (key==null || !BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalArgumentException("item");
        var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(key));
        if (count<1 || count>stack.getMaxStackSize()) throw new IllegalArgumentException("count");
        stack.setCount(count); return stack;
    }
    private ItemStack choose(double input, String mode, ValueService values, ServerPlayer player) {
        double wanted = switch(mode) {
            case "x2" -> input*2; case "x4" -> input*4; case "x8" -> input*8;
            case "p30" -> input*(1-config.houseEdge)/.30;
            case "p50" -> input*(1-config.houseEdge)/.50;
            case "p70" -> input*(1-config.houseEdge)/.70;
            default -> throw new IllegalArgumentException("mode");
        };
        ItemStack best = ItemStack.EMPTY; double distance=Double.POSITIVE_INFINITY;
        for (var entry : values.table().entries().entrySet()) {
            var e = entry.getValue(); if (e.blocked() || e.source().equals("unknown") || e.value()<=0) continue;
            var base = target(entry.getKey(),1); if (!UpgradePolicy.allowed(base,values,config) || !base.isItemEnabled(player.level().enabledFeatures())) continue;
            int count = (int)Math.clamp(Math.round(wanted/e.value()),1,base.getMaxStackSize());
            base.setCount(count); double out=values.getValue(base);
            if (!UpgradeMath.calculate(input,out,0,config).available()) continue;
            double d=Math.abs(out-wanted);
            if (d<distance) { best=base; distance=d; }
        }
        if (best.isEmpty()) throw new IllegalArgumentException("target"); return best;
    }
    public void commandRoll(ServerPlayer p) {
        var s = session(p,currentSession(p)); if (s.preview==null) throw new IllegalArgumentException("preview");
        roll(p,s.id,s.preview.token());
    }
    public void roll(ServerPlayer p, UUID sessionId, UUID token) {
        if (!request(p)) return;
        try {
            var s = session(p,sessionId); Preview q=s.preview;
            if (q==null || !q.token().equals(token)) throw new IllegalArgumentException("preview");
            s.preview=null; // One use even when inventory/revision validation later fails.
            var values=ValueService.get(server); long now=System.currentTimeMillis();
            if (q.expires()<System.nanoTime() || q.revision()!=values.table().revision()) throw new IllegalArgumentException("stale");
            if (history.stats(p.getUUID(),now).attempts().size()>=config.attemptsPerMinute) throw new IllegalArgumentException("limit");
            for (int i=0;i<q.offers().size();i++) {
                var o=q.offers().get(i); var stack=p.getInventory().getItem(o.slot());
                if (!ItemStack.matches(stack,q.snapshot().get(i)) || !UpgradePolicy.allowed(stack,values,config)) throw new IllegalArgumentException("stale");
            }
            if (!UpgradePolicy.allowedTarget(q.target(),values,config)) throw new IllegalArgumentException("item");
            // Preflight both possible inventory outcomes before taking anything. Never drop rewards on ground.
            List<ItemStack> after = new ArrayList<>(); for(int i=0;i<36;i++) after.add(p.getInventory().getItem(i).copy());
            for(var o:q.offers()) after.get(o.slot()).shrink(o.count());
            List<ItemStack> wonInventory=after.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if (!InventoryTransaction.insert(wonInventory,q.target())) throw new IllegalArgumentException("space");
            boolean won=UpgradeMath.wins(random.nextDouble(),q.chance());
            // Single main-thread commit, no yields/callbacks between debit and credit.
            var outcome=won ? wonInventory : after;
            for(int i=0;i<36;i++) p.getInventory().setItem(i,outcome.get(i));
            history.record(p.getUUID(),now,won);
            dev.bingoupgrader.server.bingo.BingoService.inventoryChanged(p);
            p.getInventory().setChanged(); p.inventoryMenu.broadcastChanges();
            UpgradeNetworking.send(p,new Result(sessionId,token,won,targetId(q.target()),q.target().getCount(),q.chance(),UpgradeMath.landing(won,q.chance(),random.nextDouble())));
            message(p,won ? "won" : "lost");
        } catch (IllegalArgumentException ex) { reject(p,ex.getMessage()); }
    }
    private void reject(ServerPlayer p,String reason) { UpgradeNetworking.send(p,new Rejected(reason)); message(p,"rejected."+reason); }
    private static void message(ServerPlayer p,String key) { p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("bingoupgrader.upgrade."+key)); }
}
