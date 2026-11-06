package dev.bingoupgrader.common.value;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.alchemy.PotionContents;

/** Pure calculation. Client callers use snapshots only; gameplay must use ValueService. */
public final class StackValues {
    private StackValues() { }
    public record Quote(double unitValue, double totalValue, boolean allowed, boolean unknown) { }
    public static Quote quote(ValueTable table, ItemStack stack) {
        if (table == null || stack.isEmpty()) return new Quote(0, 0, false, true);
        var e = table.entries().get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (e == null || e.blocked()) return new Quote(0, 0, false, e == null);
        var m = table.modifiers();
        double v = e.value();
        // Both equipped enchantments and stored book enchantments are real components in 26.2.
        for (var type : java.util.List.of(DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS)) {
            for (var enchant : stack.getOrDefault(type, ItemEnchantments.EMPTY).entrySet()) {
                int level = Math.clamp(enchant.getIntValue(), 0, 255);
                v += m.enchantmentPerLevel() * level * (double) level;
            }
        }
        for (var effect : stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects()) {
            double potency = Math.clamp((long) effect.getAmplifier() + 1, 1, 256);
            double duration = effect.isInfiniteDuration() ? 20 : Math.clamp(effect.getDuration() / 1200.0, 0, 20);
            v += potency * (effect.getEffect().value().isInstantaneous() ? m.instantEffectValue() : duration * m.potionPerMinute());
        }
        // Wear applies to the full enchanted tool value, not just its base material.
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0)
            v *= Math.max(m.minimumDurabilityFactor(), Math.clamp(1.0 - stack.getDamageValue() / (double) stack.getMaxDamage(), 0, 1));
        v = Math.min(1e12, v);
        return new Quote(v, Math.min(1e15, v * stack.getCount()), true, e.source().equals("unknown"));
    }
}
