package sjtu.ipads.wtune.solver.core.impl;

import com.google.common.collect.Sets;
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

import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

public class Z3SolverContext implements SolverContext {
  private final Context z3;
  private final Solver solver;

  private final Map<String, List<TupleSet>> tuplesMap = new HashMap<>();
  private final Set<BoolExpr> preconditions = new HashSet<>();

  private Z3SolverContext(Context z3) {
    this.z3 = z3;
    this.solver = z3.mkSolver();
  }

  public static Z3SolverContext create() {
    return new Z3SolverContext(new Context());
  }

  @Override
  public Variable const_(DataType dataType, Object value) {
    // TODO
    if (value instanceof Integer) return Variable.wrap(z3.mkInt((Integer) value), value.toString());
    else throw new UnsupportedOperationException();
  }

  @Override
  public Constraint boolConst(boolean value) {
    return value ? null : Constraint.wrap(z3.mkFalse(), "false");
  }

  @Override
  public Variable ite(Constraint cond, Variable v0, Variable v1) {
    if (cond == null) return v0;

    return Variable.wrap(
        z3.mkITE(unwrap(cond), unwrap(v0), unwrap(v1)),
        "ite(" + cond.name() + "," + v0.name() + "," + v1.name());
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
  public Constraint implies(Constraint c0, Constraint c1) {
    if (c0 == null) return c1;
    if (c1 == null) return null;
    return Constraint.wrap(
        z3.mkImplies(unwrap(c0), unwrap(c1)).simplify(),
        "(" + c0.name() + ") => (" + c1.name() + ")");
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
  public List<SymbolicColumnRef> symbolicColumnsOf(AlgNode node) {
    return tupleSourceOf(node).tuple();
  }

  @Override
  public boolean checkUnique(AlgNode q0, AlgNode q1) {
    installContext(q0, "0");
    installContext(q1, "1");

    return (q0.isForcedUnique() && q1.isForcedUnique())
        || (q0.isForcedUnique() || q0.isInferredUnique())
            && (q1.isForcedUnique() || q1.isInferredUnique());
  }

  @Override
  public boolean checkOrder(AlgNode q0, AlgNode q1) {
    installContext(q0, "0");
    installContext(q1, "1");

    final List<SymbolicColumnRef> keys0 = q0.orderKeys(), keys1 = q1.orderKeys();
    keys0.removeIf(this::inferFixedValue);
    keys1.removeIf(this::inferFixedValue);
    return checkColumnsMatch(q0, q1, keys0, keys1);
  }

  @Override
  public boolean checkEquivalence(AlgNode q0, AlgNode q1) {
    installContext(q0, "0");
    installContext(q1, "1");

    return checkColumnsMatch(q0, q1, q0.projected(), q1.projected());
  }

  @Override
  public boolean inferFixedValue(SymbolicColumnRef col) {
    // check whether the condition on the column implies that the column must be a fixed value
    // method: check the proposition always hold
    //  exists p: col.cond => col.v = p
    final TupleSet tupleSet = tupleSourceOf(col.columnRef().owner());

    final Expr probeVar = z3.mkConst("_p", sortOf(col.columnRef().dataType()));
    final BoolExpr probe = z3.mkEq(unwrap(col.variable()), probeVar);

    solver.reset();
    return check(
        solver, exists(probeVar, forAll(tupleSet.tupleExprs, unwrap(col.condition()), probe)));
  }

  @Override
  public boolean inferColumnEqs(SymbolicColumnRef c0, SymbolicColumnRef c1) {
    final BoolExpr eqCond =
        z3.mkOr(
            z3.mkNot(unwrap(c1.notNull())), z3.mkEq(unwrap(c0.variable()), unwrap(c1.variable())));
    // columnEqs(c0, c1);

    solver.reset();
    return check(solver, z3.mkImplies(unwrap(c0.condition()), eqCond))
        && check(solver, z3.mkImplies(unwrap(c1.condition()), eqCond));
  }

  @Override
  public SolverContext register(Schema schema) {
    preconditions.clear();

    for (var fk : schema.foreignKeys().entrySet()) {
      // TODO: support multi-columns FK in the future
      final Column kCol = fk.getKey().get(0), vCol = fk.getValue().get(0);
      final Sort kSort = sortOf(kCol.dataType()), vSort = sortOf(vCol.dataType());
      final Expr k = z3.mkConst("x", kSort), v = z3.mkConst("y", vSort);

      final ArrayExpr kSrc = setExprOf(nameColumnSource(kCol.toString()), kSort);
      final ArrayExpr vSrc = setExprOf(nameColumnSource(vCol.toString()), vSort);

      preconditions.add(
          forAll(k, belongsTo(k, kSrc), exists(v, belongsTo(v, vSrc), z3.mkEq(k, v))));
    }

    return this;
  }

  private void installContext(AlgNode q, String namespace) {
    q.setNamespace(namespace).setSolverContext(this);
  }

  private boolean check(Solver solver, BoolExpr toCheck) {
    solver.push();
    solver.add(z3.mkNot(toCheck));
    final Status result = solver.check();
    solver.pop();
    return result == Status.UNSATISFIABLE;
  }

  private boolean checkColumnsMatch(
      AlgNode q0, AlgNode q1, List<SymbolicColumnRef> cols0, List<SymbolicColumnRef> cols1) {

    if (cols0.size() != cols1.size()) return false;
    if (cols0.isEmpty()) return true;

    solver.reset();
    preconditions.forEach(solver::add);

    final BoolExpr[] colMatchConds = new BoolExpr[cols0.size()];
    for (int i = 0; i < cols0.size(); i++) {
      final SymbolicColumnRef o0 = cols0.get(i), o1 = cols1.get(i);
      colMatchConds[i] = columnEqs(o0, o1);
    }

    final BoolExpr colMatchCond = z3.mkAnd(colMatchConds);
    final TupleSet filtered0 = tupleSetOf(q0), filtered1 = tupleSetOf(q1);

    return check(solver, filtered0.match(filtered1, colMatchCond))
        && check(solver, filtered1.match(filtered0, colMatchCond));
  }

  private Sort sortOf(DataType dataType) {
    return z3.mkIntSort(); // TODO
  }

  private ArrayExpr setExprOf(String name, Sort sort) {
    return z3.mkArrayConst(name, sort, z3.getBoolSort());
  }

  private TupleSet tupleSetOf(AlgNode node) {
    return tupleSourceOf(node).describe(node.filtered());
  }

  private TupleSet tupleSourceOf(AlgNode node) {
    if (!(node instanceof TableNode))
      return stream(node.inputsAndSubquery())
          .map(this::tupleSourceOf)
          .reduce(TupleSet::concat)
          .orElseThrow();

    final List<TupleSet> tupleSets = tuplesMap.get(node.namespace());
    if (tupleSets != null)
      for (TupleSet tupleSet : tupleSets) if (node.equals(tupleSet.sourceNode())) return tupleSet;

    final TupleSet tupleSet = new TupleSet(node);
    tuplesMap.computeIfAbsent(node.namespace(), dumb(ArrayList::new)).add(tupleSet);
    return tupleSet;
  }

  private BoolExpr belongsTo(Expr e, ArrayExpr set) {
    return (BoolExpr) z3.mkSelect(set, e);
  }

  private BoolExpr columnEqs(SymbolicColumnRef c0, SymbolicColumnRef c1) {
    // (!n0 & !n1) || (n0 = n1 & v0 = v1)
    return (BoolExpr)
        z3.mkOr(
                z3.mkAnd(z3.mkNot(unwrap(c0.notNull())), z3.mkNot(unwrap(c1.notNull()))),
                z3.mkAnd(
                    z3.mkEq(unwrap(c0.notNull()), unwrap(c1.notNull())),
                    z3.mkEq(unwrap(c0.variable()), unwrap(c1.variable()))))
            .simplify();
  }

  private Quantifier forAll(Collection<Expr> exprs, BoolExpr cond, BoolExpr implication) {
    return z3.mkForall(
        exprs.toArray(Expr[]::new), z3.mkImplies(cond, implication), 1, null, null, null, null);
  }

  private Quantifier forAll(Expr exprs, BoolExpr cond, BoolExpr implication) {
    return z3.mkForall(asArray(exprs), z3.mkImplies(cond, implication), 1, null, null, null, null);
  }

  private Quantifier exists(Collection<Expr> exprs, BoolExpr... cond) {
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

  private static String nameNullIndicator(String columnName) {
    return columnName + "?";
  }

  private static String nameColumnSource(String columnName) {
    return columnName + ".src";
  }

  private static String withNamespace(String name, String namespace) {
    return name + "#" + namespace;
  }

  private class TupleSet {
    private final AlgNode sourceNode;
    private final List<SymbolicColumnRef> tuple;
    private final Set<Expr> tupleExprs;

    private Constraint constraint;

    private TupleSet(AlgNode sourceNode, List<SymbolicColumnRef> tuple, Set<Expr> tupleExprs) {
      this.sourceNode = sourceNode;
      this.tuple = tuple;
      this.tupleExprs = tupleExprs;
    }

    private TupleSet(AlgNode node) {
      final List<ColumnRef> columns = node.columns();
      if (columns.isEmpty())
        throw new IllegalArgumentException("input columns should not be empty");

      final String namespace = columns.get(0).owner().namespace();

      final List<SymbolicColumnRef> refs = new ArrayList<>(columns.size());
      final Set<Expr> tupleExprs = new LinkedHashSet<>(columns.size() << 1);

      BoolExpr cond = z3.mkTrue();
      for (final ColumnRef col : columns) {
        final Sort sort = sortOf(col.dataType());
        final String sourceName = nameColumnSource(col.toString());
        final String scopedColName = withNamespace(col.toString(), namespace);
        final String notNullCond = withNamespace(nameNullIndicator(scopedColName), namespace);

        final ArrayExpr src = setExprOf(sourceName, sort);
        final Expr v = z3.mkConst(scopedColName, sort);
        final BoolExpr n = z3.mkBoolConst(notNullCond);
        final BoolExpr c = (BoolExpr) z3.mkSelect(src, v);
        cond = z3.mkAnd(cond, c);

        refs.add(
            SymbolicColumnRef.create(
                    Variable.wrap(v, scopedColName),
                    col.notNull() ? null : Constraint.wrap(n, notNullCond),
                    null)
                .setColumnRef(col));

        tupleExprs.add(v);
        if (!col.notNull()) tupleExprs.add(n);

        final Expr i = z3.mkConst("i", sort);
        preconditions.add(exists(i, belongsTo(i, src)));
      }

      final Constraint srcConstraint = Constraint.wrap(cond, "src!");
      refs.forEach(it -> it.setCondition(srcConstraint));

      this.sourceNode = node;
      this.tuple = refs;
      this.constraint = null;
      this.tupleExprs = tupleExprs;
    }

    private TupleSet describe(List<SymbolicColumnRef> refs) {
      return new TupleSet(null, refs, tupleExprs);
    }

    public AlgNode sourceNode() {
      return sourceNode;
    }

    public List<SymbolicColumnRef> tuple() {
      return tuple;
    }

    public Set<Expr> tupleExprs() {
      return tupleExprs;
    }

    public Constraint constraint() {
      if (constraint != null) return constraint;
      return constraint = and(listMap(SymbolicColumnRef::condition, tuple));
    }

    public TupleSet concat(TupleSet other) {
      return new TupleSet(
          null,
          listConcat(this.tuple(), other.tuple()),
          Sets.union(this.tupleExprs(), other.tupleExprs()));
    }

    public BoolExpr match(TupleSet other, BoolExpr matcher) {
      return forAll(
          tupleExprs(),
          unwrap(constraint()),
          exists(other.tupleExprs(), unwrap(other.constraint()), matcher));
    }

    @Override
    public String toString() {
      return tuple.toString();
    }
  }
}
