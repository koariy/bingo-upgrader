package dev.bingoupgrader.server.bingo.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
@Mixin(ResultSlot.class)
public abstract class CraftingMixin {
    @Inject(method="onTake",at=@At("TAIL"))
    private void bingo$craft(Player player,ItemStack result,CallbackInfo ci){if(player instanceof ServerPlayer p)dev.bingoupgrader.server.bingo.BingoService.inventoryChanged(p);}
}
