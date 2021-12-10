package sjtu.ipads.wtune.prover.logic;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.ArraySupport;
import sjtu.ipads.wtune.common.utils.Cascade;
import sjtu.ipads.wtune.common.utils.IterableSupport;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.*;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;
import sjtu.ipads.wtune.prover.utils.Constants;
import sjtu.ipads.wtune.sqlparser.schema.Column;
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
  private final Set<Proposition> assertions;

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
  private Value nullValue;

  private int seq;

  private LogicTranslator(LogicCtx ctx) {
    this.ctx = ctx;
    this.assertions = new LinkedHashSet<>();

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
    prepareAuxiliary();

    return translateBag(expr);
  }

  Proposition translate(Constraint constraint) {
    switch (constraint.kind()) {
      case FOREIGN:
        return translateForeignKey(constraint.columns(), constraint.refColumns());
      case NOT_NULL:
        return translateNotNull(constraint.columns().get(0));
      default:
        throw new IllegalArgumentException();
    }
  }

  Set<Proposition> assertions() {
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
    nullValue = mkConst(Name.mk("0"));
  }

  private void prepareAuxiliary() {
    if (asIntFunc == null) asIntFunc = mkAsIntFunc();
  }

  private TableMeta mkTableMeta(Name tableName) {
    final Set<Name> attrNames = tableAttrs.get(tableName);
    final DataType tupleType = mkDataType(tableName.toString(), attrNames);

    final Func func = ctx.mkFunc(TABLE_FUNC_PREFIX + tableName, ctx.mkIntType(), tupleType);
    final Value x = ctx.mkVal("x", tupleType);
    final Proposition assertion = ctx.mkForall(x, func.apply(x).ge(0));

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
    final DataType[] argTypes = ArraySupport.generate(arity, i -> ctx.mkIntType(), DataType.class);
    return ctx.mkFunc(funcNameSeq.next(), ctx.mkIntType(), argTypes);
  }

  private Func mkPred(Name predName) {
    final int arity = lookup.predArityOf(predName);
    final DataType[] argTypes = ArraySupport.generate(arity, i -> ctx.mkIntType(), DataType.class);
    return ctx.mkFunc(funcNameSeq.next(), ctx.mkBoolType(), argTypes);
  }

  private DataType mkDataType(String nameSuffix, Set<Name> members) {
    final String name = TUPLE_TYPE_PREFIX + nameSuffix;

    // The type of output tuple won't be cached
    if (nameSuffix.equals(OUT_TUPLE_TYPE_NAME))
      return ctx.mkTupleType(name, ArraySupport.map(members, Object::toString, String.class));

    return knownTypes.computeIfAbsent(
        members, ms -> ctx.mkTupleType(name, ArraySupport.map(ms, Object::toString, String.class)));
  }

  private Value mkConst(Name constant) {
    if (constant.toString().equals("0")) {
      return ctx.mkConst(0);
    } else {
      return ctx.mkIntVal(constNameSeq.next());
    }
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
      assert IterableSupport.none(asList(args), Var::isBase);
      return funcs.get(name).apply(ArraySupport.map(args, this::mkVal, Value.class));
    }

    return assertFalse();
  }

  private Func mkMainFunc(DataType retType, DataType... argTypes) {
    return ctx.mkFunc(MAIN_FUNC_PREFIX + seq++, retType, argTypes);
  }

  private Func mkAsIntFunc() {
    final Func f = ctx.mkFunc("as_int", ctx.mkIntType(), ctx.mkBoolType());

    final Proposition x = ctx.mkBoolVal("x");
    final Value lhs = f.apply(x);
    final Value rhs = x.ite(ctx.mkConst(1), ctx.mkConst(0));
    assertions.add(ctx.mkForall(x, lhs.eq(rhs)));

    return f;
  }

  private Iterable<Value[]> mkMainFuncArgs(DataType... argTypes) {
    return () -> new ArgsPermutation(argTypes);
  }

  private Value mkProjection(DataType tupleType, Column column, Value v) {
    return tupleType.accessor(column.name()).apply(v);
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
    assert IterableSupport.none(asList(vars), Var::isBase);
    final Value[] args = ArraySupport.map(vars, LogicTranslator.this::mkVal, Value.class);
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

  private List<Value> translate0(Conjunction c) {
    final List<Value> values = new ArrayList<>();

    c.tables().forEach(it -> values.add(translateTable((TableTerm) it)));
    c.preds().forEach(it -> values.add(translatePred(it)));
    if (c.squash() != null) values.add(translateSet(c.squash()));
    if (c.neg() != null)
      // values.add(translateSet(c.neg()).not());
      for (Conjunction factor : c.neg()) {
        values.add(translateSet(factor).not());
      }

    return values;
  }

  private Value translateBag0(Conjunction c) {
    final List<Value> values = translate0(c);

    final ListIterator<Value> iter = values.listIterator();
    while (iter.hasNext()) {
      final Value v = iter.next();
      if (v instanceof Proposition) iter.set(asIntFunc.apply(v));
    }

    // Product all terms.
    return ctx.mkProduct(values.toArray(Value[]::new));
  }

  private Value translateBag1(Conjunction c) {
    assert !c.vars().isEmpty();
    final Value body = translateBag0(c);
    // Declare the function.
    final DataType[] argsTypes = ArraySupport.map(c.vars(), it -> varMeta.get(it).dataType, DataType.class);
    final Func func = mkMainFunc(ctx.mkIntType(), argsTypes);
    // Define the function.
    final Value[] argsValues = ArraySupport.map(c.vars(), this::mkVal, Value.class);
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
    return ctx.mkDisjunction(ArraySupport.map(d.conjunctions(), this::translateSet, Proposition.class));
  }

  private Proposition translateSet(Conjunction c) {
    final List<Value> values = translate0(c);

    final ListIterator<Value> iter = values.listIterator();
    while (iter.hasNext()) {
      final Value v = iter.next();
      if (!(v instanceof Proposition)) iter.set(v.gt(0));
    }

    final Proposition prop =
        ctx.mkConjunction(ArraySupport.map(values, it -> (Proposition) it, Proposition.class));

    return c.vars().isEmpty()
        ? prop
        : ctx.mkExists(ArraySupport.map(c.vars(), this::mkVal, Value.class), prop);
  }

  private Proposition translateForeignKey(List<Column> columns, List<Column> refColumns) {
    // Temporary patch. NotNull should be independently added.
    //    assertions.add(translateNotNull(columns.get(0)));

    final String ownerTable = columns.get(0).tableName();
    final String refTable = refColumns.get(0).tableName();
    final Func ownerTableFunc = mkTableMeta(Name.mk(ownerTable)).func;
    final Func refTableFunc = mkTableMeta(Name.mk(refTable)).func;
    final DataType xType = ownerTableFunc.paramTypes()[0];
    final DataType yType = refTableFunc.paramTypes()[0];
    final Value x = ctx.mkVal("x", xType);
    final Value y = ctx.mkVal("y", yType);
    final Value X = ownerTableFunc.apply(x);
    final Value Y = refTableFunc.apply(y);

    // R.x References S.y -->
    // forall r. (R(r) > 0 /\ r.x != Null => (exists s. S(s) > 0 /\ s.y != Null /\ r.x = s.u)
    final List<Proposition> conds0 =
        listMap(columns, col -> mkProjection(xType, col, x).eq(nullValue).not());

    final List<Proposition> conds1 =
        zipMap(
            columns,
            refColumns,
            (xCol, yCol) -> {
              final Value xVal = mkProjection(xType, xCol, x), yVal = mkProjection(yType, yCol, y);
              return xVal.eq(yVal).and(yVal.eq(nullValue).not());
            });

    final Proposition cond0 = ctx.mkConjunction(conds0.toArray(Proposition[]::new));
    final Proposition cond1 = ctx.mkConjunction(conds1.toArray(Proposition[]::new));

    return ctx.mkForall(x, X.gt(0).and(cond0).implies(ctx.mkExists(y, Y.gt(0).and(cond1))));
  }

  private Proposition translateNotNull(Column column) {
    final String ownerTable = column.tableName();
    final Func tableFunc = mkTableMeta(Name.mk(ownerTable)).func;
    final DataType tupleType = tableFunc.paramTypes()[0];
    final Value v = ctx.mkVal("x", tupleType);
    final Value nullValue = ctx.mkConst(0);

    return ctx.mkForall(
        v,
        mkProjection(tupleType, column, v).eq(nullValue).implies(tableFunc.apply(v).eq(nullValue)));
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
