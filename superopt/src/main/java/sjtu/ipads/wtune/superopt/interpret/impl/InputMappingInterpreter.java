package sjtu.ipads.wtune.superopt.interpret.impl;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Relation;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.operators.Input;

import java.util.List;

import static java.util.Collections.nCopies;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class InputMappingInterpreter {
  public static InterpretationContext interpret(Graph s, Graph t) {
    if (s.inputs().size() < t.inputs().size()) return null;

    final List<Abstraction<Relation>> sInput = listMap(Input::relation, s.inputs());
    final List<Abstraction<Relation>> tInput = listMap(Input::relation, t.inputs());

    InterpretationContext ret = InterpretationContext.empty();
    for (var assignment : Lists.cartesianProduct(nCopies(tInput.size(), sInput))) {
      final Interpretation interpretation = Interpretation.create();
      ret.addInterpretation(interpretation);

      for (int i = 0; i < tInput.size(); i++)
        interpretation.addConstraint(Constraint.eq(tInput.get(i), assignment.get(i)));
    }

    ret = ret.merge(s.interpretations()).merge(t.interpretations());

    return ret;
  }
}
