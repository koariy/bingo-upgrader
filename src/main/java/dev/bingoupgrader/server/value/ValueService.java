package dev.bingoupgrader.server.value;

import dev.bingoupgrader.common.config.ValueConfig;
import dev.bingoupgrader.common.value.*;
import dev.bingoupgrader.common.BingoUpgraderMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import java.util.*;
import java.io.IOException;
import java.nio.file.Path;

/** One instance per running server. Mutating methods must run on its server thread. */
public final class ValueService {
    private static final Map<MinecraftServer, ValueService> SERVICES = new IdentityHashMap<>();
    private final MinecraftServer server;
    private volatile ValueTable table;
    private ValueConfig config;
    private long revision;
    private ValueService(MinecraftServer server) { this.server = server; }
    public static ValueService get(MinecraftServer server) {
        var service = SERVICES.get(server);
        if (service == null) throw new IllegalStateException("Values not initialized");
        return service;
    }
    public static void start(MinecraftServer server) {
        var service = new ValueService(server);
        try { service.reload(); }
        catch (IOException e) {
            BingoUpgraderMod.LOGGER.error("Invalid values config; using defaults without overwriting file", e);
            service.config = new ValueConfig(); service.rebuild();
        }
        SERVICES.put(server, service);
    }
    public static void stop(MinecraftServer server) { SERVICES.remove(server); }
    public ValueTable table() { return table; }
    public StackValues.Quote quote(ItemStack stack) { return StackValues.quote(table, stack); }
    /** Total stack value; always check quote(stack).allowed() before accepting an upgrade input. */
    public double getValue(ItemStack stack) { return quote(stack).totalValue(); }
    public boolean isBlacklisted(ItemStack stack) {
        var e = table.entries().get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return e != null && e.blocked();
    }
    public void reload() throws IOException {
        ValueConfig candidate = ValueConfigStore.load();
        ValueTable replacement = build(candidate); // No mutation until the entire rebuild succeeds.
        config = candidate; table = replacement; revision = replacement.revision();
    }
    public void rebuild() { table = build(config); revision = table.revision(); }
    private ValueTable build(ValueConfig cfg) {
        Map<String, Double> fixed = new HashMap<>();
        Map<String, String> sources = new HashMap<>();
        Map<String, Double> tags = new HashMap<>();
        for (var item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            Double tagValue = null;
            for (var entry : cfg.tagValues.entrySet()) {
                if (item.builtInRegistryHolder().is(TagKey.create(Registries.ITEM, Identifier.parse(entry.getKey()))))
                    tagValue = tagValue == null ? entry.getValue() : Math.min(tagValue, entry.getValue());
            }
            if (tagValue != null) tags.put(id, tagValue);
            if (cfg.baseValues.containsKey(id)) { fixed.put(id, cfg.baseValues.get(id)); sources.put(id, "base"); }
            if (cfg.overrides.containsKey(id)) { fixed.put(id, cfg.overrides.get(id)); sources.put(id, "override"); }
        }
        var rules = new ArrayList<RecipeValueSolver.Rule>();
        var context = SlotDisplayContext.fromLevel(server.overworld());
        for (var holder : server.getRecipeManager().getRecipes()) {
            var r = holder.value();
            // Special/component-copying recipes cannot safely be reduced to a base item price.
            if (!(r instanceof ShapedRecipe || r instanceof ShapelessRecipe || r instanceof SingleItemRecipe || r instanceof SmithingTransformRecipe)) continue;
            try {
                if (r.isSpecial()) continue;
                List<Ingredient> ingredients = r instanceof ShapedRecipe shaped
                    ? shaped.getIngredients().stream().flatMap(Optional::stream).toList()
                    : r.placementInfo().ingredients();
                List<List<String>> inputs = ingredients.stream().map(i -> i.items()
                    .map(h -> BuiltInRegistries.ITEM.getKey(h.value()).toString()).sorted().toList()).toList();
                for (var display : r.display()) {
                    for (ItemStack result : display.result().resolveForStacks(context)) {
                        if (result.isEmpty() || !result.getComponentsPatch().isEmpty()) continue;
                        rules.add(new RecipeValueSolver.Rule(BuiltInRegistries.ITEM.getKey(result.getItem()).toString(),
                            result.getCount(), inputs, holder.id().toString()));
                    }
                }
            } catch (RuntimeException ex) { BingoUpgraderMod.LOGGER.warn("Cannot price recipe {}", holder.id(), ex); }
        }
        Set<String> blocked = new HashSet<>(cfg.blacklist); blocked.add("minecraft:air");
        Set<String> outputs = new HashSet<>(); rules.forEach(r -> outputs.add(r.output()));
        tags.forEach((id, value) -> {
            if (!fixed.containsKey(id) && !outputs.contains(id)) { fixed.put(id,value); sources.put(id,"tag"); }
        });
        var firstPass = RecipeValueSolver.solve(fixed, blocked, rules, cfg.craftingMarkup);
        tags.forEach((id, value) -> {
            if (!fixed.containsKey(id) && !firstPass.containsKey(id) && !blocked.contains(id)) {
                fixed.put(id,value); sources.put(id,"tag");
            }
        });
        var values = RecipeValueSolver.solve(fixed, blocked, rules, cfg.craftingMarkup);
        Map<String, ValueTable.Entry> entries = new TreeMap<>();
        for (var item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            boolean banned = blocked.contains(id);
            entries.put(id, new ValueTable.Entry(banned ? 0 : values.getOrDefault(id, cfg.unknownValue),
                banned ? "blacklist" : sources.getOrDefault(id, values.containsKey(id) ? "recipe" : "unknown"), banned));
        }
        return new ValueTable(revision + 1, entries, new ValueTable.Modifiers(cfg.enchantmentPerLevel,
            cfg.potionPerMinute, cfg.instantEffectValue, cfg.minimumDurabilityFactor));
    }
    public Path dump() throws IOException {
        Path target = ValueConfigStore.DIRECTORY.resolve("values-dump.json");
        ValueConfigStore.writeAtomic(target, ValueConfigStore.JSON.toJson(table)); return target;
    }
}
