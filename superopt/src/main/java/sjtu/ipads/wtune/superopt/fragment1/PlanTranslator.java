package sjtu.ipads.wtune.superopt.fragment1;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan1.*;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.common.utils.NameSequence.mkIndexed;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.PROJ;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.AttrsEq;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.AttrsFrom;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.alignOutput;
import static sjtu.ipads.wtune.superopt.fragment1.FragmentUtils.wrapFragment;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.*;

class PlanTranslator {
  private final Map<Symbol, TableDesc> tableDescs;
  private final Map<Symbol, AttrsDesc> attrsDescs;
  private final Map<Symbol, PredDesc> predDescs;
  private final NameSequence tableNameSeq, attrNameSeq, predNameSeq, queryNameSeq;

  private Constraints constraints;
  private Symbols symbols;

  PlanTranslator() {
    tableDescs = new LinkedHashMap<>(4);
    attrsDescs = new LinkedHashMap<>(8);
    predDescs = new LinkedHashMap<>(4);
    tableNameSeq = mkIndexed("r", 0);
    attrNameSeq = mkIndexed("a", 0);
    predNameSeq = mkIndexed("p", 0);
    queryNameSeq = mkIndexed("q", 0);
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

    return fragment.root().instantiate(ctx, model);
  }

  Pair<PlanNode, PlanNode> translate(Substitution substitution) {
    final Fragment fragment0 = substitution._0();
    final Fragment fragment1 = substitution._1();

    this.constraints = substitution.constraints();
    this.symbols = Symbols.merge(fragment0.symbols(), fragment1.symbols());

    completeConstraints(fragment0);
    completeConstraints(fragment1);

    assignAll();
    resolveAll();

    final Schema schema = mkSchema();
    final Model model = mkModel(schema);

    final PlanNode rawPlan0 = fragment0.root().instantiate(PlanContext.mk(schema), model);
    final PlanNode rawPlan1 = fragment1.root().instantiate(PlanContext.mk(schema), model);
    alignOutput(rawPlan0, rawPlan1);
    // Ensure the outcome is a complete plan instead of a fragment.
    final PlanNode plan0 = wrapFragment(rawPlan0);
    final PlanNode plan1 = wrapFragment(rawPlan1);
    alignOutput(plan0, plan1);

    return Pair.of(plan0, plan1);
  }

  private void assignAll() {
    assign0(symbols.symbolsOf(TABLE), tableDescs, TableDesc::new);
    assign0(symbols.symbolsOf(ATTRS), attrsDescs, AttrsDesc::new);
    assign0(symbols.symbolsOf(PRED), predDescs, PredDesc::new);
    applyAttrsFrom(symbols.symbolsOf(ATTRS));
  }

  private void resolveAll() {
    resolveTables();
    resolveAttrs();
    resolvePreds();
  }

  private <T> void assign0(Collection<Symbol> syms, Map<Symbol, T> descs, Supplier<T> supplier) {
    for (Symbol sym : syms) {
      final T desc = descs.get(sym);
      if (desc != null) continue;

      final T newDesc = supplier.get();
      for (Symbol eqSym : constraints.eqClassOf(sym))
        if (syms.contains(eqSym)) descs.put(eqSym, newDesc);
    }
  }

  private void applyAttrsFrom(Collection<Symbol> attrsSyms) {
    for (Symbol attrSym : attrsSyms) {
      final Symbol tableSym = constraints.sourceOf(attrSym);
      if (tableSym == null) continue;
      tableDescs.get(tableSym).attrs.add(attrsDescs.get(attrSym));
    }
  }

  private void resolveTables() {
    for (TableDesc desc : tableDescs.values())
      if (desc.name == null) desc.name = tableNameSeq.next();
  }

  private void resolvePreds() {
    for (PredDesc desc : predDescs.values()) if (desc.name == null) desc.name = predNameSeq.next();
  }

  private void resolveAttrs() {
    for (TableDesc tableDesc : tableDescs.values()) {
      final List<String> attrNames = tableDesc.attrNames = new ArrayList<>(4);
      for (AttrsDesc attrsDesc : tableDesc.attrs) {
        if (attrsDesc.name == null) {
          attrsDesc.table = tableDesc.name;
          attrsDesc.name = attrNameSeq.next();
        }
        attrNames.add(attrsDesc.name);
      }
    }
    for (AttrsDesc value : attrsDescs.values()) {
      if (value.name == null)
        value.name = attrNameSeq.next();
    }
  }

  private Schema mkSchema() {
    final StringBuilder builder = new StringBuilder();
    final Set<String> initedSymbol = new HashSet<>();

    for (TableDesc table : tableDescs.values()) {
      if (!initedSymbol.add(table.name)) continue;

      builder.append("create table ").append(table.name).append("(\n");

      for (String attrName : table.attrNames)
        builder.append("  ").append(attrName).append(" int,\n");
      if (table.attrNames.isEmpty())
        // empty table is syntactically error, so we add a dummy column
        builder.append("  ").append(attrNameSeq.next()).append(" int,\n");

      builder.replace(builder.length() - 2, builder.length(), ");\n");
    }

    for (Constraint uniqueKey : constraints.uniqueKeys()) {
      if (!tableDescs.containsKey(uniqueKey.symbols()[0])) continue;

      final TableDesc table = tableDescs.get(uniqueKey.symbols()[0]);
      final AttrsDesc attr = attrsDescs.get(uniqueKey.symbols()[1]);
      builder
          .append("alter table ")
          .append(table.name)
          .append(" add unique (")
          .append(attr.name)
          .append(");\n");
    }

    for (Constraint foreignKey : constraints.foreignKeys()) {
      if (!tableDescs.containsKey(foreignKey.symbols()[0])) continue;

      final TableDesc table = tableDescs.get(foreignKey.symbols()[0]);
      final AttrsDesc attr = attrsDescs.get(foreignKey.symbols()[1]);
      final TableDesc refTable = tableDescs.get(foreignKey.symbols()[2]);
      final AttrsDesc refAttr = attrsDescs.get(foreignKey.symbols()[3]);

      builder
          .append("alter table ")
          .append(table.name)
          .append(" add foreign key (")
          .append(attr.name)
          .append(") references ")
          .append(refTable.name)
          .append('(')
          .append(refAttr.name)
          .append(");\n");
    }

    return Schema.parse(MYSQL, builder.toString());
  }

  private Model mkModel(Schema schema) {
    final Model model = Model.mk();

    final Map<Symbol, InputNode> inputs = new HashMap<>(tableDescs.size());

    for (var pair : tableDescs.entrySet()) {
      final Table table = schema.table(pair.getValue().name);
      final InputNode input = InputNode.mk(table, table.name());
      model.assign(pair.getKey(), input);
      inputs.put(pair.getKey(), input);
    }

    for (var pair : attrsDescs.entrySet()) {
      final AttrsDesc desc = pair.getValue();
      if (desc.table != null) {
        final InputNode input = inputs.get(constraints.sourceOf(pair.getKey()));
        final String attrName = desc.name;
        final Value value = find(input.values(), it -> it.name().equals(attrName));
        model.assign(pair.getKey(), singletonList(value));

      } else {
        final Value value = Value.mk(queryNameSeq.next(), desc.name, Expr.mk(mkColRef()));
        model.assign(pair.getKey(), singletonList(value));
      }
    }

    for (var pair : predDescs.entrySet()) {
      final ASTNode name = ASTNode.node(NodeType.NAME_2);
      name.set(NAME_2_1, pair.getValue().name);

      final ASTNode func = ASTNode.expr(ExprKind.FUNC_CALL);
      func.set(FUNC_CALL_NAME, name);
      func.set(FUNC_CALL_ARGS, singletonList(mkColRef()));

      model.assign(pair.getKey(), Expr.mk(func));
    }

    return model;
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
    private final Set<AttrsDesc> attrs = new HashSet<>();
    private String name;
    private List<String> attrNames;
  }

  private static class AttrsDesc {
    private String table, name;
  }

  private static class PredDesc {
    private String name;
  }

  private void completeConstraints(Fragment fragment) {
    // Add missing AttrsFrom.
    fragment.root().acceptVisitor(OpVisitor.traverse(this::completeConstraints0));
    // In legacy solver, an ref that references the values of a subquery is unconstrained.
    // Thus, incomplete constraint set may be produced.
    // Example: Filter<c0>(Proj<c1>(Input<t0>)). `c0` must be a sublist of `c1`, otherwise invalid.
    // So we add a constraint AttrsEq(c0,c1) in such case (for simplicity).
    fragment.root().acceptVisitor(OpVisitor.traverse(this::completeConstraints1));
    // For Proj0.inAttrs = Proj1.inAttrs, add Proj0.outAttrs = Proj1.outAttrs
    fragment.root().acceptVisitor(OpVisitor.traverse(this::completeConstraints2));
  }

  private void completeConstraints0(Op op) {
    if (op.type() == INPUT) propagateAttrsSource(op.successor(), op, ((Input) op).table());
  }

  private void completeConstraints1(Op op) {
    if (op.type() != PROJ || op.successor() == null) return;

    propagateAttrsBound(op.successor(), op, ((Proj) op).inAttrs());
  }

  private void completeConstraints2(Op op) {
    if (op.type() != PROJ) return;

    final Proj proj = (Proj) op;

    for (Symbol symbol : constraints.eqClassOf(proj.inAttrs())) {
      final Op owner = symbols.ownerOf(ATTRS, symbol);
      if (owner != op && owner.type() == PROJ) {
        constraints.add(Constraint.mk(AttrsEq, proj.outAttrs(), ((Proj) owner).outAttrs()));
      }
    }
  }

  private void propagateAttrsSource(Op op, Op prev, Symbol source) {
    if (op == null) return;

    switch (op.type()) {
      case PROJ:
        {
          final Symbol attrs = ((Proj) op).inAttrs();
          if (constraints.sourceOf(attrs) == null)
            constraints.add(Constraint.mk(AttrsFrom, attrs, source));
          propagateAttrsSource(op.successor(), op, source);
          return;
        }

      case IN_SUB_FILTER:
      case SIMPLE_FILTER:
        {
          if (prev != op.predecessors()[0]) return;
          final Symbol attrs = ((Filter) op).attrs();
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

    switch (op.type()) {
      case PROJ:
        {
          final Symbol attrs = ((Proj) op).inAttrs();
          if (constraints.sourceOf(attrs) == constraints.sourceOf(bound))
            constraints.add(Constraint.mk(AttrsEq, attrs, bound));
          return;
        }
      case IN_SUB_FILTER:
      case SIMPLE_FILTER:
        {
          if (prev != op.predecessors()[0]) return;
          final Symbol attrs = ((Filter) op).attrs();
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
}
