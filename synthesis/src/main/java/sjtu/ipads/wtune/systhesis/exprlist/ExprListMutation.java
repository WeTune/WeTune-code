package sjtu.ipads.wtune.systhesis.exprlist;

import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.QueryCollector;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class ExprListMutation extends Stage {
  private final List<ExprListMutator> mutatorQueue;
  private final Set<Pair<Long, Class<? extends ExprListMutator>>> mutatorSet;

  public ExprListMutation() {
    this.mutatorQueue = new ArrayList<>();
    this.mutatorSet = new HashSet<>();
  }

  public static ExprListMutation build() {
    return new ExprListMutation();
  }

  private boolean registerMutator(SQLNode root, Class<? extends ExprListMutator> cls) {
    final Pair<Long, Class<? extends ExprListMutator>> pair = Pair.of(root.get(NODE_ID), cls);
    if (mutatorSet.contains(pair)) return false;

    if (cls == ReduceDistinct.class && ReduceDistinct.canReduceDistinct(root)) {
      mutatorQueue.add(new ReduceDistinct(root));
    } else if (cls == ReduceSubqueryOrderBy.class && ReduceSubqueryOrderBy.canReduceOrderBy(root)) {
      mutatorQueue.add(new ReduceSubqueryOrderBy(root));
    } else return false;

    mutatorSet.add(pair);
    return true;
  }

  private int registerApplicableMutators(SQLNode root) {
    final SQLNode rootQuery = root.get(RESOLVED_QUERY_SCOPE).queryNode();
    final List<SQLNode> subQueries = QueryCollector.collect(rootQuery, true);
    final int size = mutatorQueue.size();

    registerMutator(rootQuery, ReduceDistinct.class);
    for (SQLNode subQuery : subQueries) {
      registerMutator(subQuery, ReduceDistinct.class);
      registerMutator(subQuery, ReduceSubqueryOrderBy.class);
    }

    return mutatorQueue.size() - size;
  }

  private boolean recMutate(Statement stmt, int nextMutatorIdx) {
    if (nextMutatorIdx >= mutatorQueue.size()) return offer(stmt);
    if (!recMutate(stmt, nextMutatorIdx + 1)) return false;

    final Statement copy = stmt.copy();
    mutatorQueue.get(nextMutatorIdx).modifyAST(copy.parsed());

    return recMutate(copy, nextMutatorIdx + 1);
  }

  private boolean feed0(Statement stmt) {
    registerApplicableMutators(stmt.parsed());
    return recMutate(stmt, 0);
  }

  @Override
  public boolean feed(Object o) {
    return feed0((Statement) o);
  }
}
