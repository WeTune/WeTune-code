package sjtu.ipads.wtune.superopt.fragment.symbolic;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.internal.InterpretationsImpl;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.List;

public interface Interpretations extends MultiVersion {
  InputInterpretation getInput(Placeholder placeholder);

  AttributeInterpretation getAttributes(Placeholder placeholder);

  PredicateInterpretation getPredicate(Placeholder placeholder);

  boolean assignAttributes(Placeholder placeholder, List<OutputAttribute> projection);

  boolean assignInput(Placeholder placeholder, PlanNode planNode);

  boolean assignPredicate(Placeholder placeholder, ASTNode expr);

  static Interpretations build(Constraints constraints) {
    return InterpretationsImpl.build(constraints);
  }
}
