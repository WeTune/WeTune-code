package sjtu.ipads.wtune.superopt.optimizer.support;

import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;

public record Uniqueness(List<Set<AttributeDef>> cores, boolean isSingleton) {}
