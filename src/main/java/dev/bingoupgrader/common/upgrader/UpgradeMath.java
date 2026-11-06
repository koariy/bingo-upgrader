package dev.bingoupgrader.common.upgrader;
import dev.bingoupgrader.common.config.UpgraderConfig;

public final class UpgradeMath {
    private UpgradeMath() { }
    public record Odds(double chance, boolean available, boolean veryLow) { }
    public static Odds calculate(double input, double output, int failures, UpgraderConfig cfg) {
        if (!Double.isFinite(input) || !Double.isFinite(output) || input <= 0 || output <= input || output > cfg.maxTargetValue)
            return new Odds(0,false,false);
        double raw = input / output * (1-cfg.houseEdge);
        // Reject instead of rounding UP to minChance: that would create positive EV exploits.
        if (raw < cfg.minChance) return new Odds(raw,false,raw < .01);
        double chance = Math.min(raw,cfg.maxChance);
        if (cfg.pityEnabled && failures >= cfg.pityAfterFailures) {
            double bonus = Math.min(cfg.pityMaxBonus, ((long) failures-cfg.pityAfterFailures+1)*cfg.pityStep);
            // Pity may spend half the house margin, but never creates a fair/positive EV bet.
            chance = Math.min(Math.min(chance+bonus, input/output*(1-cfg.houseEdge/2)),cfg.maxChance);
        }
        return new Odds(chance,true,chance < .01);
    }
    /** Keep floating-point rounding strictly inside the chosen result interval. */
    public static double landing(boolean won,double chance,double sample) {
        if(!Double.isFinite(chance)||chance<=0||chance>=1||!Double.isFinite(sample)||sample<0||sample>=1)throw new IllegalArgumentException("Landing inputs");
        return won?Math.min(Math.nextDown(chance),sample*chance):Math.min(Math.nextDown(1.0),chance+sample*(1-chance));
    }
    public static boolean wins(double randomUnit, double chance) {
        if (randomUnit < 0 || randomUnit >= 1 || !Double.isFinite(randomUnit)) throw new IllegalArgumentException("Invalid RNG sample");
        return randomUnit < chance;
    }
}
