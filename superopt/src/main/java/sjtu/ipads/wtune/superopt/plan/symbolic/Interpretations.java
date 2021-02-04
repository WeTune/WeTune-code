package sjtu.ipads.wtune.superopt.plan.symbolic;

public interface Interpretations {
  InputInterpretation interpretInput(Placeholder placeholder);

  PickInterpretation interpretPick(Placeholder placeholder);

  PredicateInterpretation interpretPredicate(Placeholder placeholder);
}
