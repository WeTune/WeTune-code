package sjtu.ipads.wtune.reconfiguration;

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

import static sjtu.ipads.wtune.reconfiguration.OperatorScopeResolver.RESOLVED_OPERATOR_SCOPE;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class ColumnMatching {
  public static final Attrs.Key<Set<Column>> ACCESSED_COLUMNS =
      Attrs.key2("reconfig.attr.accessedColumns", Set.class);

  private final Map<Column, EnumSet<OpCategory>> slow;
  private final Map<Column, EnumSet<OpCategory>> fast;
  private final MutableGraph<Column> matching =
      GraphBuilder.undirected().allowsSelfLoops(false).build();
  private int matchingCount = 0;

  public ColumnMatching(
      Map<Column, EnumSet<OpCategory>> slow, Map<Column, EnumSet<OpCategory>> fast) {
    this.slow = slow;
    this.fast = fast;
  }

  public static ColumnMatching build(Statement slow, Statement fast) {
    slow.resolve(OperatorScopeResolver.class);
    fast.resolve(OperatorScopeResolver.class);

    final Map<Column, EnumSet<OpCategory>> slowColumns = collectColumns(slow.parsed());
    final Map<Column, EnumSet<OpCategory>> fastColumns = collectColumns(fast.parsed());

    slow.put(ACCESSED_COLUMNS, slowColumns.keySet());

    final ColumnMatching matching = new ColumnMatching(slowColumns, fastColumns);

    for (var slowPair : slowColumns.entrySet()) {
      final Column slowColumn = slowPair.getKey();

      for (var fastPair : fastColumns.entrySet()) {
        final Column fastColumn = fastPair.getKey();

        if (!slowColumn.equals(fastColumn)
            && fastColumn.uniquePart() == slowColumn.uniquePart()
            && !Collections.disjoint(slowPair.getValue(), fastPair.getValue())
            && matching.matching.putEdge(slowColumn, fastColumn)) ++matching.matchingCount;
      }
    }

    return matching;
  }

  private static Map<Column, EnumSet<OpCategory>> collectColumns(SQLNode root) {
    final List<SQLNode> columnRefs = ColumnRefCollector.collect(root);
    final Map<Column, EnumSet<OpCategory>> columns = new HashMap<>();

    for (SQLNode columnRef : columnRefs) {
      final OpCategory op = columnRef.get(RESOLVED_OPERATOR_SCOPE);
      final ColumnRef cRef = columnRef.get(RESOLVED_COLUMN_REF);
      final Column column;
      if (op == null || (column = cRef.resolveAsColumn()) == null) continue;
      columns.computeIfAbsent(column, ignored -> EnumSet.noneOf(OpCategory.class)).add(op);
    }

    return columns;
  }

  public Set<Column> slow() {
    return slow.keySet();
  }

  public Set<Column> fast() {
    return fast.keySet();
  }

  public Set<Column> matchOf(Column column) {
    if (!matching.nodes().contains(column)) return Collections.emptySet();
    return matching.adjacentNodes(column);
  }

  private static final EnumSet<OpCategory> EMPTY = EnumSet.noneOf(OpCategory.class);

  public EnumSet<OpCategory> opOf(Column column) {
    return slow.getOrDefault(column, EMPTY);
  }

  public int matchingCount() {
    return matchingCount;
  }
}
