package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.*;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.common.utils.LeveledException.ignorableEx;
import static sjtu.ipads.wtune.common.utils.NameSequence.mkIndexed;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.wrapWildcardProj;
import static sjtu.ipads.wtune.sqlparser.plan.ValueBag.locateValue;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;

class PlanTranslator {
  private final Map<Symbol, TableDesc> tableDescs;
  private final Map<Symbol, AttrsDesc> attrsDescs;
  private final Map<Symbol, PredDesc> predDescs;

  private Constraints constraints;
  private Symbols symbols;
  private SymbolNaming naming;

  private boolean compatibleMode;

  PlanTranslator() {
    tableDescs = new LinkedHashMap<>(4);
    attrsDescs = new LinkedHashMap<>(8);
    predDescs = new LinkedHashMap<>(4);
  }

  PlanNode translate(Fragment fragment, Constraints constraints) {
    this.constraints = constraints;
    this.symbols = fragment.symbols();

    completeConstraints(fragment);

    assignAll();
    resolveAll();

    final Schema schema = mkSchema();
    final Model model = mkModel(schema);
    final PlanContext ctx = PlanContext.mk(schema);

    return fragment.root().instantiate(model, ctx);
  }

  Pair<PlanNode, PlanNode> translate(
      Substitution substitution, boolean backwardCompatible, boolean wrapIfNeeded) {
    compatibleMode = backwardCompatible;
    naming = substitution.naming();

    final Fragment fragment0 = substitution._0();
    final Fragment fragment1 = substitution._1();

    this.constraints = substitution.constraints();
    this.symbols = Symbols.merge(fragment0.symbols(), fragment1.symbols());

    if (backwardCompatible) {
      completeConstraints(fragment0);
      completeConstraints(fragment1);
    }

    assignAll();
    resolveAll();

    final Schema schema = mkSchema();
    final Model model = mkModel(schema);

    final PlanNode rawPlan0 = fragment0.root().instantiate(model, PlanContext.mk(schema));
    final PlanNode rawPlan1 = fragment1.root().instantiate(model, PlanContext.mk(schema));
    if (!alignOutput(rawPlan0, rawPlan1)) return null;

    if (!wrapIfNeeded) return Pair.of(rawPlan0, rawPlan1);

    // Ensure the outcome is a complete plan instead of a fragment.
    final PlanNode plan0 = wrapFragment(rawPlan0);
    final PlanNode plan1 = wrapFragment(rawPlan1);
    if (!alignOutput(plan0, plan1)) return null;
    return Pair.of(plan0, plan1);
  }

  private void assignAll() {
    assignTables(symbols.symbolsOf(TABLE));
    assignAttrs(symbols.symbolsOf(ATTRS));
    assignPred(symbols.symbolsOf(PRED));
  }

  private void resolveAll() {
    applyAttrsFrom();
    if (!compatibleMode) applyAttrsSub();
    resolveTables();
    resolvePreds();
  }

  private void assignTables(Iterable<Symbol> syms) {
    for (Symbol sym : syms) {
      final TableDesc existing = tableDescs.get(sym);
      if (existing == null) {
        final TableDesc tableDesc = new TableDesc();
        for (Symbol eqSym : constraints.eqClassOf(sym)) tableDescs.put(eqSym, tableDesc);
      }
    }
  }

  private void assignAttrs(Iterable<Symbol> syms) {
    for (Symbol sym : syms) {
      final AttrsDesc existing = attrsDescs.get(sym);
      if (existing == null) {
        final AttrsDesc attrsDesc = new AttrsDesc();
        for (Symbol eqSym : constraints.eqClassOf(sym)) attrsDescs.put(eqSym, attrsDesc);
      }
    }
  }

  private void assignPred(Iterable<Symbol> syms) {
    for (Symbol sym : syms)
      if (predDescs.get(sym) == null) {
        final PredDesc predDesc = new PredDesc();
        for (Symbol eqSym : constraints.eqClassOf(sym)) predDescs.put(eqSym, predDesc);
      }
  }

  private void applyAttrsFrom() {
    for (Constraint c : constraints.ofKind(AttrsFrom)) {
      final Symbol sym0 = c.symbols()[0];
      final Symbol sym1 = c.symbols()[1];
      tableDescs.get(sym1).subValues.add(attrsDescs.get(sym0));
    }
  }

  private void applyAttrsSub() {
    for (Constraint c : constraints.ofKind(AttrsSub)) {
      final Symbol sym0 = c.symbols()[0];
      final Symbol sym1 = c.symbols()[1];

      final AttrsDesc attrsDesc = attrsDescs.get(sym0);
      if (sym1.kind() == TABLE) tableDescs.get(sym1).subValues.add(attrsDesc);
      else if (!constraints.isEq(sym0, sym1)) attrsDescs.get(sym1).subValues.add(attrsDesc);
    }
  }

  private void resolveTables() {
    final NameSequence tableNameSeq = mkIndexed("t", 0);
    final NameSequence attrsNameSeq = mkIndexed("a", 0);

    for (TableDesc desc : tableDescs.values())
      if (desc.name == null) {
        desc.name = tableNameSeq.next();
        for (AttrsDesc subValue : desc.subValues)
          desc.columnNames.addAll(resolveAttrs(subValue, attrsNameSeq));
        desc.columnNames.add(attrsNameSeq.next());
      }
  }

  private void resolvePreds() {
    final NameSequence predNameSeq = mkIndexed("p", 0);
    for (PredDesc desc : predDescs.values()) if (desc.name == null) desc.name = predNameSeq.next();
  }

  private List<String> resolveAttrs(AttrsDesc desc, NameSequence nameSeq) {
    if (desc.attrNames == null) {
      final List<String> names = desc.attrNames = new ArrayList<>(desc.subValues.size() + 1);
      for (AttrsDesc subValues : desc.subValues) names.addAll(resolveAttrs(subValues, nameSeq));
      names.add(nameSeq.next()); // Always add a dummy column.
    }
    return desc.attrNames;
  }

  private Schema mkSchema() {
    final StringBuilder builder = new StringBuilder();

    for (TableDesc table : tableDescs.values()) {
      if (table.initialized) continue;
      table.initialized = true;

      builder.append("create table ").append(table.name).append("(\n");
      assert !table.columnNames.isEmpty();
      for (String attrName : table.columnNames)
        builder.append("  ").append(attrName).append(" int,\n");

      builder.replace(builder.length() - 2, builder.length(), ");\n");
    }

    for (Constraint notNull : constraints.ofKind(NotNull)) {
      final TableDesc table = tableDescs.get(notNull.symbols()[0]);
      final AttrsDesc attr = attrsDescs.get(notNull.symbols()[1]);

      builder
          .append("alter table ")
          .append(table.name)
          .append(" modify column ")
          .append(attr.attrNames.get(0))
          .append(" int not null;\n");
    }

    for (Constraint uniqueKey : constraints.ofKind(Unique)) {
      if (!tableDescs.containsKey(uniqueKey.symbols()[0])) continue;

      final TableDesc table = tableDescs.get(uniqueKey.symbols()[0]);
      final AttrsDesc attrs = attrsDescs.get(uniqueKey.symbols()[1]);
      builder.append("alter table ").append(table.name).append(" add unique (");
      joining(",", attrs.attrNames, builder);
      builder.append(");\n");
    }

    for (Constraint foreignKey : constraints.ofKind(Reference)) {
      if (!tableDescs.containsKey(foreignKey.symbols()[0])) continue;

      final TableDesc table = tableDescs.get(foreignKey.symbols()[0]);
      final AttrsDesc attrs = attrsDescs.get(foreignKey.symbols()[1]);
      final TableDesc refTable = tableDescs.get(foreignKey.symbols()[2]);
      final AttrsDesc refAttrs = attrsDescs.get(foreignKey.symbols()[3]);

      builder.append("alter table ").append(table.name).append(" add foreign key (");
      joining(",", attrs.attrNames, builder);
      builder.append(") references ").append(refTable.name).append('(');
      joining(",", refAttrs.attrNames, builder);
      builder.append(");\n");
    }

    return SqlSupport.parseSchema(MYSQL, builder.toString());
  }

  private Model mkModel(Schema schema) {
    final Model model = Model.mk();
    final NameSequence qualificationSeq = NameSequence.mkIndexed("q", 0);
    for (Symbol symbol : tableDescs.keySet()) mkInput(schema, symbol, qualificationSeq, model);
    for (Symbol symbol : attrsDescs.keySet()) mkAttrs(symbol, qualificationSeq, model);
    for (Symbol symbol : predDescs.keySet()) mkPred(symbol, model);
    return model;
  }

  private void mkInput(Schema schema, Symbol sym, NameSequence qualificationSeq, Model model) {
    final Table table = schema.table(tableDescs.get(sym).name);
    final InputNode input = InputNode.mk(table, qualificationSeq.next());
    model.assign(sym, input);
  }

  private void mkAttrs(Symbol sym, NameSequence qualificationSeq, Model model) {
    if (model.isInterpreted(sym)) return;

    final Symbol source = constraints.sourceOf(sym);
    final AttrsDesc attrs = attrsDescs.get(sym);

    final ValueBag lookup;
    if (source.kind() == TABLE) {
      lookup = model.interpretTable(source).values();
    } else /* refTo instance ValueDesc */ {
      mkAttrs(source, qualificationSeq, model);
      lookup = ValueBag.mk(model.interpretOutAttrs(source).get(0));
    }

    final List<Value> inValues = listMap(attrs.attrNames, it -> locateValue(lookup, null, it));

    final int nullIdx;
    if ((nullIdx = inValues.indexOf(null)) != -1)
      // Ignorable. May be caused by infeasible AttrsSub constraints.
      // e.g. AttrsSub(a0,a1) /\ AttrsSub(a1,a2) /\ AttrsEq(a0,a2)
      throw ignorableEx("attrs not found: " + attrs.attrNames.get(nullIdx));

    final List<Value> outValues = listMap(inValues, Value::wrapAsExprValue);
    final String qualification = qualificationSeq.next();
    for (Value value : outValues) value.setQualification(qualification);

    model.assign(sym, inValues, outValues);
  }

  private void mkPred(Symbol sym, Model model) {
    final Symbol attrs = ((SimpleFilter) symbols.ownerOf(sym)).attrs();
    final AttrsDesc attrsDesc = attrsDescs.get(attrs);
    final int arity = attrsDesc.attrNames.size();
    final PredDesc predDesc = predDescs.get(sym);

    if (predDesc.arity != 0 && predDesc.arity != arity)
      // Ignorable. May be caused by infeasible PredicateEq & AttrsSub.
      throw ignorableEx("mismatched arity");

    final ASTNode name = ASTNode.node(NodeType.NAME_2);
    name.set(NAME_2_1, predDesc.name);

    final ASTNode func = ASTNode.expr(ExprKind.FUNC_CALL);
    final List<ASTNode> columnsRefs = new ArrayList<>(arity);
    for (int i = 0; i < arity; i++) columnsRefs.add(mkColRef());

    func.set(FUNC_CALL_NAME, name);
    func.set(FUNC_CALL_ARGS, columnsRefs);

    model.assign(sym, Expr.mk(func));
  }

  private static ASTNode mkColRef() {
    final ASTNode colName = ASTNode.node(NodeType.COLUMN_NAME);
    colName.set(COLUMN_NAME_TABLE, "?");
    colName.set(COLUMN_NAME_COLUMN, "?");

    final ASTNode colRef = ASTNode.expr(ExprKind.COLUMN_REF);
    colRef.set(COLUMN_REF_COLUMN, colName);

    return colRef;
  }

  private static class TableDesc {
    private final List<AttrsDesc> subValues = new ArrayList<>();
    private final List<String> columnNames = new ArrayList<>();
    private String name;
    private boolean initialized;
  }

  private static class AttrsDesc {
    private final List<AttrsDesc> subValues = new ArrayList<>();
    private List<String> attrNames;
  }

  private static class PredDesc {
    private String name;
    private int arity;
  }

  private void completeConstraints(Fragment fragment) {
    // Add missing AttrsFrom.
    fragment.root().acceptVisitor(OpVisitor.traverse(this::completeConstraints0));
    // In legacy solver, a ref that references the values of a subquery is unconstrained.
    // Thus, incomplete constraint set may be produced.
    // Example: Filter<c0>(Proj<c1>(Input<t0>)). `c0` must be a sublist of `c1`, otherwise invalid.
    // So we add a constraint AttrsEq(c0,c1) in such case (for simplicity).
    fragment.root().acceptVisitor(OpVisitor.traverse(this::completeConstraints1));
    // For Proj0.inAttrs = Proj1.inAttrs, add Proj0.outAttrs = Proj1.outAttrs
    //    fragment.root().acceptVisitor(OpVisitor.traverse(this::completeConstraints2));
  }

  private void completeConstraints0(Op op) {
    if (op.kind() == INPUT) propagateAttrsSource(op.successor(), op, ((Input) op).table());
  }

  private void completeConstraints1(Op op) {
    if (op.kind() != PROJ || op.successor() == null) return;

    propagateAttrsBound(op.successor(), op, ((Proj) op).attrs());
  }

  private void propagateAttrsSource(Op op, Op prev, Symbol source) {
    if (op == null) return;

    switch (op.kind()) {
      case PROJ:
        {
          final Symbol attrs = ((Proj) op).attrs();
          if (constraints.sourceOf(attrs) == null)
            constraints.add(Constraint.mk(AttrsFrom, attrs, source));
          propagateAttrsSource(op.successor(), op, source);
          return;
        }

      case IN_SUB_FILTER:
      case SIMPLE_FILTER:
        {
          if (prev != op.predecessors()[0]) return;
          final Symbol attrs = ((AttrsFilter) op).attrs();
          if (constraints.sourceOf(attrs) == null)
            constraints.add(Constraint.mk(AttrsFrom, attrs, source));
          propagateAttrsSource(op.successor(), op, source);
          return;
        }

      case LEFT_JOIN:
      case INNER_JOIN:
        {
          final Join join = (Join) op;
          final Symbol attrs0 = join.lhsAttrs(), attrs1 = join.rhsAttrs();

          if (prev == join.predecessors()[0] && constraints.sourceOf(attrs0) == null)
            constraints.add(Constraint.mk(AttrsFrom, attrs0, source));
          if (prev == join.predecessors()[1] && constraints.sourceOf(attrs1) == null)
            constraints.add(Constraint.mk(AttrsFrom, attrs1, source));

          propagateAttrsSource(op.successor(), op, source);
          return;
        }

      default:
        throw new IllegalArgumentException();
    }
  }

  private void propagateAttrsBound(Op op, Op prev, Symbol bound) {
    if (op == null) return;

    switch (op.kind()) {
      case PROJ:
        {
          final Symbol attrs = ((Proj) op).attrs();
          if (constraints.sourceOf(attrs) == constraints.sourceOf(bound))
            constraints.add(Constraint.mk(AttrsEq, attrs, bound));
          return;
        }
      case IN_SUB_FILTER:
      case SIMPLE_FILTER:
        {
          if (prev != op.predecessors()[0]) return;
          final Symbol attrs = ((AttrsFilter) op).attrs();
          if (constraints.sourceOf(attrs) == constraints.sourceOf(bound))
            constraints.add(Constraint.mk(AttrsEq, attrs, bound));
          propagateAttrsBound(op.successor(), op, bound);
          return;
        }
      case LEFT_JOIN:
      case INNER_JOIN:
        {
          final Join join = (Join) op;
          final Symbol attrs = prev == op.predecessors()[0] ? join.lhsAttrs() : join.rhsAttrs();
          if (constraints.sourceOf(attrs) == constraints.sourceOf(bound))
            constraints.add(Constraint.mk(AttrsEq, attrs, bound));
          propagateAttrsBound(op.successor(), op, bound);
          return;
        }
      default:
        throw new IllegalArgumentException();
    }
  }

  private static boolean alignOutput(PlanNode p0, PlanNode p1) {
    final ValueBag values0 = p0.values(), values1 = p1.values();
    if (values0.size() != values1.size()) return false;

    for (int i = 0, bound = values0.size(); i < bound; ++i) {
      final Value value0 = values0.get(i);
      final Value value1 = values1.get(i);
      if (value1.expr() != null) value1.setName(value0.name());
      if (value0.expr() != null) value0.setName(value1.name());
    }

    return true;
  }

  private static PlanNode wrapFragment(PlanNode plan) {
    if (isFragment(plan)) return wrapWildcardProj(plan);
    else return plan;
  }

  private static boolean isFragment(PlanNode node) {
    // Check if the node is a complete query.
    // Specifically, check if the root node is not Union/Proj.
    // (ignore Sort/Limit)
    final OperatorType type = node.kind();
    return type != SET_OP
        && type != PROJ
        && (type != SORT && type != LIMIT && type != AGG || isFragment(node.predecessors()[0]));
  }
}
