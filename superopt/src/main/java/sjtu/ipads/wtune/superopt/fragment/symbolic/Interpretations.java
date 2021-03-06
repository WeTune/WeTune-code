package sjtu.ipads.wtune.superopt.fragment.symbolic;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.internal.InterpretationsImpl;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.List;

public interface Interpretations extends MultiVersion {
  Constraints constraints();

  InputInterpretation getInput(Placeholder placeholder);

  AttributeInterpretation getAttributes(Placeholder placeholder);

  PredicateInterpretation getPredicate(Placeholder placeholder);

  boolean assignAttributes(Placeholder placeholder, List<AttributeDef> defs);

  boolean assignInput(Placeholder placeholder, PlanNode planNode);

  boolean assignPredicate(Placeholder placeholder, ASTNode expr);

  boolean hasAssignment(Placeholder placeholder);

  default PlanNode interpretInput(Placeholder placeholder) {
    final InputInterpretation inter = getInput(placeholder);
    return inter == null ? null : inter.object();
  }

  default List<AttributeDef> interpretAttributes(Placeholder placeholder) {
    final AttributeInterpretation inter = getAttributes(placeholder);
    return inter == null ? null : inter.object();
  }

  default ASTNode interpretPredicate(Placeholder placeholder) {
    final PredicateInterpretation inter = getPredicate(placeholder);
    return inter == null ? null : inter.object();
  }

  static Interpretations constrainedBy(Constraints constraints) {
    return InterpretationsImpl.build(constraints);
  }
}
