package dev.bingoupgrader.server.bingo.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
@Mixin(ItemEntity.class)
public abstract class ItemPickupMixin {
    @Inject(method="playerTouch",at=@At("TAIL"))
    private void bingo$pickup(Player player,CallbackInfo ci){if(player instanceof ServerPlayer p)dev.bingoupgrader.server.bingo.BingoService.inventoryChanged(p);}
}
