package dev.bingoupgrader;

import dev.bingoupgrader.common.config.ValueConfig;
import dev.bingoupgrader.common.value.*;
import dev.bingoupgrader.common.network.ValueTablePayload;
import dev.bingoupgrader.server.value.RecipeValueSolver;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.effect.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import java.util.*;

/** Dependency-free executable checks of real Minecraft components and network codec. */
public final class ValueModuleChecks {
    private static int checks;
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("Check " + checks); }
    private static void equal(double a, double b) { check(Math.abs(a-b) < 1e-8); }
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();
        var lookup = net.minecraft.data.registries.VanillaRegistries.createLookup();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
            .forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        var config = new ValueConfig(); config.validate();
        equal(config.baseValues.get("minecraft:stick"), 1);
        equal(config.baseValues.get("minecraft:nether_star"), 8000);
        config.craftingMarkup = .9;
        try { config.validate(); throw new AssertionError("Accepted loss-making markup"); } catch (IllegalArgumentException expected) { checks++; }
        config.craftingMarkup = 1.05;
        config.overrides.put("minecraft:stone", Double.NaN);
        try { config.validate(); throw new AssertionError("Accepted NaN"); } catch (IllegalArgumentException expected) { checks++; }
        var rules = List.of(
            new RecipeValueSolver.Rule("plank",4,List.of(List.of("wood","expensive")),"planks"),
            new RecipeValueSolver.Rule("table",1,List.of(List.of("plank"),List.of("plank"),List.of("plank"),List.of("plank")),"table"),
            new RecipeValueSolver.Rule("loop1",1,List.of(List.of("loop2")),"loop"),
            new RecipeValueSolver.Rule("loop2",1,List.of(List.of("loop1")),"loop"),
            new RecipeValueSolver.Rule("forbidden_result",1,List.of(List.of("egg")),"blocked"));
        var v = RecipeValueSolver.solve(Map.of("wood",2.0,"expensive",10.0,"egg",100.0),Set.of("egg"),rules,1.05);
        equal(v.get("plank"), .525); equal(v.get("table"), 2.205);
        check(!v.containsKey("loop1")); check(!v.containsKey("forbidden_result")); check(!v.containsKey("egg"));
        var reversed = new ArrayList<>(rules); Collections.reverse(reversed);
        check(v.equals(RecipeValueSolver.solve(Map.of("wood",2.0,"expensive",10.0,"egg",100.0),Set.of("egg"),reversed,1.05)));
        var entries = Map.of("minecraft:stick",new ValueTable.Entry(1,"base",false),
            "minecraft:iron_pickaxe",new ValueTable.Entry(50,"recipe",false),
            "minecraft:potion",new ValueTable.Entry(1,"unknown",false),
            "minecraft:dragon_egg",new ValueTable.Entry(0,"blacklist",true));
        var table = new ValueTable(7, entries, new ValueTable.Modifiers(20,5,30,.05));
        var sticks = new ItemStack(Items.STICK, 32);
        equal(StackValues.quote(table,sticks).unitValue(),1); equal(StackValues.quote(table,sticks).totalValue(),32);
        check(!StackValues.quote(table,new ItemStack(Items.DRAGON_EGG)).allowed());
        check(!StackValues.quote(table,ItemStack.EMPTY).allowed());
        var tool = new ItemStack(Items.IRON_PICKAXE);
        equal(StackValues.quote(table,tool).unitValue(),50);
        var enchantments = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        enchantments.set(lookup.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY), 2);
        tool.set(DataComponents.ENCHANTMENTS,enchantments.toImmutable());
        equal(StackValues.quote(table,tool).unitValue(),130);
        var bookEntries = new java.util.HashMap<>(entries);
        bookEntries.put("minecraft:enchanted_book",new ValueTable.Entry(2,"recipe",false));
        var bookTable = new ValueTable(8,bookEntries,table.modifiers());
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS,enchantments.toImmutable());
        equal(StackValues.quote(bookTable,book).unitValue(),82);
        tool.remove(DataComponents.ENCHANTMENTS);
        tool.setDamageValue(tool.getMaxDamage()); equal(StackValues.quote(table,tool).unitValue(),2.5);
        var potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.SPEED,1200,1)));
        equal(StackValues.quote(table,potion).unitValue(),11);
        check(StackValues.quote(table,potion).unknown());
        potion.set(DataComponents.POTION_CONTENTS, PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.INSTANT_HEALTH,1,0)));
        equal(StackValues.quote(table,potion).unitValue(),31);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            ValueTablePayload.CODEC.encode(buffer,new ValueTablePayload(table));
            check(ValueTablePayload.CODEC.decode(buffer).table().equals(table));
        } finally { buffer.release(); }
        var malformed = new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            malformed.writeLong(1); for (double d : new double[]{20,5,30,.05}) malformed.writeDouble(d);
            malformed.writeVarInt(100_001);
            try { ValueTablePayload.CODEC.decode(malformed); throw new AssertionError("Unbounded packet"); }
            catch (IllegalArgumentException expected) { checks++; }
        } finally { malformed.release(); }
        System.out.println("Value module: " + checks + " checks passed");
        UpgradeModuleChecks.run();
        BingoModuleChecks.run();
        UiModuleChecks.run();
    }
}
