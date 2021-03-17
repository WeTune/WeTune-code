package sjtu.ipads.wtune.superopt.fragment;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.expr;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.node;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.tableSource;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_ARGS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.QUERY_EXPR_QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_2_1;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_BODY;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_FROM;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_SELECT_ITEMS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.QUERY_SPEC_WHERE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_ON;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.JOINED_TYPE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.AND;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.EQUAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.IN_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.OR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.FUNC_CALL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.QUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.COLUMN_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.NAME_2;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY_SPEC;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.SELECT_ITEM;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.JOINED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;

import java.util.Deque;
import java.util.LinkedList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.util.Constraints;

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
