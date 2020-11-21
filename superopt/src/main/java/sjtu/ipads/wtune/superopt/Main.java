package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.enumerator.SkeletonEnumerator;
import sjtu.ipads.wtune.superopt.interpret.InterpretationContext;
import sjtu.ipads.wtune.superopt.interpret.impl.InputInterpreter;
import sjtu.ipads.wtune.superopt.interpret.impl.InputMappingInterpreter;

import java.util.*;

public class Main {
  public static void enumerate() {
    final List<Graph> skeletons = new ArrayList<>(SkeletonEnumerator.enumerate());

    skeletons.forEach(InputInterpreter::interpret);

    long total = 0;
    for (int i = 0; i < skeletons.size(); i++) {
      for (int j = i + 1; j < skeletons.size(); j++) {
        final Graph g0 = skeletons.get(i);
        final Graph g1 = skeletons.get(j);

        InterpretationContext ctx = InputMappingInterpreter.interpret(g0, g1);
        if (ctx == null) ctx = InputMappingInterpreter.interpret(g1, g0);

        total += ctx.count();
      }
    }

    System.out.println(total);
  }

  public static void main(String[] args) {
    Main.enumerate();
  }
}
