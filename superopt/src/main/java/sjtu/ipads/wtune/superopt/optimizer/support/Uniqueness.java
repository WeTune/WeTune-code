package sjtu.ipads.wtune.superopt.optimizer.support;

import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;

import java.util.List;
import java.util.Set;

public record Uniqueness(List<Set<AttributeDef>> cores, boolean isSingleton) {}
