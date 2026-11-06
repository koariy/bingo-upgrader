package dev.bingoupgrader.common.config;

import java.util.*;

/** JSON schema. Missing fields retain defaults. Unknown IDs are retained for absent mods. */
public final class ValueConfig {
    public int schemaVersion = 1;
    public double unknownValue = 1;
    public double craftingMarkup = 1.05;
    public double enchantmentPerLevel = 20;
    public double potionPerMinute = 5;
    public double instantEffectValue = 30;
    public double minimumDurabilityFactor = 0.05;
    public Map<String, Double> baseValues = defaults();
    public Map<String, Double> overrides = new TreeMap<>();
    public Map<String, Double> tagValues = new TreeMap<>(Map.ofEntries(
        Map.entry("minecraft:logs", 2.0), Map.entry("minecraft:planks", 0.525),
        Map.entry("c:ingots/iron", 15.0), Map.entry("c:ingots/gold", 25.0),
        Map.entry("c:gems/diamond", 250.0), Map.entry("c:cobblestones", 0.5)));
    public Set<String> blacklist = new TreeSet<>(Set.of("minecraft:dragon_egg"));

    private static Map<String, Double> defaults() {
        Map<String, Double> m = new TreeMap<>();
        String[] ids = {"stick","cobblestone","iron_ingot","gold_ingot","redstone","diamond",
            "netherite_ingot","elytra","totem_of_undying","nether_star","coal","charcoal",
            "raw_iron","raw_gold","raw_copper","copper_ingot","lapis_lazuli","emerald",
            "quartz","ancient_debris","netherite_scrap","blaze_rod","ender_pearl",
            "end_stone","netherrack","soul_sand","glowstone_dust","obsidian","sand",
            "gravel","dirt","stone","flint","clay_ball","leather","string","bone",
            "gunpowder","slime_ball","wheat","carrot","potato","sugar_cane","bamboo",
            "kelp","feather","egg","apple","beef","porkchop","chicken","rabbit",
            "mutton","cod","salmon","honeycomb","amethyst_shard","prismarine_shard",
            "prismarine_crystals","sponge","shulker_shell","chorus_fruit","ghast_tear",
            "magma_cream","nether_wart","brown_mushroom","red_mushroom","sweet_berries"};
        double[] values = {1,.5,15,25,5,250,1500,5000,3000,8000,3,3,12,20,3,4,8,100,
            10,300,350,35,40,3,.5,4,8,20,.5,.5,.25,.5,1,1,8,3,2,8,15,1,1,1,1,.5,
            .5,2,2,2,3,3,3,4,3,3,3,5,15,15,18,150,150,8,80,20,5,1,1,1};
        for (int i=0;i<ids.length;i++) m.put("minecraft:"+ids[i], values[i]);
        return m;
    }

    public void validate() {
        if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported values schema");
        positive(unknownValue); positive(craftingMarkup);
        if (craftingMarkup < 1 || craftingMarkup > 10) throw new IllegalArgumentException("craftingMarkup must be 1..10");
        nonnegative(enchantmentPerLevel); nonnegative(potionPerMinute); nonnegative(instantEffectValue);
        if (!Double.isFinite(minimumDurabilityFactor) || minimumDurabilityFactor < 0 || minimumDurabilityFactor > 1)
            throw new IllegalArgumentException("minimumDurabilityFactor must be 0..1");
        for (var map : List.of(baseValues, overrides, tagValues)) {
            if (map == null) throw new IllegalArgumentException("Null value map");
            map.forEach((id, value) -> { validId(id); if (value == null) throw new IllegalArgumentException("Null value"); positive(value); });
        }
        if (blacklist == null) throw new IllegalArgumentException("Null blacklist");
        blacklist.forEach(ValueConfig::validId);
    }
    private static void validId(String id) {
        if (id == null || !id.contains(":" ) || net.minecraft.resources.Identifier.tryParse(id) == null)
            throw new IllegalArgumentException("Invalid namespaced ID: " + id);
    }
    private static void positive(double v) { if (!Double.isFinite(v) || v <= 0 || v > 1e12) throw new IllegalArgumentException("Value must be finite, >0 and <=1e12"); }
    private static void nonnegative(double v) { if (!Double.isFinite(v) || v < 0 || v > 1e6) throw new IllegalArgumentException("Invalid coefficient"); }
}
