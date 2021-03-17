package sjtu.ipads.wtune.superopt.fragment.symbolic;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.internal.InterpretationsImpl;
import sjtu.ipads.wtune.superopt.util.Constraints;

public interface Interpretations {
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

  static Interpretations derivedInterpretations(Interpretations base) {
    return InterpretationsImpl.build(base);
  }

  static Interpretations constrainedInterpretations(Constraints constraints) {
    return InterpretationsImpl.build(constraints);
  }
}
