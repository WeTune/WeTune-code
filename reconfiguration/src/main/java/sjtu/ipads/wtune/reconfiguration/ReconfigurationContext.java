package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Constraint;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.similarity.struct.OpCategory;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level;
import static java.util.Comparator.comparingLong;
import static sjtu.ipads.wtune.reconfiguration.Reconfiguration.LOG;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.statement.Statement.TAG_BASE;

public class ReconfigurationContext {

  private final ReconfigurationOutput output = new ReconfigurationOutput();

  private final Set<Index> indexHints = output.indexHints;
  private final Map<Table, String> engineHints = output.engineHints;

  private ReconfigurationContext() {}

  public ReconfigurationOutput output() {
    return output;
  }

  public static ReconfigurationContext build() {
    return new ReconfigurationContext();
  }

  public void startOne(Statement base) {
    base.retrofitStandard();

    final List<Statement> references = collectReferences(base);
    for (Statement ref : references) {
      ref.retrofitStandard();
      final ColumnMatching matching = ColumnMatching.build(base, ref);

      final IndexHints indexHint = IndexHints.hint(matching);
      if (indexHint != null)
        for (List<Column> columns : indexHint) {
          //           heuristic: column all comes from order by is considered as bad one
          if (!filterIndexForOrderBy(base, columns, matching)) continue;

          final Index index = Index.build(columns);
          if (indexHints.add(index)) LOG.log(Level.INFO, "{0} - {1} : {2}", base, ref, index);
        }

      final EngineHints engineHint;
      if (canHintEngine(base, ref) && (engineHint = EngineHints.hint(matching)) != null)
        for (var pair : engineHint.hints().entrySet())
          if (engineHints.putIfAbsent(pair.getKey(), pair.getValue()) == null)
            LOG.log(
                Level.INFO, "{0} - {1} : {2} -> {3}", base, ref, pair.getKey(), pair.getValue());
    }
  }

  public void reduceExisting() {
    indexHints.removeIf(ReconfigurationContext::isExisting);
  }

  public void reduceLowSelectivity() {
    indexHints.removeIf(Index::isLowSelective);
  }

  public void reduceLowUsage(List<Statement> stmts) {
    final Map<Table, List<Index>> indexByTable =
        indexHints.stream().collect(Collectors.groupingBy(Index::table));
    final Set<Index> toReduce = new HashSet<>();

    for (Table table : indexByTable.keySet()) {
      final List<Index> indexes = indexByTable.get(table);

      for (Index index : indexes)
        for (Constraint existing : table.indexes())
          if (compareIndex(index, Index.build(existing), stmts) == -1) toReduce.add(index);

      for (int i = 0; i < indexes.size() - 1; i++)
        for (int j = i + 1; j < indexes.size(); j++) {
          final Index index0 = indexes.get(i);
          final Index index1 = indexes.get(j);
          if (toReduce.contains(index0) || toReduce.contains(index1)) continue;

          final int result = compareIndex(index0, index1, stmts);
          if (result == -1) toReduce.add(index0);
          else if (result == 1) toReduce.add(index1);
        }
    }

    indexHints.removeAll(toReduce);
  }

  public void reduceEngine() {
    indexHints.forEach(it -> engineHints.remove(it.table()));
  }

  // -1: reduce index0, 1: reduce index1, 0: do nothing
  private int compareIndex(Index index0, Index index1, List<Statement> stmts) {
    // quick check
    if (Collections.disjoint(index0.columns(), index1.columns())) return 0;
    // -2: init, -1: less, 0: equal, 1: greater, 2: other
    int relation = -2;

    for (Statement stmt : stmts) {
      final Set<Column> usage0 = index0.usage(stmt);
      final Set<Column> usage1 = index1.usage(stmt);
      if (usage0.isEmpty() && usage1.isEmpty()) continue;

      final int compare = compareUsage(usage0, usage1);
      if (relation == -2 || relation == 0) relation = compare;
      else if (compare == 2) relation = 2;
      else if (compare != 0 && compare != relation) relation = 2;

      if (relation == 2) break;
    }

    if (relation == -2 || relation == 2) return 0;
    if (relation == -1) return -1;
    else if (relation == 1) return 1;
    else {
      // prefer shorter
      if (index0.size() < index1.size()) return 1;
      else return -1;
    }
  }

  private static boolean filterIndexForOrderBy(
      Statement stmt, List<Column> index, ColumnMatching matching) {
    return stmt.parsed().get(QUERY_ORDER_BY) == null
        || (stmt.parsed().get(QUERY_LIMIT) == null
            || !index.stream()
                .map(matching::opOf)
                .allMatch(it -> it.contains(OpCategory.ORDER_BY)));
  }

  private static List<Statement> collectReferences(Statement stmt) {
    final long baseTiming = stmt.timing(TAG_BASE).p50();
    final long timingThreshold = (long) (baseTiming * 0.8);
    return stmt.structSimilarGroups().stream()
        .map(SimGroup::stmts)
        .flatMap(Collection::stream)
        .distinct()
        .filter(it -> it.timing(TAG_BASE) != null && it.timing(TAG_BASE).p50() < timingThreshold)
        .sorted(comparingLong(it -> it.timing(TAG_BASE).p50()))
        .collect(Collectors.toList());
  }

  private static boolean canHintEngine(Statement base, Statement ref) {
    return base.appContext().dbType().equals(MYSQL) && ref.appContext().dbType().equals(MYSQL);
  }

  private static boolean isExisting(Index index) {
    for (Constraint constraint : index.table().constraints())
      if (!constraint.fromPatch() && constraint.isIndex() && index.coveredBy(constraint.columns()))
        return true;
    return false;
  }

  private static int compareUsage(Set<Column> usage0, Set<Column> usage1) {
    final Set<Column> s = usage0.size() < usage1.size() ? usage0 : usage1;
    final Set<Column> l = usage0.size() < usage1.size() ? usage1 : usage0;
    if (l.containsAll(s))
      if (s.size() == l.size()) return 0;
      else return s == usage0 ? -1 : 1;
    else return 2;
  }
}
