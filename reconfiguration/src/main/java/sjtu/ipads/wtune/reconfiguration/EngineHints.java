package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.func;

public class EngineHints {
  private Map<Table, String> hints = new HashMap<>();

  public String hintOf(Table table) {
    return hints.get(table);
  }

  public Map<Table, String> hints() {
    return hints;
  }

  public static EngineHints hint(ColumnMatching matching) {
    if (matching.matchingCount() == 0) return null;

    final Map<Table, Map<Table, Long>> tableMatch = matchTable(matching.slow(), matching);

    final EngineHints hints = new EngineHints();
    final Map<Table, String> map = hints.hints;

    for (var slowToFast : tableMatch.entrySet()) {
      final Map<Table, Long> matchedFast = slowToFast.getValue();
      if (matchedFast.isEmpty()) continue;

      final long max = matchedFast.values().stream().mapToLong(Long::longValue).max().getAsLong();

      final Table slowTable = slowToFast.getKey();
      final String slowEngine = slowTable.engine();

      for (var fastToCount : matchedFast.entrySet()) {
        final Table fastTable = fastToCount.getKey();
        final String fastEngine = fastTable.engine();
        if (fastToCount.getValue() == max && !slowTable.engine().equals(fastEngine))
          map.put(slowTable, fastEngine);
      }
    }

    return hints;
  }

  private static Map<Table, Map<Table, Long>> matchTable(
      Set<Column> columns, ColumnMatching match) {
    return columns.stream()
        .collect(
            groupingBy(
                Column::table,
                flatMapping(
                    func(match::matchOf).andThen(Collection::stream),
                    groupingBy(Column::table, counting()))));
  }
}
