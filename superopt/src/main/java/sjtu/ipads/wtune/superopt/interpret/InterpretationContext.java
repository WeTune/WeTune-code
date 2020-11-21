package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.interpret.impl.InterpretationContextImpl;

import java.util.List;

public interface InterpretationContext extends Interpretation, Iterable<Interpretation> {
  void addInterpretation(Interpretation interpretation);

  int count();

  void forward();

  List<Interpretation> interpretations();

  InterpretationContext merge(InterpretationContext other);

  static InterpretationContext empty() {
    return InterpretationContextImpl.empty();
  }

  static InterpretationContext from(List<Interpretation> interpretations) {
    return InterpretationContextImpl.from(interpretations);
  }
}
