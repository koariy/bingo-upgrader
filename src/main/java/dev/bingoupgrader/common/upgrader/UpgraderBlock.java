package dev.bingoupgrader.common.upgrader;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import dev.bingoupgrader.server.upgrader.UpgradeService;

public final class UpgraderBlock {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("bingo_upgrader","upgrader");
    public static Block BLOCK;
    public static Item ITEM;
    private UpgraderBlock() { }
    public static void register() {
        BLOCK=Registry.register(BuiltInRegistries.BLOCK,ID,new Block(BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK,ID)).strength(3.5f).requiresCorrectToolForDrops()));
        ITEM=Registry.register(BuiltInRegistries.ITEM,ID,new BlockItem(BLOCK,new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM,ID)).useBlockDescriptionPrefix()));
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            if(!level.getBlockState(hit.getBlockPos()).is(BLOCK)) return InteractionResult.PASS;
            if(player instanceof ServerPlayer sp) UpgradeService.get(sp.level().getServer()).open(sp);
            return InteractionResult.SUCCESS;
        });
    }
}
