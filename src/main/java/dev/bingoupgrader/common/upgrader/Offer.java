package dev.bingoupgrader.common.upgrader;
/** Main inventory index (0..35), amount to consume. Armor/offhand excluded. */
public record Offer(int slot, int count) {
    public Offer { if (slot < 0 || slot >= 36 || count < 1 || count > 99) throw new IllegalArgumentException("Invalid offer"); }
}
