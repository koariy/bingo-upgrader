package dev.bingoupgrader.common.config;

import java.util.*;

/** Probabilities use fractions, not percentages: .0005 = .05%. */
public final class UpgraderConfig {
    public double houseEdge = .10, minChance = .0005, maxChance = .90;
    public double maxTargetValue = 100000;
    public int attemptsPerMinute = 30, requestsPerSecond = 10, sessionSeconds = 300, previewSeconds = 15;
    public boolean pityEnabled = false;
    public boolean allowUnknownTargets = false;
    public int pityAfterFailures = 10;
    public double pityStep = .002, pityMaxBonus = .05;
    public Set<String> blacklist = new TreeSet<>(Set.of("minecraft:dragon_egg", "minecraft:bedrock",
        "minecraft:barrier", "minecraft:command_block", "minecraft:chain_command_block",
        "minecraft:repeating_command_block", "minecraft:structure_block", "minecraft:structure_void",
        "minecraft:jigsaw", "minecraft:debug_stick", "minecraft:spawner", "minecraft:trial_spawner",
        "minecraft:light", "minecraft:knowledge_book"));
    public Set<String> blacklistTags = new TreeSet<>(Set.of("minecraft:spawn_eggs"));
    public void validate() {
        if (!Double.isFinite(houseEdge) || houseEdge <= 0 || houseEdge >= 1) bad();
        if (!Double.isFinite(minChance) || !Double.isFinite(maxChance) || minChance <= 0 || maxChance > 1 || minChance > maxChance) bad();
        if (!Double.isFinite(maxTargetValue) || maxTargetValue <= 0 || maxTargetValue > 1e12) bad();
        if (attemptsPerMinute < 1 || attemptsPerMinute > 600 || requestsPerSecond < 1 || requestsPerSecond > 100 || sessionSeconds < 5 || sessionSeconds > 3600 || previewSeconds < 1 || previewSeconds > 60 || pityAfterFailures < 1) bad();
        if (!Double.isFinite(pityStep) || !Double.isFinite(pityMaxBonus) || pityStep < 0 || pityStep > 1 || pityMaxBonus < 0 || pityMaxBonus > 1) bad();
        if (blacklist == null || blacklistTags == null) bad();
        for (var ids : List.of(blacklist,blacklistTags)) for (String id : ids)
            if (id == null || !id.contains(":" ) || net.minecraft.resources.Identifier.tryParse(id) == null) bad();
    }
    private static void bad() { throw new IllegalArgumentException("Invalid upgrader configuration"); }
}
