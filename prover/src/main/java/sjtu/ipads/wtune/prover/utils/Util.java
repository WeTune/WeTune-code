package sjtu.ipads.wtune.prover.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

public class Util {
  public static Tuple[] subst(Tuple[] args, Tuple t, Tuple rep) {
    final Tuple[] newArgs = Arrays.copyOf(args, args.length);
    boolean changed = false;
    for (int i = 0; i < args.length; i++) {
      newArgs[i] = args[i].subst(t, rep);
      changed |= newArgs[i] != args[i];
    }
    if (!changed) return args;
    else return newArgs;
  }

  public static String interpolateToString(String template, Tuple[] tuples) {
    final StringBuilder builder = new StringBuilder(template.length() + 16);

    int start = 0;
    for (Tuple arg : tuples) {
      final int end = template.indexOf("?", start);
      builder.append(template, start, end - 1);
      builder.append(arg);
      start = end + 2;
    }
    builder.append(template, start, template.length());

    return builder.toString();
  }

  public static <T> List<T> arrange(List<T> ts, int[] arrangement) {
    final List<T> matching = new ArrayList<>(arrangement.length);
    for (int i : arrangement) matching.add(ts.get(i));
    return matching;
  }

  public static boolean validateTableTerm(TableTerm table) {
    final Tuple vx = table.tuple();
    final Tuple[] rx = vx.base();
    return rx != null && rx.length == 1 && rx[0].base() == null && rx[0].root() != null;
  }

  public static Map<String, List<TableTerm>> groupTables(Conjunction c) {
    return c.tables().stream()
        .map(it -> (TableTerm) it)
        .collect(Collectors.groupingBy(it -> it.name().toString()));
  }

  public static String ownerTableOf(Constraint constraint) {
    return constraint.columns().get(0).tableName();
  }
}
