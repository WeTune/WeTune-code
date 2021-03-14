package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;

import java.util.List;
import java.util.Set;

public record Uniqueness(List<Set<AttributeDef>> cores, boolean isSingleton) {}
