package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.Helper;
import sjtu.ipads.wtune.superopt.Relation;
import sjtu.ipads.wtune.superopt.interpret.Abstraction;
import sjtu.ipads.wtune.superopt.interpret.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.operators.Input;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class InputInterpreter {
  public static void interpret(Graph graph) {
    final List<Abstraction<Relation>> inputs = listMap(Input::relation, graph.inputs());
    final int[][] bits = Helper.setPartition(inputs.size());

    final InterpretationContext interpretations = InterpretationContext.empty();
    for (int[] bit : bits) {
      final Interpretation interpretation = Interpretation.create();
      interpretations.addInterpretation(interpretation);

      for (int i = 0; i < bit.length; i++)
        for (int j = i + 1; j < bit.length; j++)
          if (bit[i] == bit[j])
            interpretation.addConstraint(Constraint.eq(inputs.get(i), inputs.get(j)));
    }

    graph.mergeInterpretations(interpretations);
  }
}
