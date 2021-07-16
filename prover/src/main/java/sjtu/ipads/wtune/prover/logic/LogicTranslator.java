package sjtu.ipads.wtune.prover.logic;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.Commons.permutation;
import static sjtu.ipads.wtune.common.utils.FuncUtils.arrayMap;
import static sjtu.ipads.wtune.common.utils.FuncUtils.generate;
import static sjtu.ipads.wtune.common.utils.FuncUtils.none;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipMap;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import sjtu.ipads.wtune.common.utils.Cascade;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.EqPredTerm;
import sjtu.ipads.wtune.prover.uexpr.Name;
import sjtu.ipads.wtune.prover.uexpr.TableTerm;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;
import sjtu.ipads.wtune.prover.uexpr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.uexpr.Var;
import sjtu.ipads.wtune.prover.utils.Constants;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

public class LogicTranslator {
  private static final String TUPLE_TYPE_PREFIX = "Tup_";
  private static final String TABLE_FUNC_PREFIX = "t_";
  private static final String UNINTER_FUNC_PREFIX = "g";
  private static final String MAIN_FUNC_PREFIX = "f";
  private static final String CONSTANT_PREFIX = "k";
  private static final String FREE_VAL_PREFIX = "v";
  private static final Var FREE_VAR = Var.mkBase(Constants.FREE_VAR);

  private final LogicCtx ctx;
  private final List<Proposition> assertions;

  private final Map<Var, DataType> varTypes;
  private final Map<Var, Value> varValues;
  private final Map<Name, Func> tableFuncs;
  private final Map<Name, Func> uninterFuncs;
  private final Map<Name, Value> constVals;
  private final Multimap<DataType, Value> freeVals;

  private Func asIntFunc;
  private Disjunction disjunction;
  private VarUsage varUsage;
  private int nextTmpTableId, nextTmpFuncId;

  private LogicTranslator(LogicCtx ctx) {
    this.ctx = ctx;
    this.assertions = new ArrayList<>();

    this.tableFuncs = new HashMap<>(8);
    this.varTypes = new HashMap<>(8);
    this.varValues = new HashMap<>(8);
    this.uninterFuncs = new HashMap<>(2);
    this.constVals = new HashMap<>(2);
    this.freeVals = MultimapBuilder.hashKeys(8).arrayListValues(2).build();
  }

  public static LogicTranslator mk(LogicCtx ctx) {
    return new LogicTranslator(ctx);
  }

  public Value translate(Disjunction d) {
    this.varTypes.clear();
    this.varValues.clear();
    this.disjunction = d;
    this.varUsage = VarUsage.mk(d);

    return translateBag(d);
  }

  public Proposition translate(Constraint constraint) {
    assert constraint.type() == ConstraintType.FOREIGN;
    final String ownerTable = constraint.columns().get(0).tableName();
    final String refTable = constraint.refColumns().get(0).tableName();
    final Func ownerTableFunc = tableFuncs.get(Name.mk(ownerTable));
    final Func refTableFunc = tableFuncs.get(Name.mk(refTable));
    final DataType xType = ownerTableFunc.paramTypes()[0];
    final DataType yType = refTableFunc.paramTypes()[0];
    final Value x = ctx.mkVal("x", xType);
    final Value y = ctx.mkVal("y", yType);
    final Value X = ownerTableFunc.apply(x);
    final Value Y = refTableFunc.apply(y);
    final List<Proposition> eqConds =
        zipMap(
            constraint.columns(),
            constraint.refColumns(),
            (xCol, yCol) ->
                xType.accessor(xCol.name()).apply(x).eq(yType.accessor(yCol.name()).apply(y)));
    final Value eqCond = ctx.mkProduct(eqConds.toArray(Proposition[]::new));
    return ctx.mkForall(x, X.gt(0).implies(ctx.mkExists(y, Y.mul(eqCond).gt(0))));
  }

  public List<Proposition> assertions() {
    return assertions;
  }

  private DataType mkTupleType(Var v) {
    if (!v.isBase()) throw new IllegalArgumentException();

    final DataType type = varTypes.get(v);
    if (type != null) return type;

    final Name table = varUsage.tableOf(v);
    final String typeName;

    if (table != null) typeName = TUPLE_TYPE_PREFIX + table;
    else if (v.equals(FREE_VAR)) typeName = TUPLE_TYPE_PREFIX + "out";
    else typeName = mkTmpTableName();

    final Set<Name> memberSet = table != null ? varUsage.usageOf(table) : varUsage.usageOf(v);
    final String[] members = arrayMap(memberSet, Object::toString, String.class);

    final DataType dataType = ctx.mkTupleType(typeName, members);
    varTypes.put(v, dataType);
    return dataType;
  }

  private Func mkTableFunc(TableTerm t) {
    return tableFuncs.computeIfAbsent(
        t.name(), n -> ctx.mkFunc(TABLE_FUNC_PREFIX + n, ctx.mkIntType(), mkTupleType(t.var())));
  }

  private Value mkVal(Var v) {
    final Name name = v.name();

    if (v.isBase()) {
      return varValues.computeIfAbsent(v, ignored -> ctx.mkVal(name.toString(), mkTupleType(v)));
    }

    if (v.isProjected()) {
      final Var base = v.base()[0];
      assert base.isBase();
      final DataType dataType = mkTupleType(base);
      final Value val = mkVal(base);
      final Func accessor = dataType.accessor(name.toString());
      return accessor.apply(val);
    }

    if (v.isFunc()) {
      final Var[] args = v.base();
      assert none(asList(args), Var::isBase);
      final Func func = mkUninterFunc(name, args.length, ctx.mkIntType());
      final Value[] vals = arrayMap(args, this::mkVal, Value.class);
      return func.apply(vals);
    }

    if (v.isConstant()) {
      final String valName = CONSTANT_PREFIX + constVals.size();
      return constVals.computeIfAbsent(name, it -> ctx.mkIntVal(valName));
    }

    return assertFalse();
  }

  private Value mkFreeVal(DataType type) {
    return ctx.mkVal(FREE_VAL_PREFIX + freeVals.size(), type);
  }

  private Func mkUninterFunc(Name desc, int numArgs, DataType retType) {
    final Func existing = uninterFuncs.get(desc);
    if (existing != null) return existing;

    final String funcName = UNINTER_FUNC_PREFIX + uninterFuncs.size();
    final DataType[] argTypes = generate(numArgs, i -> ctx.mkIntType(), DataType.class);
    final Func func = ctx.mkFunc(funcName, retType, argTypes);
    uninterFuncs.put(desc, func);

    return func;
  }

  private Func mkMainFunc(DataType retType, DataType... argTypes) {
    return ctx.mkFunc(mkMainFuncName(), retType, argTypes);
  }

  private Func mkAsIntFunc() {
    if (asIntFunc != null) return asIntFunc;
    asIntFunc = ctx.mkFunc("as_int", ctx.mkIntType(), ctx.mkBoolType());

    final Proposition x = ctx.mkBoolVal("x");
    final Value lhs = asIntFunc.apply(x);
    final Value rhs = x.ite(ctx.mkConst(1), ctx.mkConst(0));
    assertions.add(ctx.mkForall(x, lhs.eq(rhs)));

    return asIntFunc;
  }

  private String mkTmpTableName() {
    return TUPLE_TYPE_PREFIX + nextTmpTableId++;
  }

  private String mkMainFuncName() {
    return MAIN_FUNC_PREFIX + nextTmpFuncId++;
  }

  private Iterable<Value[]> mkMainFuncArgs(DataType... argTypes) {
    return () -> new ArgsPermutation(argTypes);
  }

  private Value translateTable(TableTerm tableTerm) {
    // Each table creates
    // 1. a tuple type
    // 2. a bounded variable
    // 3. a table function
    // 4. a term that applies the function to the variable
    assert tableTerm.var().isBase();

    final Func func = mkTableFunc(tableTerm);
    final Value v = mkVal(tableTerm.var());

    return func.apply(v);
  }

  private Proposition translatePred(UExpr pred) {
    if (pred.kind() == Kind.EQ_PRED) return translateEqPred((EqPredTerm) pred);
    else return translateUninterPred((UninterpretedPredTerm) pred);
  }

  private Proposition translateEqPred(EqPredTerm eqPred) {
    // Each eq-pred creates
    // 1. (= lhs rhs)
    // 2. (optional) constants
    assert !eqPred.lhs().isBase() && !eqPred.rhs().isBase();
    return mkVal(eqPred.lhs()).eq(mkVal(eqPred.rhs()));
  }

  private Proposition translateUninterPred(UninterpretedPredTerm p) {
    // Each uninterpreted pred creates
    // 1. a uninterpreted predicate that takes N integers as parameters
    // 2. a term apply the predicate to the arguments
    // 3. (optional) constants
    final Var[] vars = p.vars();
    assert none(asList(vars), Var::isBase);
    final Func func = mkUninterFunc(p.name(), vars.length, ctx.mkBoolType());
    final Value[] args = arrayMap(vars, LogicTranslator.this::mkVal, Value.class);
    return ((Proposition) func.apply(args));
  }

  private Value translateBag(Disjunction d) {
    final List<Value> values = new ArrayList<>(d.conjunctions().size() + 2);
    for (Conjunction c : d) {
      if (c.vars().isEmpty()) values.add(translateBag0(c));
      else values.add(translateBag1(c));
    }

    return ctx.mkSum(values.toArray(Value[]::new));
  }

  private Value translateBag0(Conjunction c) {
    final List<Value> values = new ArrayList<>();

    c.tables().forEach(it -> values.add(translateTable((TableTerm) it)));
    c.preds().forEach(it -> values.add(mkAsIntFunc().apply(translatePred(it))));
    if (c.squash() != null) values.add(mkAsIntFunc().apply(translateSet(c.squash())));
    if (c.neg() != null) values.add(mkAsIntFunc().apply(translateSet(c.neg()).not()));

    // Product all terms.
    return ctx.mkProduct(values.toArray(Value[]::new));
  }

  private Value translateBag1(Conjunction c) {
    assert !c.vars().isEmpty();
    final Value body = translateBag0(c);
    // Declare the function.
    final DataType[] argsTypes = arrayMap(c.vars(), this::mkTupleType, DataType.class);
    final Func func = mkMainFunc(ctx.mkIntType(), argsTypes);
    // Define the function.
    final Value[] argsValues = arrayMap(c.vars(), this::mkVal, Value.class);
    final Value funcVal = func.apply(argsValues);
    assertions.add(ctx.mkForall(argsValues, funcVal.eq(body)));

    // permute the argument
    final List<Value> values = new ArrayList<>();
    for (Value[] args : mkMainFuncArgs(argsTypes)) {
      values.add(func.apply(args));
    }
    return ctx.mkSum(values.toArray(Value[]::new));
  }

  private Proposition translateSet(Disjunction d) {
    return ctx.mkDisjunction(arrayMap(d.conjunctions(), this::translateSet, Proposition.class));
  }

  private Proposition translateSet(Conjunction c) {
    final Value val = translateBag0(c);
    final Value[] vars = arrayMap(c.vars(), this::mkVal, Value.class);
    return ctx.mkExists(vars, val.gt(0));
  }

  private class ArgsPermutation implements Iterator<Value[]> {
    private final Value[] args;
    private final PermutationGroup head;
    private boolean hasNext;

    private ArgsPermutation(DataType... argTypes) {
      final int numArgs = argTypes.length;
      final Value[] args = new Value[numArgs];
      final boolean[] done = new boolean[numArgs];
      PermutationGroup head = null, tail = null;

      for (int i = 0; i < numArgs; i++) {
        if (done[i]) continue;

        final DataType iType = argTypes[i];
        final List<Value> candidates = ((List<Value>) freeVals.get(iType));
        final TIntList indices = new TIntArrayList(2);

        for (int j = i; j < numArgs; j++)
          if (argTypes[j] == iType) {
            done[j] = true;
            indices.add(j);
            if (indices.size() > candidates.size()) {
              candidates.add(mkFreeVal(iType));
            }
          }

        final PermutationGroup group = new PermutationGroup(args, indices.toArray(), candidates);
        if (head == null) head = group;
        if (tail != null) tail.setDownstream(group);
        tail = group;
      }

      this.args = args;
      this.head = head;
      this.hasNext = head.init();
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public Value[] next() {
      if (!hasNext) throw new NoSuchElementException();

      final Value[] args = Arrays.copyOf(this.args, this.args.length);
      hasNext = head.forward();
      return args;
    }
  }

  private static class PermutationGroup implements Cascade {
    private final Value[] destination;
    private final int[] indices;
    private final List<Value> candidates;
    private final Iterable<int[]> permutation;
    private Cascade downstream;
    private Iterator<int[]> iter;

    private PermutationGroup(Value[] destination, int[] indices, List<Value> candidates) {
      this.destination = destination;
      this.indices = indices;
      this.candidates = candidates;

      final int count = indices.length;
      this.permutation = permutation(count, count);
      reset();
    }

    @Override
    public Cascade downstream() {
      return downstream;
    }

    public void setDownstream(Cascade downstream) {
      this.downstream = downstream;
    }

    @Override
    public boolean forward() {
      if (!iter.hasNext()) return false;

      final int[] permutation = iter.next();
      assert permutation.length == indices.length;

      for (int i = 0; i < permutation.length; ++i)
        destination[indices[i]] = candidates.get(permutation[i]);

      return true;
    }

    @Override
    public void reset() {
      iter = permutation.iterator();
    }
  }
}
