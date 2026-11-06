package dev.bingoupgrader.common.value;

import java.util.*;

/** Immutable, atomic snapshot shared over the wire. Values are per one item. */
public record ValueTable(long revision, Map<String, Entry> entries, Modifiers modifiers) {
    public ValueTable { entries = Collections.unmodifiableMap(new TreeMap<>(entries)); }
    public record Entry(double value, String source, boolean blocked) {
        public Entry {
            if (!Double.isFinite(value) || value < 0 || value > 1e12) throw new IllegalArgumentException("Invalid table value");
            Objects.requireNonNull(source);
        }
    }
    public record Modifiers(double enchantmentPerLevel, double potionPerMinute,
                            double instantEffectValue, double minimumDurabilityFactor) {
        public Modifiers {
            for (double v : new double[]{enchantmentPerLevel,potionPerMinute,instantEffectValue})
                if (!Double.isFinite(v) || v < 0 || v > 1e6) throw new IllegalArgumentException("Invalid modifier");
            if (!Double.isFinite(minimumDurabilityFactor) || minimumDurabilityFactor < 0 || minimumDurabilityFactor > 1)
                throw new IllegalArgumentException("Invalid durability factor");
        }
    }
}
