package sjtu.ipads.wtune.superopt.interpret.impl;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.superopt.Graph;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.operators.Input;

import java.util.*;

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
      constraints.push(Constraint.refEq(tInput.relation(), sInput.relation()));

      final Set<Input> sInputsNext =
          sInput.canBeTable() ? sInputs : Sets.difference(sInputs, singleton(sInput));
      doInterpret(idx + 1, tInputs, sInputsNext, constraints, ctx);

      constraints.pop();
    }
  }

  public static InterpretationContext interpret(Graph s, Graph t) {
    //    if (s.inputs().size() < t.inputs().size()) return null;
    //    final List<Abstraction<Relation>> sInput = listMap(Input::relation, s.inputs());
    //    final List<Abstraction<Relation>> tInput = listMap(Input::relation, t.inputs());

    InterpretationContext ret = InterpretationContext.empty();
    final Set<Input> sInputs = Sets.newIdentityHashSet();
    sInputs.addAll(t.inputs());
    doInterpret(0, t.inputs(), sInputs, new Stack<>(), ret);
//    System.out.println(ret.count());
    //    for (var assignment : Lists.cartesianProduct(nCopies(tInput.size(), sInput))) {
    //      final Interpretation interpretation = Interpretation.create();
    //      ret.addInterpretation(interpretation);
    //
    //      for (int i = 0; i < tInput.size(); i++)
    //        interpretation.addConstraint(Constraint.eq(tInput.get(i), assignment.get(i)));
    //    }

    //    ret = ret.merge(s.interpretations()).merge(t.interpretations());

    return ret;
  }
}
