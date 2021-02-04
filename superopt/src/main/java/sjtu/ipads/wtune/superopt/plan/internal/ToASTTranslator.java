package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprType;
import sjtu.ipads.wtune.superopt.plan.*;
import sjtu.ipads.wtune.superopt.plan.symbolic.*;
import sjtu.ipads.wtune.superopt.util.PlaceholderNumbering;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.INNER_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.JoinType.LEFT_JOIN;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.*;

public class ToASTTranslator implements PlanVisitor, Interpretations {
  private final Deque<Relation> stack;
  private Interpretations interpretations;
  private PlaceholderNumbering numbering;
  private ConstraintRegistry constraints;

  private ToASTTranslator() {
    this.stack = new LinkedList<>();
  }

  public static ToASTTranslator build() {
    return new ToASTTranslator();
  }

  public ASTNode translate(Plan plan) {
    plan.acceptVisitor(this);
    assert stack.size() == 1;
    return stack.peek().assembleAsQuery();
  }

  public void setInterpretation(Interpretations interpretations) {
    this.interpretations = interpretations;
  }

  public void setNumbering(PlaceholderNumbering numbering) {
    this.numbering = numbering;
  }

  public void setConstraints(ConstraintRegistry constraints) {
    this.constraints = constraints;
  }

  @Override
  public void leaveInput(Input input) {
    stack.push(Relation.from(interpretInput(input.table()).node().copy()));
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final Placeholder pick = op.fields();
    final Placeholder predicate = op.predicate();

    final ASTNode node =
        interpretPredicate(predicate)
            .instantiate(listMap(ASTNode::copy, interpretPick(pick).nodes()));

    assert !stack.isEmpty();
    stack.peek().appendSelection(node, true);
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final Placeholder pick = op.fields();

    final ASTNode query = stack.pop().assembleAsQuery();

    final ASTNode queryExpr = expr(QUERY_EXPR);
    queryExpr.set(QUERY_EXPR_QUERY, query);

    final ASTNode column = interpretPick(pick).nodes().get(0).copy();

    final ASTNode binary = expr(BINARY);
    binary.set(BINARY_LEFT, column);
    binary.set(BINARY_RIGHT, queryExpr);
    binary.set(BINARY_OP, IN_SUBQUERY);

    assert !stack.isEmpty();
    stack.peek().appendSelection(binary, true);
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    stack.push(Relation.from(makeJoin(op)));
  }

  @Override
  public void leaveLeftJoin(LeftJoin op) {
    stack.push(Relation.from(makeJoin(op)));
  }

  @Override
  public void leaveProj(Proj op) {
    assert !stack.isEmpty();
    stack.peek().setProjection(listMap(ASTNode::copy, interpretPick(op.fields()).nodes()));
  }

  @Override
  public InputInterpretation interpretInput(Placeholder placeholder) {
    InputInterpretation interpretation = null;

    if (interpretations != null) interpretation = interpretations.interpretInput(placeholder);

    if (interpretation == null || interpretation.node() == null) {
      final ASTNode tableName = node(TABLE_NAME);
      tableName.set(TABLE_NAME_TABLE, nameOf(placeholder));
      final ASTNode tableSource = tableSource(SIMPLE_SOURCE);
      tableSource.set(SIMPLE_TABLE, tableName);

      interpretation = () -> tableSource;
    }

    return interpretation;
  }

  @Override
  public PickInterpretation interpretPick(Placeholder placeholder) {
    PickInterpretation interpretation = null;

    if (interpretations != null) interpretation = interpretations.interpretPick(placeholder);

    if (interpretation == null || interpretation.nodes() == null) {
      final Placeholder[] srcs = constraints == null ? null : constraints.sourceOf(placeholder);
      final String srcName = srcs == null ? null : String.join(",", listMap(this::nameOf, srcs));
      final String colName = nameOf(placeholder);

      final ASTNode name = node(COLUMN_NAME);
      if (srcName != null) name.set(COLUMN_NAME_TABLE, srcName);
      name.set(COLUMN_NAME_COLUMN, colName);

      final ASTNode ref = expr(COLUMN_REF);
      ref.set(COLUMN_REF_COLUMN, name);

      interpretation = new PickStub(srcs, ref);
    }

    return interpretation;
  }

  @Override
  public PredicateInterpretation interpretPredicate(Placeholder placeholder) {
    PredicateInterpretation interpretation = null;

    if (interpretations != null) interpretation = interpretations.interpretPredicate(placeholder);

    if (interpretation == null) interpretation = new PredicateStub(nameOf(placeholder));

    return interpretation;
  }

  private String nameOf(Placeholder placeholder) {
    return placeholder.tag()
        + (numbering == null ? placeholder.index() : numbering.numberOf(placeholder));
  }

  private ASTNode makeJoin(Join op) {
    final PickInterpretation left = interpretPick(op.leftFields());
    final PickInterpretation right = interpretPick(op.rightFields());

    final ASTNode leftExpr = left.nodes().get(0).copy();
    final ASTNode rightExpr = right.nodes().get(0).copy();

    final ASTNode binary = expr(BINARY);
    binary.set(BINARY_LEFT, leftExpr);
    binary.set(BINARY_RIGHT, rightExpr);
    binary.set(BINARY_OP, EQUAL);

    final ASTNode rightSource = stack.pop().assembleAsSource();
    final ASTNode leftSource = stack.pop().assembleAsSource();

    final ASTNode join = tableSource(JOINED);
    join.set(JOINED_LEFT, leftSource);
    join.set(JOINED_RIGHT, rightSource);
    join.set(JOINED_ON, binary);
    join.set(JOINED_TYPE, op instanceof InnerJoin ? INNER_JOIN : LEFT_JOIN);

    return join;
  }

  private static class Relation {
    private List<ASTNode> projection;
    private ASTNode selection;
    private ASTNode source;

    private static Relation from(ASTNode source) {
      final Relation rel = new Relation();
      rel.source = source;
      return rel;
    }

    private void appendSelection(ASTNode node, boolean conjunctive) {
      if (projection == null)
        if (selection == null) selection = node;
        else {
          final ASTNode newSelection = expr(ExprType.BINARY);
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
        source = this.assembleAsSource();
        projection = projection;
        selection = null;
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

        return newArrayList(item);
      }
    }
  }

  private static class PickStub implements PickInterpretation {
    private final Placeholder[] sources;
    private final ASTNode node;

    private PickStub(Placeholder[] sources, ASTNode node) {
      this.sources = sources;
      this.node = node;
    }

    @Override
    public Placeholder[] sources() {
      return sources;
    }

    @Override
    public List<ASTNode> nodes() {
      return Collections.singletonList(node);
    }
  }

  private static class PredicateStub implements PredicateInterpretation {
    private final String name;

    private PredicateStub(String name) {
      this.name = name;
    }

    @Override
    public ASTNode instantiate(List<ASTNode> picks) {
      final ASTNode funcName = node(NAME_2);
      funcName.set(NAME_2_1, name);

      final ASTNode func = expr(FUNC_CALL);
      func.set(FUNC_CALL_NAME, funcName);
      func.set(FUNC_CALL_ARGS, listMap(ASTNode::copy, picks));

      return func;
    }
  }
}
