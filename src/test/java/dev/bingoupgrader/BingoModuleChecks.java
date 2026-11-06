package dev.bingoupgrader;
import dev.bingoupgrader.common.bingo.*;
import dev.bingoupgrader.common.config.BingoConfig;
import dev.bingoupgrader.common.value.ValueTable;
import dev.bingoupgrader.common.network.BingoPackets.*;
import dev.bingoupgrader.server.bingo.*;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import io.netty.buffer.Unpooled;
import java.util.*;

public final class BingoModuleChecks {
    private static int checks;
    private static void check(boolean b){checks++;if(!b)throw new AssertionError("Bingo check "+checks);}
    private static final String A=UUID.randomUUID().toString(),B=UUID.randomUUID().toString();
    private static final Item[] ITEMS={Items.STICK,Items.COBBLESTONE,Items.IRON_INGOT,Items.GOLD_INGOT,Items.REDSTONE,
        Items.DIAMOND,Items.EMERALD,Items.COAL,Items.QUARTZ,Items.NETHERITE_INGOT,Items.LAPIS_LAZULI,Items.RAW_IRON,
        Items.RAW_GOLD,Items.RAW_COPPER,Items.COPPER_INGOT,Items.SAND,Items.GRAVEL,Items.DIRT,Items.STONE,
        Items.FLINT,Items.CLAY_BALL,Items.LEATHER,Items.STRING,Items.BONE,Items.GUNPOWDER};
    private static BingoState state(String mode,String goal,boolean surrender,boolean free){
        var s=new BingoState();s.rules.mode=mode;s.rules.goal=goal;s.rules.surrender=surrender;s.rules.freeCenter=free;
        if(free){var t=s.rules.tiers.get(0);s.rules.tiers.set(0,new BingoConfig.Tier(t.name(),t.min(),t.max(),9));}
        s.card=new ArrayList<>(Arrays.stream(ITEMS).map(i->BuiltInRegistries.ITEM.getKey(i).toString()).toList());
        if(free)s.card.set(12,"");s.status="running";int mask=free?BingoRules.CENTER:0;s.sharedMask=mask;
        s.players.put(A,new BingoState.Progress("Alice",mask));s.players.put(B,new BingoState.Progress("Bob",mask));s.validate();return s;
    }
    private static SimpleContainer inventory(int... cells){var inv=new SimpleContainer(36);for(int i=0;i<cells.length;i++)inv.setItem(i,new ItemStack(ITEMS[cells[i]],3));return inv;}
    public static void run() {
        check(BingoRules.lines(BingoRules.FULL)==12);check(BingoRules.lines(0)==0);
        check(BingoRules.won(31,"line"));check(!BingoRules.won(31,"two_lines"));
        check(BingoRules.won(1023,"two_lines"));check(!BingoRules.won(1023,"full"));check(BingoRules.won(BingoRules.FULL,"full"));
        int diag=0,anti=0;for(int i=0;i<5;i++){diag|=1<<(i*6);anti|=1<<(4+i*4);}check(BingoRules.lines(diag)==1);check(BingoRules.lines(anti)==1);
        check(BingoRules.winningCells(31)==31);check(BingoRules.cells(BingoRules.CENTER,true)==0);
        int[] previous=null;
        for(String level:BingoConfig.difficulties())for(boolean freeCenter:new boolean[]{false,true}) {
            var preset=new BingoConfig();preset.freeCenter=freeCenter;preset.applyDifficulty(level);preset.validate();
            check(preset.displayedDifficulty().equals(level));check(preset.tiers.stream().mapToInt(BingoConfig.Tier::count).sum()==(freeCenter?24:25));
        }
        for(String level:BingoConfig.difficulties()){
            int[] counts=BingoConfig.distribution(level,false);int prefix=0,oldPrefix=0;
            if(previous!=null)for(int i=0;i<4;i++){prefix+=counts[i];oldPrefix+=previous[i];check(prefix<=oldPrefix);}
            previous=counts;
        }
        var legacy=dev.bingoupgrader.common.bingo.BingoJson.JSON.fromJson("{}",BingoConfig.class);legacy.validate();check(legacy.difficulty.equals("normal"));
        var custom=new BingoConfig();custom.tiers.set(0,new BingoConfig.Tier("easy",0,10,11));custom.tiers.set(1,new BingoConfig.Tier("medium",10,100,9));custom.validate();check(custom.displayedDifficulty().equals("custom"));
        var easyConfig=BingoConfig.fromJson(com.google.gson.JsonParser.parseString("{\"difficulty\":\"easy\"}").getAsJsonObject());check(easyConfig.tiers.get(0).count()==20);
        var freeConfig=BingoConfig.fromJson(com.google.gson.JsonParser.parseString("{\"difficulty\":\"expert\",\"freeCenter\":true}").getAsJsonObject());check(freeConfig.tiers.get(0).count()==1);
        var oldConfig=dev.bingoupgrader.common.bingo.BingoJson.JSON.toJsonTree(custom).getAsJsonObject();oldConfig.remove("difficulty");
        var preserved=BingoConfig.fromJson(oldConfig);check(preserved.difficulty.equals("custom"));check(preserved.tiers.get(0).count()==11);
        check(!BingoGenerator.fitsDifficulty("easy","minecraft:end_stone"));
        check(!BingoGenerator.fitsDifficulty("easy","minecraft:warped_planks"));
        check(!BingoGenerator.fitsDifficulty("easy","minecraft:nether_bricks"));
        check(BingoGenerator.fitsDifficulty("easy","minecraft:oak_planks"));
        check(BingoGenerator.fitsDifficulty("hard","minecraft:end_stone"));
        var cfg=new BingoConfig();cfg.validate();var entries=new TreeMap<String,ValueTable.Entry>();
        for(int tier=0;tier<4;tier++)for(int i=0;i<30;i++)entries.put("test:t"+tier+"_"+i,new ValueTable.Entry(new double[]{1,15,250,1500}[tier],"base",false));
        entries.put("test:unknown",new ValueTable.Entry(1,"unknown",false));entries.put("test:blocked",new ValueTable.Entry(1,"base",true));
        var table=new ValueTable(1,entries,new ValueTable.Modifiers(20,5,30,.05));
        for(int seed=0;seed<100;seed++) {
            var card=BingoGenerator.generate(cfg,table,id->!id.equals("test:t0_0"),new Random(seed));
            check(card.size()==25 && new HashSet<>(card).size()==25);check(!card.contains("test:unknown")&&!card.contains("test:blocked")&&!card.contains("test:t0_0"));
            for(var tier:cfg.tiers)check(card.stream().filter(id->entries.get(id).value()>=tier.min()&&entries.get(id).value()<tier.max()).count()==tier.count());
        }
        for(String difficulty:BingoConfig.difficulties()){
            var preset=new BingoConfig();preset.applyDifficulty(difficulty);
            var generated=BingoGenerator.generate(preset,table,id->true,new Random(101));
            for(var band:preset.tiers)check(generated.stream().filter(id->entries.get(id).value()>=band.min()&&entries.get(id).value()<band.max()).count()==band.count());
        }
        cfg.freeCenter=true;cfg.tiers.set(0,new BingoConfig.Tier("easy",0,10,9));var free=BingoGenerator.generate(cfg,table,id->true,new Random(1));check(free.size()==25&&free.get(12).isEmpty());
        try{BingoGenerator.generate(cfg,table,id->false,new Random());throw new AssertionError("Undersized pool");}catch(IllegalArgumentException expected){checks++;}
        var competitive=state("competitive","line",false,false);var inv=inventory(0,1,2,3,4);
        var delta=BingoProgress.claim(competitive,A,"Alice",inv);check(delta.cells()==5&&delta.lines()==1&&delta.won());
        check(inv.getItem(0).getCount()==3);check(competitive.players.get(B).mask==0);check(competitive.winners.equals(List.of(A)));
        check(BingoProgress.claim(competitive,B,"Bob",inventory(0,1,2,3,4)).cells()==0);
        var turnIn=state("solo","full",true,false);var material=inventory(0);
        check(BingoProgress.claim(turnIn,A,"Alice",material).cells()==1);check(material.getItem(0).getCount()==2);
        check(BingoProgress.claim(turnIn,A,"Alice",material).cells()==0);check(material.getItem(0).getCount()==2);
        check(BingoProgress.claim(turnIn,UUID.randomUUID().toString(),"Spectator",material).cells()==0);
        var coop=state("coop","line",true,false);var first=inventory(0,1,2);var second=inventory(2,3,4);
        check(BingoProgress.claim(coop,A,"Alice",first).cells()==3);var finish=BingoProgress.claim(coop,B,"Bob",second);
        check(finish.cells()==2&&finish.won());check(second.getItem(0).getCount()==3);check(second.getItem(1).getCount()==2);
        check(coop.sharedMask==31);check(coop.players.get(A).lineCredits==0&&coop.players.get(B).lineCredits==1);
        check(new HashSet<>(coop.winners).equals(Set.of(A,B)));
        var two=state("competitive","two_lines",false,false);check(!BingoProgress.claim(two,A,"Alice",inventory(0,1,2,3,4)).won());
        check(BingoProgress.claim(two,A,"Alice",inventory(5,6,7,8,9)).won());
        var full=state("solo","full",true,true);var all=new SimpleContainer(36);for(int i=0;i<25;i++)all.setItem(i,new ItemStack(ITEMS[i]));
        check(BingoProgress.claim(full,A,"Alice",all).won());check(full.players.get(A).mask==BingoRules.FULL);check(all.getItem(12).getCount()==1);
        check(BingoRules.cells(full.players.get(A).mask,true)==24);
        var saved=BingoSavedData.TYPE.constructor().get();saved.state=turnIn;saved.state.elapsedTicks=1450;saved.state.revision=15;
        var encoded=BingoSavedData.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
        var loaded=BingoSavedData.TYPE.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
        var nbt=BingoSavedData.TYPE.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,saved).getOrThrow();
        var fromNbt=BingoSavedData.TYPE.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE,nbt).getOrThrow();
        check(fromNbt.state.elapsedTicks==1450&&fromNbt.state.players.get(A).mask==1);
        check(fromNbt.state.rules.surrender&&!fromNbt.state.rules.freeCenter);
        var freeSave=BingoSavedData.TYPE.constructor().get();freeSave.state=full;
        var freeNbt=BingoSavedData.TYPE.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,freeSave).getOrThrow();
        check(BingoSavedData.TYPE.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE,freeNbt).getOrThrow().state.rules.freeCenter);
        check(loaded.state.matchId.equals(turnIn.matchId));check(loaded.state.elapsedTicks==1450&&loaded.state.revision==15);check(loaded.state.players.get(A).mask==1);
        check(BingoProgress.claim(loaded.state,A,"Alice",material).cells()==0);check(material.getItem(0).getCount()==2);
        var timed=state("solo","full",false,false);timed.rules.timeLimitSeconds=1;
        for(int i=0;i<19;i++)check(!BingoClock.advance(timed));
        check(BingoClock.advance(timed)&&timed.status.equals("timeout")&&timed.elapsedTicks==20);
        check(!BingoClock.advance(timed)&&timed.elapsedTicks==20);
        var unlimited=state("solo","full",false,false);unlimited.rules.timeLimitSeconds=0;
        for(int i=0;i<100;i++)check(!BingoClock.advance(unlimited));
        unlimited.status="stopped";check(!BingoClock.advance(unlimited)&&unlimited.elapsedTicks==100);
        var scores=BingoScores.rows(coop);check(scores.get(0).uuid().equals(B)&&scores.get(0).lines()==1);
        var rewardSlots=new ArrayList<ItemStack>();for(int i=0;i<36;i++)rewardSlots.add(ItemStack.EMPTY);
        check(dev.bingoupgrader.server.upgrader.InventoryTransaction.insert(rewardSlots,new ItemStack(Items.DIAMOND)));
        var rewardInventory=new SimpleContainer(36);for(int i=0;i<36;i++)rewardInventory.setItem(i,rewardSlots.get(i));
        var rewarded=state("solo","full",false,false);
        check(BingoProgress.claim(rewarded,A,"Alice",rewardInventory).cells()==1);
        check((rewarded.players.get(A).mask&(1<<5))!=0);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            var control=new Control(turnIn.matchId,1,"set","mode","coop");Control.CODEC.encode(buffer,control);check(Control.CODEC.decode(buffer).equals(control));
            var notice=new Notice("line","Alice",2);Notice.CODEC.encode(buffer,notice);check(Notice.CODEC.decode(buffer).equals(notice));
            var packet=new Snapshot("{}","card");Snapshot.CODEC.encode(buffer,packet);check(Snapshot.CODEC.decode(buffer).equals(packet));
        }finally{buffer.release();}
        turnIn.players.get(A).mask=1<<30;try{turnIn.validate();throw new AssertionError("Invalid saved mask");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("Bingo module: "+checks+" checks passed (including 100 generated cards)");
    }
}
