package sjtu.ipads.wtune.reconfiguration;

import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.similarity.struct.OpCategory;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.reconfiguration.OperatorScopeResolver.RESOLVED_OPERATOR_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class ColumnMatching {
  public static final Attrs.Key<Set<Column>> ACCESSED_COLUMNS =
      Attrs.key2("reconfig.attr.accessedColumns", Set.class);

  private final Set<Column> slow = new HashSet<>();
  private final Set<Column> fast = new HashSet<>();
  private final MutableGraph<Column> matching =
      GraphBuilder.undirected().allowsSelfLoops(false).build();
  private int matchingCount = 0;

  public static ColumnMatching build(Statement slow, Statement fast) {
    slow.resolve(OperatorScopeResolver.class);
    fast.resolve(OperatorScopeResolver.class);

    final Map<OpCategory, Set<Column>> slowColumnsByOp = collectColumnsByOp(slow.parsed());
    final Map<OpCategory, Set<Column>> fastColumnsByOp = collectColumnsByOp(fast.parsed());

    slow.put(
        ACCESSED_COLUMNS,
        slowColumnsByOp.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));

    final ColumnMatching matching = new ColumnMatching();

    slowColumnsByOp.values().forEach(matching.slow::addAll);
    fastColumnsByOp.values().forEach(matching.fast::addAll);

    matching.slow.forEach(matching.matching::addNode);
    matching.fast.forEach(matching.matching::addNode);

    for (OpCategory op : OpCategory.values()) {
      final Set<Column> slowColumns = slowColumnsByOp.get(op);
      final Set<Column> fastColumns = fastColumnsByOp.get(op);
      if (slowColumns == null || fastColumns == null) continue;
      for (Column slowColumn : slowColumns)
        for (Column fastColumn : fastColumns)
          if (!slowColumn.equals(fastColumn) && matching.matching.putEdge(slowColumn, fastColumn))
            ++matching.matchingCount;
    }

    return matching;
  }

  private static Map<OpCategory, Set<Column>> collectColumnsByOp(SQLNode root) {
    final List<SQLNode> columnRefs = ColumnRefCollector.collect(root);
    final Map<OpCategory, Set<Column>> columnsByOp = new HashMap<>();

    for (SQLNode columnRef : columnRefs) {
      final OpCategory op = columnRef.get(RESOLVED_OPERATOR_SCOPE);
      final ColumnRef cRef = columnRef.get(RESOLVED_COLUMN_REF);
      final Column column;
      if (op == null || (column = cRef.resolveAsColumn()) == null) continue;
      columnsByOp.computeIfAbsent(op, ignored -> new HashSet<>()).add(column);
    }

    return columnsByOp;
  }

  public Set<Column> slow() {
    return slow;
  }

  public Set<Column> fast() {
    return fast;
  }

  public Graph<Column> matching() {
    return matching;
  }

  public int matchingCount() {
    return matchingCount;
  }
}
