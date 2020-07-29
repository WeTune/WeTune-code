package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;
import static sjtu.ipads.wtune.sqlparser.SQLNode.MYSQL;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_BOOLEAN;
import static sjtu.ipads.wtune.stmt.schema.Column.COLUMN_IS_ENUM;

public class ReconfigurationContext {
  private Set<Index> indexHints = new HashSet<>();
  private Map<Table, String> engineHints = new HashMap<>();

  public void startOne(Statement base) {
    base.retrofitStandard();

    final List<Statement> references = collectReferences(base);
    for (Statement ref : references) {
      final ColumnMatching matching = ColumnMatching.build(base, ref);

      final IndexHints indexHint = IndexHints.hint(matching);
      if (indexHint != null)
        for (List<Column> columns : indexHint) indexHints.add(Index.build(columns));

      if (canHintEngine(base, ref)) {
        final EngineHints engineHint = EngineHints.hint(matching);
        if (engineHint != null) engineHints.putAll(engineHint.hints());
      }
    }
  }

  public void reduceLowSelectivity() {
    indexHints.removeIf(ReconfigurationContext::isLowSelectivity);
  }

  public void reduceLowUsage(List<Statement> stmts) {
    final Map<Table, List<Index>> indexByTable =
        indexHints.stream().collect(Collectors.groupingBy(Index::table));
    final Set<Index> toReduce = new HashSet<>();

    for (List<Index> indexes : indexByTable.values())
      for (int i = 0; i < indexes.size() - 1; i++)
        for (int j = 1; j < indexes.size(); j++) {
          final Index index0 = indexes.get(i);
          final Index index1 = indexes.get(j);
          if (toReduce.contains(index0) || toReduce.contains(index1)) continue;

          // quick check
          if (!index0.column(0).equals(index1.column(0))) continue;
          // -2: init, -1: less, 0: equal, 1: greater, 2: other
          int relation = -2;

          for (Statement stmt : stmts) {
            final List<Column> usage0 = index0.usage(stmt);
            final List<Column> usage1 = index1.usage(stmt);

            final int compare = compareUsage(usage0, usage1);
            if (relation == -2) relation = compare;
            else if (relation == -1 && compare == 1) relation = 2;
            else if (relation == 1 && compare == -1) relation = 2;
            else if (compare == 2) relation = 2;

            if (relation == 2) break;
          }

          assert relation != -2;

          if (relation == 2) continue;
          if (relation == -1) toReduce.add(index0);
          else if (relation == 1) toReduce.add(index1);
          else {
            // prefer shorter
            if (index0.size() < index1.size()) toReduce.add(index1);
            else toReduce.add(index0);
          }
        }

    indexHints.removeAll(toReduce);
  }

  private static List<Statement> collectReferences(Statement stmt) {
    final long baseTiming = stmt.timing(Statement.TAG_BASE).p50();
    final long timingThreshold = (long) (baseTiming * 0.8);
    return stmt.structSimilarGroups().stream()
        .map(SimGroup::stmts)
        .flatMap(Collection::stream)
        .distinct()
        .filter(it -> it.timing(Statement.TAG_BASE).p50() < timingThreshold)
        .sorted(comparingLong(it -> it.timing(Statement.TAG_BASE).p50()))
        .collect(Collectors.toList());
  }

  private static boolean canHintEngine(Statement base, Statement ref) {
    return base.appContext().dbType().equals(MYSQL) && ref.appContext().dbType().equals(MYSQL);
  }

  private static boolean isLowSelectivity(Index index) {
    final Column prefix = index.column(0);
    return guessBoolean(prefix) || guessEnum(prefix);
  }

  private static boolean guessBoolean(Column column) {
    final String columnName = column.columnName();
    final SQLDataType dataType = column.dataType();
    return column.isFlagged(COLUMN_IS_BOOLEAN)
        || columnName.startsWith("is")
        || columnName.endsWith("ed")
        || columnName.endsWith("able")
        || dataType.width() == 1
        || dataType.storageSize() == 1;
  }

  public static boolean guessEnum(Column column) {
    final String columnName = column.columnName();
    return column.isFlagged(COLUMN_IS_ENUM) || columnName.endsWith("type");
  }

  private static int compareUsage(List<Column> usage0, List<Column> usage1) {
    final List<Column> s = usage0.size() < usage1.size() ? usage0 : usage1;
    final List<Column> l = usage0.size() < usage1.size() ? usage1 : usage0;
    for (int i = 0; i < s.size(); i++) if (!s.get(i).equals(l.get(i))) return 2;
    if (s.size() == l.size()) return 0;
    return s == usage0 ? -1 : 1;
  }
}
