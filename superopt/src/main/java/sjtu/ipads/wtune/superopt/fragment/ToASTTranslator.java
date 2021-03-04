package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.util.Constraints;

import java.util.Deque;
import java.util.LinkedList;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.*;

public class ToASTTranslator implements OperatorVisitor {
  private final Deque<Query> stack;
  private Numbering numbering;
  private Constraints constraints;

  private ToASTTranslator() {
    this.stack = new LinkedList<>();
  }

  public static ToASTTranslator build() {
    return new ToASTTranslator();
  }

  public ASTNode translate(Fragment fragment) {
    stack.clear();
    fragment.acceptVisitor(this);
    assert stack.size() == 1;
    return stack.peek().assembleAsQuery();
  }

  public ToASTTranslator setNumbering(Numbering numbering) {
    this.numbering = numbering;
    return this;
  }

  public ToASTTranslator setConstraints(Constraints constraints) {
    this.constraints = constraints;
    return this;
  }

  @Override
  public void leaveInput(Input input) {
    final ASTNode tableName = node(TABLE_NAME);
    tableName.set(TABLE_NAME_TABLE, nameOf(input.table()));
    final ASTNode tableSource = tableSource(SIMPLE_SOURCE);
    tableSource.set(SIMPLE_TABLE, tableName);

    stack.push(Query.from(tableSource));
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final ASTNode funcName = node(NAME_2);
    funcName.set(NAME_2_1, nameOf(op.predicate()));

    final ASTNode func = expr(FUNC_CALL);
    func.set(FUNC_CALL_NAME, funcName);
    func.set(FUNC_CALL_ARGS, singletonList(makeColumnRefNode(op.fields())));

    assert !stack.isEmpty();
    stack.peek().appendSelection(func, true);
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final ASTNode query = stack.pop().assembleAsQuery();

    final ASTNode queryExpr = expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, query);

    final ASTNode column = makeColumnRefNode(op.fields());

    final ASTNode binary = expr(BINARY);
    binary.set(BINARY_LEFT, column);
    binary.set(BINARY_RIGHT, queryExpr);
    binary.set(BINARY_OP, IN_SUBQUERY);

    assert !stack.isEmpty();
    stack.peek().appendSelection(binary, true);
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    stack.push(Query.from(makeJoin(op)));
  }

  @Override
  public void leaveLeftJoin(LeftJoin op) {
    stack.push(Query.from(makeJoin(op)));
  }

  @Override
  public void leaveProj(Proj op) {
    assert !stack.isEmpty();
    stack.peek().setProjection(makeColumnRefNode(op.fields()));
  }

  private ASTNode makeColumnRefNode(Placeholder placeholder) {
    final Placeholder[] srcs = constraints == null ? null : constraints.sourceOf(placeholder);
    final String srcName = srcs == null ? null : String.join(",", listMap(this::nameOf, srcs));
    final String colName = nameOf(placeholder);

    final ASTNode name = node(COLUMN_NAME);
    if (srcName != null) name.set(COLUMN_NAME_TABLE, srcName);
    name.set(COLUMN_NAME_COLUMN, colName);

    final ASTNode ref = expr(COLUMN_REF);
    ref.set(COLUMN_REF_COLUMN, name);

    return ref;
  }

  private ASTNode makeJoin(Join op) {
    final ASTNode leftExpr = makeColumnRefNode(op.leftFields());
    final ASTNode rightExpr = makeColumnRefNode(op.rightFields());

    final ASTNode binary = expr(BINARY);
    binary.set(BINARY_LEFT, leftExpr);
    binary.set(BINARY_RIGHT, rightExpr);
    binary.set(BINARY_OP, EQUAL);

    final ASTNode rightSource = stack.pop().assembleAsSource();
    final ASTNode leftSource = stack.pop().assembleAsSource();

    final ASTNode join = tableSource(JOINED_SOURCE);
    join.set(JOINED_LEFT, leftSource);
    join.set(JOINED_RIGHT, rightSource);
    join.set(JOINED_ON, binary);
    join.set(JOINED_TYPE, op instanceof InnerJoin ? INNER_JOIN : LEFT_JOIN);

    return join;
  }

  private String nameOf(Placeholder placeholder) {
    return placeholder.tag()
        + (numbering == null ? placeholder.index() : numbering.numberOf(placeholder));
  }

  private static class Query {
    private ASTNode projection;
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

    private void setProjection(ASTNode projection) {
      if (this.projection == null) {
        if (!SELECT_ITEM.isInstance(projection)) {
          final ASTNode selectItem = ASTNode.node(SELECT_ITEM);
          selectItem.set(SELECT_ITEM_EXPR, projection);
          projection = selectItem;
        }
        this.projection = projection;

      } else {
        this.source = this.assembleAsSource();
        this.projection = projection;
        this.selection = null;
      }
    }

    private ASTNode assembleAsQuery() {
      if (projection == null && selection == null && QUERY.isInstance(source)) return source;

      final ASTNode querySpec = node(QUERY_SPEC);
      querySpec.set(QUERY_SPEC_SELECT_ITEMS, singletonList(selectItems()));
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

    private ASTNode selectItems() {
      if (projection != null) return projection;
      else {
        final ASTNode wildcard = expr(WILDCARD);

        final ASTNode item = node(SELECT_ITEM);
        item.set(SELECT_ITEM_EXPR, wildcard);

        return item;
      }
    }
  }
}
