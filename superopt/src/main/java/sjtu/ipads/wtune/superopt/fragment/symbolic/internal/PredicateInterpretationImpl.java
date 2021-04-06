package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.plan.Expr;
import sjtu.ipads.wtune.superopt.fragment.symbolic.PredicateInterpretation;

public class PredicateInterpretationImpl extends InterpretationBase<Expr>
    implements PredicateInterpretation {
  public PredicateInterpretationImpl(Expr object) {
    super(object);
  }

  @Override
  public boolean isCompatible(Expr obj) {
    return this.object().equals(obj);
  }
}
