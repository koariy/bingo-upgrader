package dev.bingoupgrader.server.upgrader;

import dev.bingoupgrader.common.config.UpgraderConfig;
import dev.bingoupgrader.server.value.ValueService;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import java.util.Set;

public final class UpgradePolicy {
    private UpgradePolicy() { }
    public static boolean allowedTarget(ItemStack stack,ValueService values,UpgraderConfig cfg){return allowedTarget(stack,values.table(),cfg);}
    public static boolean allowedTarget(ItemStack stack,dev.bingoupgrader.common.value.ValueTable table,UpgraderConfig cfg){
        return allowed(stack,table,cfg)&&(cfg.allowUnknownTargets||!dev.bingoupgrader.common.value.StackValues.quote(table,stack).unknown());
    }
    public static boolean allowed(ItemStack stack, ValueService values, UpgraderConfig cfg) {
        return allowed(stack,values.table(),cfg);
    }
    public static boolean allowed(ItemStack stack, dev.bingoupgrader.common.value.ValueTable table, UpgraderConfig cfg) {
        if (stack.isEmpty() || !dev.bingoupgrader.common.value.StackValues.quote(table,stack).allowed()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (cfg.blacklist.contains(id) || stack.getItem() instanceof SpawnEggItem) return false;
        for (String tag : cfg.blacklistTags)
            if (stack.is(TagKey.create(Registries.ITEM,Identifier.parse(tag)))) return false;
        // Do not turn unpriced container contents, copied custom data or arbitrary components into money.
        var accepted = Set.of(DataComponents.DAMAGE,DataComponents.ENCHANTMENTS,
            DataComponents.STORED_ENCHANTMENTS,DataComponents.POTION_CONTENTS);
        for (var patch : stack.getComponentsPatch().entrySet()) if (!accepted.contains(patch.getKey())) return false;
        return true;
    }
}
