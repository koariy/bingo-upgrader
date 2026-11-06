package dev.bingoupgrader;

import dev.bingoupgrader.common.config.UpgraderConfig;
import dev.bingoupgrader.common.upgrader.*;
import dev.bingoupgrader.common.network.UpgradePackets.*;
import dev.bingoupgrader.server.upgrader.*;
import net.minecraft.world.item.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import io.netty.buffer.Unpooled;
import java.util.*;

/** Real component checks share the bootstrap performed by ValueModuleChecks. */
public final class UpgradeModuleChecks {
    private static int checks;
    private static void check(boolean b) { checks++; if(!b) throw new AssertionError("Upgrade check "+checks); }
    public static void run() throws Exception {
        var cfg=new UpgraderConfig();cfg.validate();
        check(Math.abs(UpgradeMath.calculate(1,250,0,cfg).chance()-.0036)<1e-12);
        check(UpgradeMath.calculate(1,250,0,cfg).veryLow());
        check(Math.abs(UpgradeMath.calculate(64,250,0,cfg).chance()-.2304)<1e-12);
        check(!UpgradeMath.calculate(1,8000,0,cfg).available());
        check(!UpgradeMath.calculate(10,10,0,cfg).available());
        check(!UpgradeMath.calculate(Double.NaN,250,0,cfg).available());
        check(!UpgradeMath.calculate(1,100001,0,cfg).available());
        cfg.maxChance=.5;check(UpgradeMath.calculate(90,100,0,cfg).chance()==.5);cfg.maxChance=.9;
        check(UpgradeMath.wins(0,.3));check(!UpgradeMath.wins(.3,.3));check(!UpgradeMath.wins(.999,.3));
        cfg.pityEnabled=true;
        for(int failures:new int[]{0,9,10,100,1000000}) {
            double odds=UpgradeMath.calculate(64,250,failures,cfg).chance();
            check(odds>=.2304 && odds*250<=64*(1-cfg.houseEdge/2)+1e-10);
        }
        check(UpgradeMath.calculate(64,250,100,cfg).chance()>.2304);cfg.pityEnabled=false;
        var random=new SplittableRandom(2622026);int n=2_000_000;
        double[][] pairs={{1,250},{64,250},{15,250},{250,1500},{1500,5000},{3000,8000}};
        var report=new StringBuilder("input,output,trials,chance,expected,observed,tolerance\n");
        for(var pair:pairs) {
            double input=pair[0],output=pair[1];var odds=UpgradeMath.calculate(input,output,0,cfg);check(odds.available());
            long wins=0;for(int i=0;i<n;i++) if(UpgradeMath.wins(random.nextDouble(),odds.chance())) wins++;
            double expected=(1-cfg.houseEdge)*input,observed=wins*output/n;
            double se=output*Math.sqrt(odds.chance()*(1-odds.chance())/n);
            double tolerance=6*se;check(Math.abs(observed-expected)<tolerance);
            report.append(String.format(Locale.ROOT,"%.2f,%.2f,%d,%.8f,%.6f,%.6f,%.6f%n",input,output,n,odds.chance(),expected,observed,tolerance));
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("balance.csv"),report);
        var id=UUID.randomUUID();var history=UpgradeHistory.TYPE.constructor().get();
        history.record(id,100000,false);history.record(id,100001,false);
        check(history.stats(id,100002).failures()==2);check(history.stats(id,100002).attempts().size()==2);
        check(history.stats(id,160001).attempts().isEmpty());
        history.record(id,160002,true);check(history.stats(id,160002).failures()==0);
        // SavedData codec round trip: rate/pity survive a normal save/restart.
        var encoded=UpgradeHistory.TYPE.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE,history).getOrThrow();
        var restored=UpgradeHistory.TYPE.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
        check(restored.stats(id,160003).attempts().size()==1);
        var slots=new ArrayList<ItemStack>();for(int i=0;i<36;i++)slots.add(new ItemStack(Items.COBBLESTONE,64));
        var before=slots.stream().map(ItemStack::copy).toList();
        check(!InventoryTransaction.insert(slots,new ItemStack(Items.DIAMOND)));
        for(int i=0;i<36;i++) check(ItemStack.matches(before.get(i),slots.get(i)));
        slots.set(0,ItemStack.EMPTY);check(InventoryTransaction.insert(slots,new ItemStack(Items.DIAMOND,64)));
        check(slots.get(0).getCount()==64);
        slots.set(1,new ItemStack(Items.DIAMOND,63));check(InventoryTransaction.insert(slots,new ItemStack(Items.DIAMOND)));
        check(slots.get(1).getCount()==64);
        var table=new dev.bingoupgrader.common.value.ValueTable(1,
            Map.of("minecraft:dirt",new dev.bingoupgrader.common.value.ValueTable.Entry(1,"unknown",false),"minecraft:stick",new dev.bingoupgrader.common.value.ValueTable.Entry(1,"base",false),
                   "minecraft:bedrock",new dev.bingoupgrader.common.value.ValueTable.Entry(1,"unknown",false),
                   "minecraft:pig_spawn_egg",new dev.bingoupgrader.common.value.ValueTable.Entry(1,"unknown",false)),
            new dev.bingoupgrader.common.value.ValueTable.Modifiers(20,5,30,.05));
        check(UpgradePolicy.allowed(new ItemStack(Items.STICK),table,cfg));
        check(UpgradePolicy.allowed(new ItemStack(Items.DIRT),table,cfg));
        check(!UpgradePolicy.allowedTarget(new ItemStack(Items.DIRT),table,cfg));
        cfg.allowUnknownTargets=true;check(UpgradePolicy.allowedTarget(new ItemStack(Items.DIRT),table,cfg));cfg.allowUnknownTargets=false;
        check(!UpgradePolicy.allowed(new ItemStack(Items.BEDROCK),table,cfg));
        check(!UpgradePolicy.allowed(new ItemStack(Items.PIG_SPAWN_EGG),table,cfg));
        var custom=new ItemStack(Items.STICK);
        custom.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Unpriced component"));
        check(!UpgradePolicy.allowed(custom,table,cfg));
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            var request=new PreviewRequest(id,List.of(new Offer(0,64),new Offer(8,2)),"minecraft:diamond",1,"manual");
            PreviewRequest.CODEC.encode(buf,request);check(request.equals(PreviewRequest.CODEC.decode(buf)));
            var token=UUID.randomUUID();var start=new StartRequest(id,token);
            StartRequest.CODEC.encode(buf,start);check(StartRequest.CODEC.decode(buf).equals(start));
            var result=new Result(id,token,true,"minecraft:diamond",1,.23,.1);
            Result.CODEC.encode(buf,result);check(Result.CODEC.decode(buf).equals(result));
        } finally {buf.release();}
        try {new Offer(36,1);throw new AssertionError("Bad slot");}catch(IllegalArgumentException expected){checks++;}
        try {new Offer(0,-1);throw new AssertionError("Bad count");}catch(IllegalArgumentException expected){checks++;}
        cfg.houseEdge=0;try {cfg.validate();throw new AssertionError("Zero edge");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("Upgrade module: "+checks+" checks, 12,000,000 balance trials passed");
    }
}
