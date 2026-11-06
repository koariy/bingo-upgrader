package dev.bingoupgrader.server.upgrader;
import net.minecraft.world.item.ItemStack;
import java.util.List;
/** Operates only on a private simulated inventory; caller commits after validation. */
public final class InventoryTransaction {
    private InventoryTransaction() { }
    public static boolean insert(List<ItemStack> slots, ItemStack reward) {
        ItemStack remaining=reward.copy();
        for(var stack:slots) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,remaining)) {
                int n=Math.min(remaining.getCount(),Math.max(0,stack.getMaxStackSize()-stack.getCount()));
                stack.grow(n); remaining.shrink(n); if(remaining.isEmpty()) return true;
            }
        }
        for(int i=0;i<slots.size();i++) if(slots.get(i).isEmpty()) {
            int n=Math.min(remaining.getCount(),remaining.getMaxStackSize());
            slots.set(i,remaining.copyWithCount(n)); remaining.shrink(n); if(remaining.isEmpty()) return true;
        }
        return remaining.isEmpty();
    }
}
