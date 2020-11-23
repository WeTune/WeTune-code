package sjtu.ipads.wtune.superopt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.superopt.Helper.cartesianProductStream;
import static sjtu.ipads.wtune.superopt.Helper.pack;

public class Main {
  private static final System.Logger LOG = System.getLogger("Enumerator");

  private static final String LOGGER_CONFIG =
      "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  public static void main(String[] args) {
    final Set<Graph> skeletons = Enumerator.enumSkeleton();
    LOG.log(System.Logger.Level.INFO, "#skeletons = {0}", skeletons.size());

    final List<Substitution> substitutions =
        cartesianProductStream(skeletons, skeletons)
            .map(pack(Enumerator::enumSubstitution))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    LOG.log(System.Logger.Level.INFO, "#constraints = {0}", substitutions.size());
  }

  //    final Map<OutputEstimation, List<Graph>> collect =
  //        skeletons.stream().collect(Collectors.groupingBy(OutputEstimator::estimateOutput));
  //    long total = 0;
  //    for (var pair : OutputEstimation.pairsMayMatch()) {
  //      final List<Graph> left = collect.get(pair.getLeft());
  //      final List<Graph> right = collect.get(pair.getRight());
  //      if (left == null || right == null) continue;
  //      for (Graph s : left) {
  //        for (Graph t : right) {
  //          if (s == t) continue;
  //          InterpretationContext ctx = InputMappingInterpreter.interpret(s, t);
  //          total += ctx.count();
  //        }
  //      }
  //    }
  //    skeletons.forEach(InputInterpreter::interpret);

  //    long total = 0;
  //    for (int i = 0; i < skeletons.size(); i++) {
  //      for (int j = 0; j < skeletons.size(); j++) {
  //        if (i == j) continue;
  //
  //        final Graph g0 = skeletons.get(i);
  //        final Graph g1 = skeletons.get(j);
  //
  //        InterpretationContext ctx = InputMappingInterpreter.interpret(g0, g1);
  //
  //        total += ctx.count();
  //      }
  //    }

  //    System.out.println(total);
}
