package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level;
import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Reconfiguration {
  public static final System.Logger LOG = System.getLogger("Reconfigure.Core");

  public static ReconfigurationOutput reconfigure(String appName, List<Statement> stmts) {
    appName = coalesce(appName, "anonymous");
    LOG.log(Level.INFO, "start {0}", appName);

    final ReconfigurationContext ctx = ReconfigurationContext.build();
    final ReconfigurationOutput output = ctx.output();
    output.appName = appName;

    for (Statement stmt : stmts) ctx.startOne(stmt);

    final Set<Index> indexHints = output.indexHints;
    final Map<Table, String> engineHints = output.engineHints;

    logNumHints("0", indexHints.size(), engineHints.size());

    ctx.reduceExisting();
    logNumHints("1", indexHints.size(), engineHints.size());

    ctx.reduceLowSelectivity();
    logNumHints("2", indexHints.size(), engineHints.size());

    ctx.reduceLowUsage(stmts);
    logNumHints("3", indexHints.size(), engineHints.size());

    ctx.reduceEngine();
    logNumHints("4", indexHints.size(), engineHints.size());

    final List<Index> sorted =
        indexHints.stream()
            .sorted(Comparator.comparing(it -> it.table().tableName()))
            .collect(Collectors.toList());

    LOG.log(Level.INFO, "index:\n  {0}", String.join("\n  ", listMap(Index::toString, sorted)));
    LOG.log(
        Level.INFO,
        "engine:\n  {0}",
        String.join(
            "\n  ", listMap(Reconfiguration::stringifyEngineChange, engineHints.entrySet())));

    return output;
  }

  private static void logNumHints(String tag, int indexHintSize, int engineHintSize) {
    LOG.log(Level.INFO, "{0}. #index: {1}, #engine: {2}", tag, indexHintSize, engineHintSize);
  }

  private static String stringifyEngineChange(Map.Entry<Table, String> pair) {
    return String.format("%s -> %s", pair.getKey(), pair.getValue());
  }
}
