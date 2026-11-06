package dev.bingoupgrader.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import dev.bingoupgrader.client.network.ClientValues;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class BingoUpgraderClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        var config = ClientValueConfig.load();
        ClientValues.register();
        dev.bingoupgrader.client.network.ClientBingo.register();
        dev.bingoupgrader.client.network.ClientUpgrader.register();
        ItemTooltipCallback.EVENT.register((stack, context, flags, lines) -> {
            if (!config.showItemValues || !ClientValues.ready() || stack.isEmpty()) return;
            var q = ClientValues.quote(stack);
            if (!q.allowed()) lines.add(Component.translatable("bingoupgrader.tooltip.blocked").withStyle(ChatFormatting.RED));
            else {
                lines.add(Component.translatable("bingoupgrader.tooltip.value", format(q.unitValue()))
                    .withStyle(ChatFormatting.GOLD));
                if (stack.getCount() > 1) lines.add(Component.translatable("bingoupgrader.tooltip.total", format(q.totalValue()))
                    .withStyle(ChatFormatting.GRAY));
                if (q.unknown()) lines.add(Component.translatable("bingoupgrader.value.unknown").withStyle(ChatFormatting.DARK_GRAY));
            }
        });
    }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }
}
