package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.superopt.substitution.Substitution;

public record OptimizationStep(String original, Substitution substitution) {
    public int substitutionId() {
        return substitution == null ? -1 : substitution.id();
    }
}
