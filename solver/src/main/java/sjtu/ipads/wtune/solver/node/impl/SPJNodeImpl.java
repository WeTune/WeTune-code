package sjtu.ipads.wtune.solver.node.impl;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.JoinType;
import sjtu.ipads.wtune.solver.node.SPJNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;
import sjtu.ipads.wtune.solver.sql.Operator;
import sjtu.ipads.wtune.solver.sql.ProjectionItem;
import sjtu.ipads.wtune.solver.sql.expr.Expr;
import sjtu.ipads.wtune.solver.sql.expr.InputRef;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class SPJNodeImpl extends BaseAlgNode implements SPJNode {
  private final List<String> inputAliases;
  private final List<ProjectionItem> projections;
  private final Expr filters;

  private final List<JoinType> joinTypes; // the length should be selections.size() - 1
  private final List<Expr> joinConditions;

  // for cache
  private List<ColumnRef> columns;
  private List<SymbolicColumnRef> filtered;
  private List<SymbolicColumnRef> projected;

  private SPJNodeImpl(
      List<AlgNode> inputs,
      List<String> inputAliases,
      List<ProjectionItem> projections,
      List<JoinType> joinTypes,
      List<Expr> joinConditions,
      Expr filters) {
    super(inputs);
    this.inputAliases = inputAliases;
    this.projections = projections;
    this.joinTypes = joinTypes;
    this.joinConditions = joinConditions;
    this.filters = filters;
  }

  public static Builder builder() {
    return new BuilderImpl();
  }

  @Override
  public List<String> inputAliases() {
    return inputAliases;
  }

  @Override
  public List<ColumnRef> columns() {
    if (columns != null) return columns;

    columns = listMap(it -> it.projectOn(inputs(), inputAliases(), inputColumns()), projections);
    columns.forEach(it -> it.setOwner(this));
    return columns;
  }

  @Override
  public List<SymbolicColumnRef> filtered() {
    if (filtered != null) return filtered;

    // first build the join conditions
    final List<SymbolicColumnRef> refs = new ArrayList<>(inputColumns().size());
    refs.addAll(copyCols(inputs().get(0).projected()));

    for (int i = 1; i < inputs().size(); i++) {
      final JoinType joinType = joinTypes().get(i - 1);
      final Constraint joinCond = applyPredicate(joinConditions().get(i - 1));
      final List<SymbolicColumnRef> rightCols = copyCols(inputs().get(i).projected());

      if (joinType == JoinType.INNER) {
        refs.forEach(it -> it.updateCondition(joinCond, ctx::and));
        rightCols.forEach(it -> it.updateCondition(joinCond, ctx::and));
      }

      if (joinType == JoinType.LEFT)
        rightCols.forEach(it -> it.updateNotNull(joinCond, (c, n) -> ctx.ite(c, n, null)));

      refs.addAll(rightCols);
    }

    // then add filter
    final Constraint filterCond = applyPredicate(filters());
    refs.forEach(it -> it.updateCondition(filterCond, ctx::and));

    return filtered = refs;
  }

  @Override
  public List<SymbolicColumnRef> projected() {
    if (projected != null) return projected;

    final List<ColumnRef> cols = columns();
    final List<SymbolicColumnRef> projected =
        listMap(it -> it.projectOn(inputs(), inputAliases(), filtered(), ctx), projections());

    for (int i = 0; i < projected.size(); i++) projected.get(i).setColumnRef(cols.get(i));

    return this.projected = projected;
  }

  @Override
  public List<ProjectionItem> projections() {
    return projections;
  }

  @Override
  public List<Expr> joinConditions() {
    return joinConditions;
  }

  @Override
  public List<JoinType> joinTypes() {
    return joinTypes;
  }

  @Override
  public Expr filters() {
    return filters;
  }

  private List<SymbolicColumnRef> copyCols(List<SymbolicColumnRef> refs) {
    return listMap(SymbolicColumnRef::copy, refs);
  }

  private Constraint applyPredicate(Expr predicate) {
    return predicate == null
        ? null
        : predicate.asConstraint(inputs(), inputAliases(), inputSymbolicColumns(), ctx);
  }

  @Override
  public String toString() {
    return toString(0);
  }

  @Override
  public String toString(int indentLevel) {
    if (FORMAT_BREAK_LINE) return toString0(indentLevel);
    else return toString1(indentLevel);
  }

  private String toString0(int indentLevel) {
    final StringBuilder builder = new StringBuilder();
    if (indentLevel != 0) builder.append(" ".repeat(indentLevel)).append("\n(");
    builder.append("SELECT");

    for (ColumnRef column : columns())
      builder
          .append("\n")
          .append(" ".repeat(indentLevel + 2))
          .append(column)
          .append(" AS ")
          .append(column.alias())
          .append(",");

    builder.delete(builder.length() - 1, builder.length());

    builder.append('\n').append(" ".repeat(indentLevel)).append("FROM ");

    if (inputs().size() > 1) builder.append('\n').append(" ".repeat(indentLevel + 2));

    builder.append(inputs().get(0).toString(indentLevel + 2));

    if (inputAliases().get(0) != null) builder.append(" AS ").append(inputAliases().get(0));

    for (int i = 1; i < inputs().size(); i++) {
      builder
          .append("\n")
          .append(" ".repeat(indentLevel + 2))
          .append(joinTypes().get(i - 1))
          .append(" JOIN ")
          .append(inputs().get(i).toString(indentLevel + 2));

      if (inputAliases().get(i) != null) builder.append(" AS ").append(inputAliases().get(i));

      builder.append(" ON ").append(joinConditions().get(i - 1));
    }

    if (filters() != null)
      builder.append('\n').append(" ".repeat(indentLevel)).append("WHERE ").append(filters());

    if (indentLevel != 0) builder.append(')');

    return builder.toString();
  }

  private String toString1(int indentLevel) {
    final StringBuilder builder = new StringBuilder();
    if (indentLevel != 0) builder.append("\n(");
    builder.append("SELECT");

    for (ColumnRef column : columns())
      builder.append(" ").append(column).append(" AS ").append(column.alias()).append(",");

    builder.delete(builder.length() - 1, builder.length()).append(" FROM ");

    builder.append(inputs().get(0).toString(indentLevel + 2));
    if (inputAliases().get(0) != null) builder.append(" AS ").append(inputAliases().get(0));

    for (int i = 1; i < inputs().size(); i++) {
      builder
          .append(" ")
          .append(joinTypes().get(i - 1))
          .append(" JOIN ")
          .append(inputs().get(i).toString(indentLevel + 2));

      if (inputAliases().get(i) != null) builder.append(" AS ").append(inputAliases().get(i));

      builder.append(" ON ").append(joinConditions().get(i - 1));
    }

    if (filters() != null) builder.append(" WHERE ").append(filters());

    if (indentLevel != 0) builder.append(')');

    return builder.toString();
  }

  private static class BuilderImpl implements Builder {
    private final List<AlgNode> inputs = new ArrayList<>();
    private final List<String> inputAliases = new ArrayList<>();
    private final List<JoinType> joinTypes = new ArrayList<>();
    private final List<Pair<InputRef, InputRef>> joins = new ArrayList<>();
    private final List<Expr> projExpr = new ArrayList<>();
    private final List<String> projAliases = new ArrayList<>();
    private Expr filters;

    @Override
    public Builder from(AlgNode node, String alias) {
      alias = inferAlias(node, alias);
      if (joins.size() == inputs.size()) {
        inputs.add(0, node);
        inputAliases.add(0, alias);
      } else {
        inputs.set(0, node);
        inputAliases.set(0, alias);
      }
      return this;
    }

    @Override
    public Builder projection(Expr refs, String alias) {
      projExpr.add(refs);
      projAliases.add(alias);
      return this;
    }

    @Override
    public Builder projections(Expr... items) {
      projExpr.addAll(asList(items));
      projAliases.addAll(nCopies(items.length, null));
      return this;
    }

    @Override
    public Builder join(AlgNode node, String alias, JoinType type, InputRef left, InputRef right) {
      alias = inferAlias(node, alias);
      inputs.add(node);
      inputAliases.add(alias);
      joinTypes.add(type);
      joins.add(Pair.of(left, right));
      return this;
    }

    @Override
    public Builder filter(Expr predicate) {
      if (!predicate.isPredicate())
        throw new IllegalArgumentException("not a predicate " + predicate);

      filters = predicate;
      return this;
    }

    @Override
    public SPJNode build() {
      return build(false);
    }

    @Override
    public SPJNode build(boolean compileExpr) {
      if (joins.size() != inputs.size() - 1)
        throw new IllegalArgumentException(
            "unexpected #inputs, expected: " + (joins.size() + 1) + ", got: " + inputs);

      assert inputs.size() == inputAliases.size();
      assert projExpr.size() == projAliases.size();

      final List<ProjectionItem> projections = new ArrayList<>(projExpr.size());
      for (int i = 0; i < projExpr.size(); i++) {
        final Expr expr = projExpr.get(i);
        final String alias = projAliases.get(i);
        projections.add(
            makeProjection(compileExpr ? expr.compile(inputs, inputAliases) : expr, alias));
      }

      final List<Expr> joinConditions = new ArrayList<>(joins.size());
      for (Pair<InputRef, InputRef> join : joins) {
        final Expr expr = makeJoinKey(join.getLeft(), join.getRight());
        joinConditions.add(compileExpr ? expr.compile(inputs, inputAliases) : expr);
      }

      if (filters != null && compileExpr) filters = filters.compile(inputs, inputAliases);

      return new SPJNodeImpl(inputs, inputAliases, projections, joinTypes, joinConditions, filters);
    }

    private static String inferAlias(AlgNode node, String alias) {
      if (alias != null) return alias;
      if (node instanceof TableNode) return ((TableNode) node).table().name();
      return null;
    }

    private ProjectionItem makeProjection(Expr expr, String alias) {
      if (expr instanceof InputRef)
        return ProjectionItem.create(((InputRef) expr).locateIn(inputs, inputAliases), alias);
      else throw new IllegalArgumentException("unsupported projection expr: " + expr);
    }

    private Expr makeJoinKey(InputRef left, InputRef right) {
      return Expr.binary(left, Operator.EQ, right);
    }
  }
}
