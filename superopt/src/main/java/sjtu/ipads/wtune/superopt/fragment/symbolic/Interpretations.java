package sjtu.ipads.wtune.superopt.fragment.symbolic;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.internal.InterpretationsImpl;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.List;

public interface Interpretations extends MultiVersion {
  Constraints constraints();

  InputInterpretation getInput(Placeholder placeholder);

  AttributeInterpretation getAttributes(Placeholder placeholder);

  PredicateInterpretation getPredicate(Placeholder placeholder);

  boolean assignAttributes(
      Placeholder placeholder, List<PlanAttribute> in, List<PlanAttribute> out);

  boolean assignInput(Placeholder placeholder, PlanNode planNode);

  boolean assignPredicate(Placeholder placeholder, ASTNode expr);

  default boolean assignAttributes(Placeholder placeholder, List<PlanAttribute> in) {
    return assignAttributes(placeholder, in, null);
  }

  static Interpretations constrainedBy(Constraints constraints) {
    return InterpretationsImpl.build(constraints);
  }
}
