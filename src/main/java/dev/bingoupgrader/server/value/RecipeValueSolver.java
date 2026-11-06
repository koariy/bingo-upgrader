package dev.bingoupgrader.server.value;

import java.util.*;

/** Deterministic bounded relaxation: unknown fallbacks never contaminate recipe inputs. */
public final class RecipeValueSolver {
    private RecipeValueSolver() { }
    public record Rule(String output, int count, List<List<String>> ingredients, String recipe) { }
    public static Map<String, Double> solve(Map<String, Double> fixed, Set<String> blocked,
                                           List<Rule> rules, double markup) {
        Map<String, Double> values = new HashMap<>(fixed);
        blocked.forEach(values::remove);
        // Snapshot each pass so recipe ordering cannot change the result.
        for (int pass = 0; pass < Math.min(512, rules.size() + 1); pass++) {
            Map<String, Double> next = new HashMap<>(values);
            for (Rule r : rules) {
                if (fixed.containsKey(r.output()) || blocked.contains(r.output()) || r.count() <= 0 || r.ingredients().isEmpty()) continue;
                double sum = 0;
                for (List<String> alternatives : r.ingredients()) {
                    double min = alternatives.stream().filter(id -> !blocked.contains(id))
                        .mapToDouble(id -> values.getOrDefault(id, Double.POSITIVE_INFINITY)).min().orElse(Double.POSITIVE_INFINITY);
                    sum += min;
                }
                double candidate = sum / r.count() * markup;
                if (Double.isFinite(candidate) && candidate > 0 && candidate <= 1e12)
                    next.merge(r.output(), candidate, Math::min);
            }
            if (next.equals(values)) break;
            values.clear(); values.putAll(next);
        }
        return Map.copyOf(values);
    }
}
