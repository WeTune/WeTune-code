package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.superopt.optimization.Operator;
import sjtu.ipads.wtune.superopt.plan.Placeholder;

import java.util.List;

public interface Interpretations extends MultiVersion {
  InputInterpretation interpretInput(Placeholder placeholder);

  ProjectionInterpretation interpretPick(Placeholder placeholder);

  PredicateInterpretation interpretPredicate(Placeholder placeholder);

  boolean assignProjection(Placeholder placeholder, List<Attribute> projection);

  boolean assignInput(Placeholder placeholder, Operator operator);
}
