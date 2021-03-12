package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;

import java.util.List;
import java.util.Set;

public record UniquenessCore(Set<Set<AttributeDef>> attrs, boolean isSingleton) {}
