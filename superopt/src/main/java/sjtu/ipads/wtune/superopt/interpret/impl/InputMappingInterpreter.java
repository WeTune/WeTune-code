package sjtu.ipads.wtune.superopt.interpret.impl;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.operators.Input;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import static java.util.Collections.singleton;

public class InputMappingInterpreter {
  private static void doInterpret(
      int idx,
      List<Input> tInputs,
      Set<Input> sInputs,
      Stack<Constraint> constraints,
      InterpretationContext ctx) {
    if (idx >= sInputs.size()) {
      final Interpretation interpretation = Interpretation.create();
      constraints.forEach(interpretation::addConstraint);
      ctx.addInterpretation(interpretation);
      return;
    }
    final Input tInput = tInputs.get(idx);
    for (Input sInput : sInputs) {
      constraints.push(Constraint.refEq(tInput.source(), sInput.source()));

      final Set<Input> sInputsNext =
          sInput.canBeTable() ? sInputs : Sets.difference(sInputs, singleton(sInput));
      doInterpret(idx + 1, tInputs, sInputsNext, constraints, ctx);

      constraints.pop();
    }
  }
}
