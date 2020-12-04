package sjtu.ipads.wtune.solver.core.impl;

import com.microsoft.z3.*;
import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.core.Variable;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.schema.Schema;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Z3SolverContext implements SolverContext {
  private final Context z3;

  private final Map<String, TupleSet> tuples = new HashMap<>();
  private final Map<AlgNode, List<SymbolicColumnRef>> columns = new IdentityHashMap<>();
  private final List<BoolExpr> preconditions = new ArrayList<>();

  private Z3SolverContext(Context z3) {
    this.z3 = z3;
  }

  public static Z3SolverContext create() {
    return new Z3SolverContext(new Context());
  }

  @Override
  public Variable const_(DataType dataType, Object value) {
    // TODO
    return Variable.wrap(z3.mkInt(1), value.toString());
  }

  @Override
  public Constraint boolConst(boolean value) {
    return value ? null : Constraint.wrap(z3.mkFalse(), "false");
  }

  @Override
  public Constraint ite(Constraint cond, Constraint v0, Constraint v1) {
    if (cond == null) return v0;
    if (v0 == null && v1 == null) return null;

    return Constraint.wrap(
        z3.mkITE(unwrap(cond), unwrap(v0), unwrap(v1)).simplify(),
        "ite("
            + cond.name()
            + ","
            + (v0 == null ? "true" : v0.name())
            + ","
            + (v1 == null ? "true" : v1.name()));
  }

  @Override
  public Constraint and(Iterable<Constraint> constraints) {
    BoolExpr constraint = null;

    final List<String> desc = new ArrayList<>();
    for (Constraint c : constraints) {
      if (c == null) continue;
      if (constraint == null) constraint = unwrap(c);
      else constraint = z3.mkAnd(constraint, unwrap(c));
      desc.add(c.name());
    }

    return constraint == null
        ? null
        : Constraint.wrap(
            constraint.simplify(),
            desc.size() == 1 ? desc.get(0) : "(" + String.join(") and (", desc) + ")");
  }

  @Override
  public Constraint or(Iterable<Constraint> constraints) {
    BoolExpr constraint = null;

    final List<String> desc = new ArrayList<>();
    for (Constraint c : constraints) {
      if (c == null) continue;
      if (constraint == null) constraint = unwrap(c);
      else constraint = z3.mkOr(constraint, unwrap(c));
      desc.add(c.name());
    }

    return constraint == null
        ? null
        : Constraint.wrap(
            constraint.simplify(),
            desc.size() == 1 ? desc.get(0) : "(" + String.join(") or (", desc) + ")");
  }

  @Override
  public Constraint eq(Variable left, Variable right) {
    return Constraint.wrap(
        z3.mkEq(unwrap(left), unwrap(right)).simplify(), left.name() + "=" + right.name());
  }

  @Override
  public Constraint eq(Constraint left, Constraint right) {
    if (left == null) return right;
    if (right == null) return left;
    return Constraint.wrap(
        z3.mkEq(unwrap(left), unwrap(right)).simplify(), left.name() + "=" + right.name());
  }

  @Override
  public Constraint convert(Variable variable) {
    final Expr v = unwrap(variable);
    return v instanceof BoolExpr ? Constraint.wrap(v, variable.name()) : null;
  }

  @Override
  public List<SymbolicColumnRef> tupleSourceOf(AlgNode node) {
    return columns.get(node);
  }

  @Override
  public boolean checkEquivalence(Schema schema, AlgNode q0, AlgNode q1) {
    register(schema);
    register(q0.setNamespace("0").setSolverContext(this));
    register(q1.setNamespace("1").setSolverContext(this));

    final TupleSet filtered0 = tupleSetOf(q0).describe(q0.filtered());
    final TupleSet filtered1 = tupleSetOf(q1).describe(q1.filtered());

    final Solver solver = z3.mkSolver();

    preconditions.forEach(solver::add);

    final List<SymbolicColumnRef> outputs0 = q0.projected(), outputs1 = q1.projected();

    Constraint colMatchCond = null;
    for (int i = 0; i < outputs0.size(); i++) {
      final SymbolicColumnRef o0 = outputs0.get(i), o1 = outputs1.get(i);
      colMatchCond =
          and(
              colMatchCond,
              or( // (n0 & n1) || (n0 = n1 & v0 = v1)
                  and(o0.notNull(), o0.notNull()),
                  and(eq(o0.notNull(), o1.notNull()), eq(o0.variable(), o1.variable()))));
    }

    return check(solver, filtered0.match(filtered1, colMatchCond))
        && check(solver, filtered1.match(filtered0, colMatchCond));
  }

  private void register(AlgNode q) {
    final String namespace = q.namespace();
    final TupleSet tupleSet = new TupleSet(columnsOf(inputsOf(q)));

    tuples.put(namespace, tupleSet);

    final Map<AlgNode, List<SymbolicColumnRef>> columnsMap =
        tupleSet.tuple().stream().collect(Collectors.groupingBy(it -> it.columnRef().owner()));

    this.columns.putAll(columnsMap);
  }

  private void register(Schema schema) {
    preconditions.clear();

    for (var fk : schema.foreignKeys().entrySet()) {
      // TODO: support multi-columns FK in the future
      final Column kCol = fk.getKey().get(0), vCol = fk.getValue().get(0);
      final Sort kSort = sortOf(kCol.dataType()), vSort = sortOf(vCol.dataType());
      final Expr k = z3.mkConst("x", kSort), v = z3.mkConst("y", vSort);

      final ArrayExpr kSrc = setOf(columnSourceOf(kCol.toString()), kSort);
      final ArrayExpr vSrc = setOf(columnSourceOf(vCol.toString()), vSort);

      preconditions.add(
          forAll(k, containsBy(kSrc, k), exists(v, containsBy(vSrc, v), z3.mkEq(k, v))));
    }
  }

  private boolean check(Solver solver, BoolExpr toCheck) {
    solver.push();
    solver.add(z3.mkNot(toCheck));
    final Status result = solver.check();
    solver.pop();
    return result == Status.UNSATISFIABLE;
  }

  private Sort sortOf(DataType dataType) {
    return z3.mkIntSort(); // TODO
  }

  private ArrayExpr setOf(String name, Sort sort) {
    return z3.mkArrayConst(name, sort, z3.getBoolSort());
  }

  private TupleSet tupleSetOf(AlgNode node) {
    return tuples.get(node.namespace());
  }

  private BoolExpr containsBy(ArrayExpr set, Expr e) {
    return (BoolExpr) z3.mkSelect(set, e);
  }

  private Quantifier forAll(List<Expr> exprs, BoolExpr cond, BoolExpr implication) {
    return z3.mkForall(
        exprs.toArray(Expr[]::new), z3.mkImplies(cond, implication), 1, null, null, null, null);
  }

  private Quantifier forAll(Expr exprs, BoolExpr cond, BoolExpr implication) {
    return z3.mkForall(asArray(exprs), z3.mkImplies(cond, implication), 1, null, null, null, null);
  }

  private Quantifier exists(List<Expr> exprs, BoolExpr... cond) {
    return z3.mkExists(exprs.toArray(Expr[]::new), z3.mkAnd(cond), 1, null, null, null, null);
  }

  private Quantifier exists(Expr exprs, BoolExpr... cond) {
    return z3.mkExists(asArray(exprs), z3.mkAnd(cond), 1, null, null, null, null);
  }

  private Expr unwrap(Variable v) {
    return v.unwrap(Expr.class);
  }

  private BoolExpr unwrap(Constraint c) {
    return c == null ? z3.mkTrue() : c.unwrap(BoolExpr.class);
  }

  private static String notNullIndicatorOf(String columnName) {
    return columnName + "?";
  }

  private static String columnSourceOf(String columnName) {
    return columnName + ".src";
  }

  private static String withNamespace(String name, String namespace) {
    return name + "." + namespace;
  }

  private static List<TableNode> inputsOf(AlgNode n) {
    if (n instanceof TableNode) return singletonList(((TableNode) n));
    return n.inputs().stream()
        .map(Z3SolverContext::inputsOf)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static List<ColumnRef> columnsOf(Collection<TableNode> inputs) {
    return inputs.stream()
        .map(TableNode::columns)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private class TupleSet {
    private final List<SymbolicColumnRef> tuple;
    private final Constraint constraint;
    private final List<Expr> tupleExprs;

    private TupleSet(List<SymbolicColumnRef> tuple, Constraint constraint, List<Expr> tupleExprs) {
      this.tuple = tuple;
      this.constraint = constraint;
      this.tupleExprs = tupleExprs;
    }

    private TupleSet(List<ColumnRef> columns) {
      if (columns.isEmpty())
        throw new IllegalArgumentException("input columns should not be empty");

      final String namespace = columns.get(0).owner().namespace();

      final List<SymbolicColumnRef> refs = new ArrayList<>(columns.size());
      final List<Expr> tupleExprs = new ArrayList<>(columns.size() << 1);
      for (final ColumnRef col : columns) {
        final Sort sort = sortOf(col.dataType());
        final String sourceName = columnSourceOf(col.toString());
        final String scopedColName = withNamespace(col.toString(), namespace);
        final String notNullCond = withNamespace(notNullIndicatorOf(scopedColName), namespace);
        final String sourceCond = col.toString() + "!";

        final ArrayExpr src = setOf(sourceName, sort);
        final Expr v = z3.mkConst(scopedColName, sort);
        final BoolExpr c = (BoolExpr) z3.mkSelect(src, v);
        final BoolExpr n = z3.mkBoolConst(notNullCond);

        refs.add(
            SymbolicColumnRef.create(
                    Variable.wrap(v, scopedColName),
                    col.notNull() ? null : Constraint.wrap(n, notNullCond),
                    Constraint.wrap(c, sourceCond))
                .setColumnRef(col));

        tupleExprs.add(v);
        if (!col.notNull()) tupleExprs.add(n);

        final Expr i = z3.mkConst("i", sort);
        preconditions.add(exists(i, containsBy(src, i)));
      }

      this.tuple = refs;
      this.constraint = null;
      this.tupleExprs = tupleExprs;
    }

    private TupleSet describe(List<SymbolicColumnRef> refs) {
      final Constraint c = and(listMap(SymbolicColumnRef::condition, refs));
      return new TupleSet(refs, c, tupleExprs);
    }

    public List<SymbolicColumnRef> tuple() {
      return tuple;
    }

    public List<Expr> tupleExprs() {
      return tupleExprs;
    }

    public Constraint constraint() {
      return constraint;
    }

    public BoolExpr match(TupleSet other, Constraint matcher) {
      return forAll(
          tupleExprs(),
          unwrap(constraint()),
          exists(other.tupleExprs(), unwrap(other.constraint()), unwrap(matcher)));
    }

    @Override
    public String toString() {
      return tuple.toString();
    }
  }
}
