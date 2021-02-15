package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Interpretation;
import sjtu.ipads.wtune.superopt.fragment.symbolic.PredicateInterpretation;

public class PredicateInterpretationImpl extends InterpretationBase<ASTNode>
    implements PredicateInterpretation {
  public PredicateInterpretationImpl(ASTNode object) {
    super(object);
  }

  @Override
  public boolean isCompatible(Interpretation<ASTNode> other) {
    return this.object().equals(other.object());
  }
}
