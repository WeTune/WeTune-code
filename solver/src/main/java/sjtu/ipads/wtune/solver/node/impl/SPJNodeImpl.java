package sjtu.ipads.wtune.solver.node.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
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
import sjtu.ipads.wtune.solver.sql.expr.QueryExpr;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.solver.sql.expr.ExprVisitor.seeker;

public class SPJNodeImpl extends BaseAlgNode implements SPJNode {
  private final boolean forceDistinct;
  private final List<String> inputAliases;
  private final List<ProjectionItem> projections;
  private final Expr filters;

  private final List<JoinType> joinTypes; // the length must be selections.size() - 1
  private final List<Expr> joinConditions;

  private final List<Expr> orderKeyExprs;

  //// for cache
  private List<AlgNode> subquery;

  private List<ColumnRef> outCols;
  private List<SymbolicColumnRef> filteredCols;
  private List<SymbolicColumnRef> projectedCols;

  // mapping that input column -> output column
  private Map<ColumnRef, SymbolicColumnRef> projectRel;
  private Set<Set<SymbolicColumnRef>> uniqueCores;
  private Boolean isSingletonOutput = null;

  private List<SymbolicColumnRef> orderKeys;

  private SPJNodeImpl(
      boolean forceDistinct,
      List<AlgNode> inputs,
      List<String> inputAliases,
      List<ProjectionItem> projections,
      List<JoinType> joinTypes,
      List<Expr> joinConditions,
      Expr filters,
      List<Expr> orderKeys) {
    super(forceDistinct, inputs);
    this.forceDistinct = forceDistinct;
    this.inputAliases = inputAliases;
    this.projections = projections;
    this.joinTypes = joinTypes;
    this.joinConditions = joinConditions;
    this.filters = filters;
    this.orderKeyExprs = orderKeys;
  }

  public static Builder builder() {
    return new BuilderImpl();
  }

  @Override
  public List<String> inputAliases() {
    return inputAliases;
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

  @Override
  public List<ColumnRef> columns() {
    if (outCols != null) return outCols;

    outCols = listMap(it -> it.projectOn(inputs(), inputAliases(), inputColumns()), projections);
    outCols.forEach(it -> it.setOwner(this));
    return outCols;
  }

  @Override
  public List<SymbolicColumnRef> filtered() {
    if (filteredCols != null) return filteredCols;

    // first build the join conditions
    final List<SymbolicColumnRef> refs = new ArrayList<>(inputColumns().size());
    refs.addAll(listMap(SymbolicColumnRef::copy, inputs().get(0).projected()));

    for (int i = 1; i < inputs().size(); i++) {
      final JoinType joinType = joinTypes().get(i - 1);
      final Constraint joinCond = applyPredicate(joinConditions().get(i - 1));
      final List<SymbolicColumnRef> rightCols =
          listMap(SymbolicColumnRef::copy, inputs().get(i).projected());

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

    return filteredCols = refs;
  }

  @Override
  public List<SymbolicColumnRef> projected() {
    if (projectedCols != null) return projectedCols;

    final List<ColumnRef> cols = columns();

    final List<SymbolicColumnRef> projected =
        listMap(it -> it.projectOn(inputs(), inputAliases(), filtered(), ctx), projections());

    // record the mapping between input and output column
    final Map<ColumnRef, SymbolicColumnRef> projectRel = new HashMap<>();
    projected.forEach(it -> projectRel.put(it.columnRef(), it));
    this.projectRel = projectRel;

    for (int i = 0; i < projected.size(); i++) projected.get(i).setColumnRef(cols.get(i));
    return this.projectedCols = projected;
  }

  @Override
  public Set<Set<SymbolicColumnRef>> uniqueCores() {
    if (uniqueCores != null) return uniqueCores;

    final Set<Set<SymbolicColumnRef>> uniqueCores = new HashSet<>();
    for (var subCore : Sets.cartesianProduct(listMap(AlgNode::uniqueCores, inputs()))) {
      // after join, the new unique cores are (all possible) concatenation of both sides cores
      //   in this step, replace the input sym ref with the internal filtered ref (findFiltered)
      Set<SymbolicColumnRef> newCore =
          subCore.stream().map(this::findFiltered).reduce(Sets::union).orElseThrow();
      Set<SymbolicColumnRef> newCore2 = null;

      // To handle a.uk = b.uk. the merged core <a.uk, b.uk> should be split as <a.uk>, <b.uk>
      // for now only handle single pair (i.e. ignore a.uk_p1 = b.uk_p1 && a.uk_p2 ==b.uk_p2)
      // TODO: support multi-keys UK
      if (newCore.size() == 2) {
        final Iterator<SymbolicColumnRef> iter = newCore.iterator();
        final SymbolicColumnRef left = iter.next(), right = iter.next();
        if (ctx.inferColumnEqs(left, right)) {
          newCore = newHashSet(left);
          newCore2 = newHashSet(right);
        }
      }

      // filter out those are not projected
      if (newCore.stream().allMatch(this::isProjected)) uniqueCores.add(newCore);
      if (newCore2 != null && newCore2.stream().allMatch(this::isProjected))
        uniqueCores.add(newCore2);
    }

    for (Set<SymbolicColumnRef> uniqueCore : uniqueCores) {
      // reduce unique core, remove columns that are inferred to be fixed value
      // e.g. we have UK <c0, c1>
      // with predicate "c0 = 10", c1 itself is guaranteed to be unique, so remove c0
      uniqueCore.removeIf(ctx::inferFixedValue);
      if (uniqueCore.isEmpty()) {
        // if all column in a unique core is inferred to be fixed value
        // then there must be only single tuple that match the condition
        isSingletonOutput = true;
        break;
      }
    }

    return this.uniqueCores = uniqueCores;
  }

  @Override
  public boolean isSingletonOutput() {
    if (isSingletonOutput != null) return isSingletonOutput;
    uniqueCores(); // trigger lazy calculation
    if (isSingletonOutput == null) isSingletonOutput = false;
    return isSingletonOutput;
  }

  @Override
  public List<SymbolicColumnRef> orderKeys() {
    if (orderKeys != null) return orderKeys;
    return orderKeys =
        listMap(it -> it.asVariable(inputs(), inputAliases(), filtered(), ctx), orderKeyExprs);
  }

  @Override
  public boolean isForcedUnique() {
    return forceDistinct;
  }

  @Override
  public Iterable<AlgNode> inputsAndSubquery() {
    if (subquery != null) return Iterables.concat(inputs(), subquery);
    final List<AlgNode> subquery = new ArrayList<>();
    if (filters != null)
      filters.acceptVisitor(seeker(QueryExpr.class, it -> subquery.add(it.query())));
    this.subquery = subquery;
    return Iterables.concat(inputs(), subquery);
  }

  private Map<ColumnRef, SymbolicColumnRef> projectRel() {
    if (projectRel != null) return projectRel;
    projected(); // trigger lazy calculation of projectRel
    return projectRel;
  }

  // cache
  private Map<ColumnRef, List<SymbolicColumnRef>> lookUp;

  private Set<SymbolicColumnRef> findFiltered(Set<SymbolicColumnRef> refs) {
    if (lookUp == null)
      lookUp = filtered().stream().collect(Collectors.groupingBy(SymbolicColumnRef::columnRef));

    final Set<SymbolicColumnRef> found = new HashSet<>();
    for (SymbolicColumnRef ref : refs) {
      final List<SymbolicColumnRef> matched = lookUp.get(ref.columnRef());
      if (matched == null || matched.size() != 1)
        throw new IllegalStateException("unexpected column matching");

      found.add(matched.get(0));
    }

    return found;
  }

  private boolean isProjected(SymbolicColumnRef inputRef) {
    return projectRel().containsKey(inputRef.columnRef());
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
    if (isForcedUnique()) builder.append(" DISTINCT");

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

    if (!orderKeyExprs.isEmpty())
      builder
          .append('\n')
          .append(" ".repeat(indentLevel))
          .append("ORDER BY ")
          .append(String.join(", ", listMap(Objects::toString, orderKeyExprs)));

    if (indentLevel != 0) builder.append(')');

    return builder.toString();
  }

  private String toString1(int indentLevel) {
    final StringBuilder builder = new StringBuilder();
    if (indentLevel != 0) builder.append("(");
    builder.append("SELECT");
    if (isForcedUnique()) builder.append(" DISTINCT");

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

    if (!orderKeyExprs.isEmpty())
      builder
          .append(" ORDER BY ")
          .append(String.join(", ", listMap(Objects::toString, orderKeyExprs)));

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
    private final List<Expr> orderKeys = new ArrayList<>();
    private boolean forceDistinct;
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
    public Builder forceUnique(boolean forceDistinct) {
      this.forceDistinct = forceDistinct;
      return this;
    }

    @Override
    public Builder orderBy(Expr... orderKeys) {
      this.orderKeys.addAll(Arrays.asList(orderKeys));
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

      return new SPJNodeImpl(
          forceDistinct,
          inputs,
          inputAliases,
          projections,
          joinTypes,
          joinConditions,
          filters,
          orderKeys);
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
