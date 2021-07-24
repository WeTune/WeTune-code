package sjtu.ipads.wtune.prover.logic;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.Cascade;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.*;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;
import sjtu.ipads.wtune.prover.utils.Constants;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;

import java.util.*;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.Commons.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

class LogicTranslator {
  private static final String TUPLE_TYPE_PREFIX = "Tup_";
  private static final String TABLE_FUNC_PREFIX = "t_";
  private static final String UNINTER_FUNC_PREFIX = "g";
  private static final String MAIN_FUNC_PREFIX = "f";
  private static final String CONSTANT_PREFIX = "k";
  private static final String FREE_VAL_PREFIX = "v";
  private static final String OUT_TUPLE_TYPE_NAME = "out";
  private static final Var FREE_VAR = Var.mkBase(Constants.FREE_VAR);

  private final LogicCtx ctx;
  private final List<Proposition> assertions;

  private final SetMultimap<Name, Name> tableAttrs;
  private final Map<Name, TableMeta> tableMeta;
  private final Map<Var, VarMeta> varMeta;
  private final Map<Name, Func> funcs;
  private final Map<Name, Value> consts;
  private final Multimap<DataType, Value> freeVals;
  private final Map<Set<Name>, DataType> knownTypes;
  private final NameSequence tmpTypeNameSeq, funcNameSeq, constNameSeq, freeValNameSeq;

  private SymbolLookup lookup;
  private Func asIntFunc;

  private int seq;

  private LogicTranslator(LogicCtx ctx) {
    this.ctx = ctx;
    this.assertions = new ArrayList<>();

    this.tableAttrs = MultimapBuilder.SetMultimapBuilder.hashKeys(8).hashSetValues(2).build();
    this.tableMeta = new HashMap<>(4);
    this.varMeta = new HashMap<>(8);
    this.funcs = new HashMap<>(8);
    this.consts = new HashMap<>(2);
    this.freeVals = MultimapBuilder.hashKeys(8).arrayListValues(2).build();
    this.knownTypes = new HashMap<>(8);

    this.tmpTypeNameSeq = NameSequence.mkIndexed("m", 0);
    this.funcNameSeq = NameSequence.mkIndexed(UNINTER_FUNC_PREFIX, 0);
    this.constNameSeq = NameSequence.mkIndexed(CONSTANT_PREFIX, 0);
    this.freeValNameSeq = NameSequence.mkIndexed(FREE_VAL_PREFIX, 0);
  }

  static LogicTranslator mk(LogicCtx ctx) {
    return new LogicTranslator(ctx);
  }

  SymbolLookup prepare(Disjunction d) {
    final SymbolLookup usage = SymbolLookup.mk(d);
    for (Name tableName : usage.tables()) tableAttrs.putAll(tableName, usage.attrsOf(tableName));
    return usage;
  }

  // Assumptions: each variable has distinct name.
  //              each attribute has distinct name.
  Value translate(Disjunction expr, /* nullable */ SymbolLookup usage) {
    this.lookup = coalesce(usage, () -> prepare(expr));

    this.tableMeta.clear();
    this.varMeta.clear();
    // funcs shouldn't be cleared

    prepareTables();
    prepareVars();
    prepareFuncs();
    prepareConsts();

    return translateBag(expr);
  }

  Proposition translate(Constraint constraint) {
    assert constraint.type() == ConstraintType.FOREIGN;
    final String ownerTable = constraint.columns().get(0).tableName();
    final String refTable = constraint.refColumns().get(0).tableName();
    final Func ownerTableFunc = mkTableMeta(Name.mk(ownerTable)).func;
    final Func refTableFunc = mkTableMeta(Name.mk(refTable)).func;
    final DataType xType = ownerTableFunc.paramTypes()[0];
    final DataType yType = refTableFunc.paramTypes()[0];
    final Value x = ctx.mkVal("x", xType);
    final Value y = ctx.mkVal("y", yType);
    final Value X = ownerTableFunc.apply(x);
    final Value Y = refTableFunc.apply(y);

    final List<Value> eqConds =
        zipMap(
            constraint.columns(),
            constraint.refColumns(),
            (xCol, yCol) ->
                asIntFunc.apply(
                    xType.accessor(xCol.name()).apply(x).eq(yType.accessor(yCol.name()).apply(y))));
    final Value eqCond = ctx.mkProduct(eqConds.toArray(Value[]::new));

    return ctx.mkForall(x, X.gt(0).implies(ctx.mkExists(y, Y.mul(eqCond).gt(0))));
  }

  List<Proposition> assertions() {
    return assertions;
  }

  private void prepareTables() {
    for (Name table : lookup.tables()) {
      final TableMeta meta = mkTableMeta(table);
      tableMeta.put(table, meta);
      assertions.add(meta.assertion);
    }
  }

  private void prepareVars() {
    for (Var var : lookup.vars()) varMeta.put(var, mkVarMeta(var));
  }

  private void prepareFuncs() {
    for (Name pred : lookup.preds()) funcs.computeIfAbsent(pred, this::mkPred);
    for (Name func : lookup.funcs()) funcs.computeIfAbsent(func, this::mkFunc);
  }

  private void prepareConsts() {
    for (Name constant : lookup.consts()) consts.computeIfAbsent(constant, this::mkConst);
  }

  private TableMeta mkTableMeta(Name tableName) {
    final Set<Name> attrNames = tableAttrs.get(tableName);
    final DataType tupleType = mkDataType(tableName.toString(), attrNames);

    final Func func = ctx.mkFunc(TABLE_FUNC_PREFIX + tableName, ctx.mkIntType(), tupleType);
    final Value x = ctx.mkVal("x", tupleType);
    final Proposition assertion = ctx.mkForall(x, func.apply(x).gt(0));

    return new TableMeta(tableName, tupleType, func, assertion);
  }

  private VarMeta mkVarMeta(Var var) {
    final Name vName = var.name();
    final Name tName = lookup.tableOf(var);
    final TableMeta tableMeta = this.tableMeta.get(tName);

    final DataType dataType;
    if (tableMeta != null) dataType = tableMeta.dataType;
    else {
      final String typeName = FREE_VAR.equals(var) ? OUT_TUPLE_TYPE_NAME : tmpTypeNameSeq.next();
      final Set<Name> attrNames = tName != null ? tableAttrs.get(tName) : lookup.attrsOf(var);
      dataType = mkDataType(typeName, attrNames);
    }

    final Value val = ctx.mkVal(vName.toString(), dataType);

    return new VarMeta(vName, tName, dataType, val);
  }

  private Func mkFunc(Name funcName) {
    final int arity = lookup.funcArityOf(funcName);
    final DataType[] argTypes = generate(arity, i -> ctx.mkIntType(), DataType.class);
    return ctx.mkFunc(funcNameSeq.next(), ctx.mkIntType(), argTypes);
  }

  private Func mkPred(Name predName) {
    final int arity = lookup.predArityOf(predName);
    final DataType[] argTypes = generate(arity, i -> ctx.mkIntType(), DataType.class);
    return ctx.mkFunc(funcNameSeq.next(), ctx.mkBoolType(), argTypes);
  }

  private DataType mkDataType(String nameSuffix, Set<Name> members) {
    final String name = TUPLE_TYPE_PREFIX + nameSuffix;

    // The type of output tuple won't be cached
    if (nameSuffix.equals(OUT_TUPLE_TYPE_NAME))
      return ctx.mkTupleType(name, arrayMap(members, Object::toString, String.class));

    return knownTypes.computeIfAbsent(
        members, ms -> ctx.mkTupleType(name, arrayMap(ms, Object::toString, String.class)));
  }

  private Value mkConst(Name constant) {
    return ctx.mkIntVal(constNameSeq.next());
  }

  private Value mkFreeVal(DataType type) {
    return ctx.mkVal(freeValNameSeq.next(), type);
  }

  private Value mkVal(Var v) {
    final Name name = v.name();

    if (v.isBase()) {
      return varMeta.get(v).val;
    }

    if (v.isConstant()) {
      return consts.get(name);
    }

    if (v.isProjected()) {
      final Var base = v.base()[0];
      assert base.isBase();
      final VarMeta meta = this.varMeta.get(base);
      return meta.dataType.accessor(name.toString()).apply(meta.val);
    }

    if (v.isFunc()) {
      final Var[] args = v.base();
      assert none(asList(args), Var::isBase);
      return funcs.get(name).apply(arrayMap(args, this::mkVal, Value.class));
    }

    return assertFalse();
  }

  private Func mkMainFunc(DataType retType, DataType... argTypes) {
    return ctx.mkFunc(MAIN_FUNC_PREFIX + seq++, retType, argTypes);
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

    final Func func = tableMeta.get(tableTerm.name()).func;
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
    final Value[] args = arrayMap(vars, LogicTranslator.this::mkVal, Value.class);
    return ((Proposition) funcs.get(p.name()).apply(args));
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
    final DataType[] argsTypes = arrayMap(c.vars(), it -> varMeta.get(it).dataType, DataType.class);
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

    if (c.vars().isEmpty()) return val.gt(0);
    else {
      final Value[] vars = arrayMap(c.vars(), this::mkVal, Value.class);
      return ctx.mkExists(vars, val.gt(0));
    }
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

  private static class TableMeta {
    private final Name tableName;
    private final DataType dataType;
    private final Func func;
    private final Proposition assertion;

    private TableMeta(Name tableName, DataType dataType, Func func, Proposition assertion) {
      this.tableName = tableName;
      this.dataType = dataType;
      this.func = func;
      this.assertion = assertion;
    }
  }

  private static class VarMeta {
    private final Name varName, tableName;
    private final DataType dataType;
    private final Value val;

    private VarMeta(Name varName, Name tableName, DataType dataType, Value val) {
      this.varName = varName;
      this.tableName = tableName;
      this.dataType = dataType;
      this.val = val;
    }
  }
}
