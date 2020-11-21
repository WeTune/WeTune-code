package sjtu.ipads.wtune.superopt.interpret.impl;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;

import java.util.*;

public class InterpretationContextImpl implements InterpretationContext {
  private final List<Interpretation> interpretations;
  private int currentIdx = 0;

  private InterpretationContextImpl(List<Interpretation> interpretations) {
    this.interpretations = interpretations;
  }

  public static InterpretationContextImpl empty() {
    return new InterpretationContextImpl(new ArrayList<>());
  }

  public static InterpretationContextImpl from(List<Interpretation> interpretations) {
    return new InterpretationContextImpl(interpretations);
  }

  @Override
  public <T> T interpret(Abstraction<T> abs) {
    return current().interpret(abs);
  }

  @Override
  public boolean assign(Abstraction<?> abs, Object interpretation) {
    return current().assign(abs, interpretation);
  }

  @Override
  public Set<Abstraction<?>> abstractions() {
    return current().abstractions();
  }

  private Interpretation current() {
    return interpretations.get(currentIdx);
  }

  @Override
  public Set<Constraint> constraints() {
    return null;
  }

  @Override
  public Interpretation merge(Interpretation other) {
    if (other instanceof InterpretationContext) return merge(((InterpretationContext) other));
    else return merge(from(Collections.singletonList(other)));
  }

  @Override
  public void addConstraint(Constraint constraint) {
    current().addConstraint(constraint);
  }

  @Override
  public void clearAssignment() {
    current().clearAssignment();
  }

  @Override
  public int count() {
    return interpretations.size();
  }

  @Override
  public void addInterpretation(Interpretation interpretation) {
    interpretations.add(interpretation);
  }

  @Override
  public void forward() {
    currentIdx++;
  }

  @Override
  public List<Interpretation> interpretations() {
    return interpretations;
  }

  @Override
  public InterpretationContext merge(InterpretationContext other) {
    if (interpretations.isEmpty()) return from(new ArrayList<>(other.interpretations()));

    final List<Interpretation> list = new ArrayList<>(count() * other.count());

    for (List<Interpretation> pair :
        Lists.cartesianProduct(interpretations(), other.interpretations())) {
      final Interpretation left = pair.get(0);
      final Interpretation right = pair.get(1);

      final Interpretation newInterpret = left.merge(right);
      if (newInterpret == null) return null;

      list.add(newInterpret);
    }

    return from(list);
  }

  @Override
  public Iterator<Interpretation> iterator() {
    return interpretations.iterator();
  }
}
