package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.constraint.ConstraintSet;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Sets.union;

public class MergedInterpretation implements Interpretation {
  private final Interpretation left;
  private final Interpretation right;

  private MergedInterpretation(Interpretation left, Interpretation right) {
    this.left = left;
    this.right = right;
  }

  public static MergedInterpretation create(Interpretation left, Interpretation right) {
    return new MergedInterpretation(left, right);
  }

  @Override
  public <T> T interpret(Abstraction<T> abs) {
    final T leftAssign = left.interpret(abs);
    if (leftAssign != null) return leftAssign;
    else return right.interpret(abs);
  }

  @Override
  public boolean assign(Abstraction<?> abs, Object interpretation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean assignEq(ConstraintSet constraints, boolean alsoCheck) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Interpretation assignNew(Abstraction<?> abs, Object interpretation) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Abstraction<?>> abstractions() {
    return union(left.abstractions(), right.abstractions());
  }

  @Override
  public Collection<Object> assignments() {
    throw new UnsupportedOperationException();
  }
}
