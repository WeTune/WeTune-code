package sjtu.ipads.wtune.systhesis.op;

import com.google.common.collect.Multimap;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.analyzer.ColumnAccessAnalyzer;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.systhesis.op.impl.DeleteImpl;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

public class DeleteOp implements Op {
  private final SQLNode node;
  private final OpContext ctx;

  private DeleteOp(OpContext ctx, SQLNode node) {
    this.ctx = ctx;
    this.node = node;
  }

  public static DeleteOp build(OpContext ctx, SQLNode node) {
    return new DeleteOp(ctx, node);
  }

  @Override
  public OpContext apply(OpContext ctx) {
    final SQLNode root = ctx.current().parsed();
    final SQLNode target = NodeFinder.find(root, node);

    if (target.type() == SQLNode.Type.TABLE_SOURCE)
      replaceTableIfNeed(ctx.current().get(JOIN_CONDITIONS), target);

    DeleteImpl.apply(root, target);

    ctx.addOp(this);
    ctx.current().reResolve();
    return ctx;
  }

  private void replaceTableIfNeed(Multimap<ColumnRef, ColumnRef> joinConds, SQLNode target) {
    final ColumnRefFixer fixer = new ColumnRefFixer(joinConds, target);
    // only need to apply fixer within the scope
    target.get(RESOLVED_QUERY_SCOPE).queryNode().accept(fixer);
    if (!fixer.allFixed)
      throw new StmtException(
          "failed to delete table source <" + target + "> within [" + ctx.current() + "]");
  }

  Long targetId() {
    return node.get(NODE_ID);
  }

  public static Set<DeleteOp> collectApplicable(OpContext ctx) {
    final SQLNode parsed = ctx.current().parsed();

    final Set<DeleteOp> operations = new HashSet<>();
    final Collector collector = new Collector();
    parsed.accept(collector);

    Set<SQLNode> reducibleTables = null;
    for (SQLNode candidate : collector.candidates) {

      if (candidate.type() == SQLNode.Type.TABLE_SOURCE) {
        if (reducibleTables == null) reducibleTables = ReducibleTableAnalyzer.analyze(parsed);
        if (!containsTable(reducibleTables, candidate)) continue;
      }

      if (candidate.type() == SQLNode.Type.EXPR)
        if (candidate.get(BOOL_EXPR).isJoinCondition()) continue;

      operations.add(build(ctx, candidate));
    }

    return operations;
  }

  private static boolean containsTable(Set<SQLNode> nodes, SQLNode node) {
    if (isJoined(node))
      return containsTable(nodes, node.get(JOINED_LEFT))
          || containsTable(nodes, node.get(JOINED_RIGHT));
    else return nodes.contains(node);
  }

  @Override
  public String toString() {
    return String.format("DeleteOp(<%d: %s>)", node.get(NODE_ID), node);
  }

  private static class Collector implements SQLVisitor {
    private final List<SQLNode> candidates = new ArrayList<>();

    @Override
    public boolean enter(SQLNode node) {
      final SQLNode.Type nodeType = node.type();
      if (nodeType == SQLNode.Type.ORDER_ITEM) candidates.add(node);
      else if (nodeType == SQLNode.Type.GROUP_ITEM) candidates.add(node);
      else if (nodeType == SQLNode.Type.TABLE_SOURCE) {
        // if parent is not a JOIN, i.e. the only table in query, don't delete it
        if (node.parent().type() == SQLNode.Type.TABLE_SOURCE && !isJoined(node))
          candidates.add(node);

      } else if (nodeType == SQLNode.Type.EXPR) {
        final QueryScope.Clause clause = node.get(RESOLVED_CLAUSE_SCOPE);
        if ((clause == QueryScope.Clause.WHERE || clause == QueryScope.Clause.HAVING)
            && node.get(BOOL_EXPR) != null
            && node.get(BOOL_EXPR).isPrimitive()) candidates.add(node);

      } else if (nodeType == SQLNode.Type.QUERY) {
        final QueryScope enclosingScope = node.get(RESOLVED_QUERY_SCOPE).parent();
        if (enclosingScope != null && node.parent().type() == SQLNode.Type.SET_OP)
          candidates.add(node);
      }
      return true;
    }
  }

  private static class ReducibleTableAnalyzer implements SQLVisitor {
    private final Set<SQLNode> nodes = new HashSet<>();

    public static Set<SQLNode> analyze(SQLNode node) {
      final ReducibleTableAnalyzer analyzer = new ReducibleTableAnalyzer();
      node.accept(analyzer);
      return analyzer.nodes;
    }

    @Override
    public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
      if (checkReducibility(simpleTableSource)) nodes.add(simpleTableSource);
      return false;
    }

    @Override
    public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
      if (checkReducibility(derivedTableSource)) nodes.add(derivedTableSource);
      return true;
    }

    private boolean checkReducibility(SQLNode tableSource) {
      final QueryScope scope = tableSource.get(RESOLVED_QUERY_SCOPE);
      final Set<ColumnRef> usedColumns =
          ColumnAccessAnalyzer.analyze(
              scope.queryNode(), tableSource.get(RESOLVED_TABLE_SOURCE), false);
      return usedColumns.size() == 1;
      //      return usedColumns.stream().map(ColumnRef::identity).distinct().count() == 1;
    }
  }

  private static class ColumnRefFixer implements SQLVisitor {
    private final Multimap<ColumnRef, ColumnRef> joinConds;
    private final TableSource targetTable;
    private boolean allFixed = true;

    private ColumnRefFixer(Multimap<ColumnRef, ColumnRef> joinConds, SQLNode tableNode) {
      this.joinConds = joinConds;
      this.targetTable = tableNode.get(RESOLVED_TABLE_SOURCE);
    }

    @Override
    public boolean enterColumnRef(SQLNode columnRef) {
      final ColumnRef ref = columnRef.get(RESOLVED_COLUMN_REF);
      if (!ref.isFrom(targetTable)) return false;

      final Collection<ColumnRef> eqRefs = joinConds.get(ref);

      boolean fixed = false;
      for (ColumnRef eqRef : eqRefs) {
        if (eqRef.node().get(RESOLVED_QUERY_SCOPE) != columnRef.get(RESOLVED_QUERY_SCOPE)) continue;

        final Long originalId = columnRef.get(NODE_ID);
        columnRef.replaceThis(eqRef.node());
        columnRef.put(NODE_ID, originalId);

        fixed = true;
        break;
      }

      allFixed &= fixed;

      return false;
    }
  }
}
