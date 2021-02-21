package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;

public class ToASTTranslator {
  private final Deque<Query> stack;

  private ToASTTranslator() {
    this.stack = new LinkedList<>();
  }

  public static ASTNode translate(PlanNode head) {
    final ToASTTranslator translator = new ToASTTranslator();
    translator.translate0(head);
    assert translator.stack.size() == 1;
    return translator.stack.peek().assembleAsQuery();
  }

  private void translate0(PlanNode node) {
    if (node instanceof FilterGroupNode) {
      ((FilterGroupNode) node).filters().forEach(this::translate0);
      return;
    }

    for (PlanNode predecessor : node.predecessors()) translate0(predecessor);

    if (node instanceof InputNode) translateInput((InputNode) node);
    else if (node instanceof JoinNode) translateJoin((JoinNode) node);
    else if (node instanceof ProjNode) translateProj((ProjNode) node);
    else if (node instanceof PlainFilterNode) translateFilter((PlainFilterNode) node);
    else if (node instanceof SubqueryFilterNode) translateFilter((SubqueryFilterNode) node);
    else assert false;
  }

  private void translateInput(InputNode input) {
    stack.push(Query.from(input.toTableSource()));
  }

  private void translateProj(ProjNode op) {
    assert !stack.isEmpty();
    stack.peek().setProjection(op.selectItems());
  }

  private void translateFilter(FilterNode op) {
    assert !stack.isEmpty();
    stack.peek().appendSelection(op.expr(), true);
  }

  private void translateFilter(SubqueryFilterNode op) {
    final ASTNode subquery = stack.pop().assembleAsQuery();
    final ASTNode column = op.expr().get(BINARY_LEFT);

    final ASTNode queryExpr = expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, subquery);

    final ASTNode binary = expr(BINARY);
    binary.set(BINARY_OP, IN_SUBQUERY);
    binary.set(BINARY_LEFT, column);
    binary.set(BINARY_RIGHT, queryExpr);

    assert !stack.isEmpty();
    stack.peek().appendSelection(binary, true);
  }

  private void translateJoin(JoinNode op) {
    final ASTNode rightSource = stack.pop().assembleAsSource();
    final ASTNode leftSource = stack.pop().assembleAsSource();

    final ASTNode join = tableSource(JOINED_SOURCE);
    join.set(JOINED_LEFT, leftSource);
    join.set(JOINED_RIGHT, rightSource);
    join.set(JOINED_ON, op.onCondition());
    join.set(JOINED_TYPE, op instanceof InnerJoinNode ? INNER_JOIN : LEFT_JOIN);

    stack.push(Query.from(join));
  }

  private static class Query {
    private List<ASTNode> projection;
    private ASTNode selection;
    private ASTNode source;

    private static Query from(ASTNode source) {
      final Query rel = new Query();
      rel.source = source;
      return rel;
    }

    private void appendSelection(ASTNode node, boolean conjunctive) {
      if (projection == null)
        if (selection == null) selection = node;
        else {
          final ASTNode newSelection = expr(ExprKind.BINARY);
          newSelection.set(BINARY_LEFT, node);
          newSelection.set(BINARY_RIGHT, selection);
          newSelection.set(BINARY_OP, conjunctive ? AND : OR);
          selection = newSelection;
        }
      else {
        source = this.assembleAsSource();
        projection = null;
        selection = node;
      }
    }

    private void setProjection(List<ASTNode> projection) {
      if (this.projection == null) this.projection = projection;
      else {
        this.source = this.assembleAsSource();
        this.projection = projection;
        this.selection = null;
      }
    }

    private ASTNode assembleAsQuery() {
      if (projection == null && selection == null && QUERY.isInstance(source)) return source;

      final ASTNode querySpec = node(QUERY_SPEC);
      querySpec.set(QUERY_SPEC_SELECT_ITEMS, selectItems());
      querySpec.set(QUERY_SPEC_FROM, source);
      if (selection != null) querySpec.set(QUERY_SPEC_WHERE, selection);

      final ASTNode query = node(QUERY);
      query.set(QUERY_BODY, querySpec);

      return query;
    }

    private ASTNode assembleAsSource() {
      if (projection == null && selection == null) return source;

      final ASTNode source = tableSource(DERIVED_SOURCE);
      source.set(DERIVED_SUBQUERY, assembleAsQuery());

      return source;
    }

    private List<ASTNode> selectItems() {
      if (projection != null) return projection;
      else {
        final ASTNode wildcard = expr(WILDCARD);

        final ASTNode item = node(SELECT_ITEM);
        item.set(SELECT_ITEM_EXPR, wildcard);

        return singletonList(item);
      }
    }
  }
}
