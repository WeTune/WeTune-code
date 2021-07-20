package sjtu.ipads.wtune.superopt.fragment1;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.common.utils.NameSequence.mkIndexed;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_ARGS;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.FUNC_CALL_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.NAME_2_1;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.ATTRS;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.PRED;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.TABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan1.Expr;
import sjtu.ipads.wtune.sqlparser.plan1.InputNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

class PlanTranslator {
  private final Fragment fragment;
  private final Constraints constraints;
  private final Map<Symbol, TableDesc> tableDescs;
  private final Map<Symbol, AttrsDesc> attrsDescs;
  private final Map<Symbol, PredDesc> predDescs;

  private final NameSequence tableNameSeq, attrNameSeq, predNameSeq, queryNameSeq;

  private PlanTranslator(Fragment fragment, Constraints constraints) {
    this.fragment = fragment;
    this.constraints = constraints;

    tableDescs = new LinkedHashMap<>(4);
    attrsDescs = new LinkedHashMap<>(8);
    predDescs = new LinkedHashMap<>(4);
    tableNameSeq = mkIndexed("t", 0);
    attrNameSeq = mkIndexed("a", 0);
    predNameSeq = mkIndexed("p", 0);
    queryNameSeq = mkIndexed("q", 0);
  }

  static PlanNode translate(Fragment fragment, Constraints constraints) {
    return new PlanTranslator(fragment, constraints).translate();
  }

  PlanNode translate() {
    final Symbols symbols = fragment.symbols();

    assign(symbols.symbolsOf(TABLE), tableDescs, TableDesc::new);
    assign(symbols.symbolsOf(ATTRS), attrsDescs, AttrsDesc::new);
    assign(symbols.symbolsOf(PRED), predDescs, PredDesc::new);

    applyAttrsFrom(symbols.symbolsOf(ATTRS));

    resolveTables();
    resolveAttrs();
    resolvePreds();

    final Schema schema = mkSchema();
    final Model model = mkModel(schema);
    final PlanContext ctx = PlanContext.mk(schema);

    return fragment.root().instantiate(ctx, model);
  }

  private <T> void assign(Collection<Symbol> syms, Map<Symbol, T> descs, Supplier<T> supplier) {
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
    for (TableDesc desc : tableDescs.values()) desc.name = tableNameSeq.next();
  }

  private void resolvePreds() {
    for (PredDesc desc : predDescs.values()) desc.name = predNameSeq.next();
  }

  private void resolveAttrs() {
    for (TableDesc tableDesc : tableDescs.values()) {
      final List<String> attrNames = tableDesc.attrNames = new ArrayList<>(4);
      for (AttrsDesc attrsDesc : tableDesc.attrs) {
        attrsDesc.table = tableDesc.name;
        attrsDesc.name = attrNameSeq.next();
        attrNames.add(attrsDesc.name);
      }
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

    final Map<String, InputNode> inputs = new HashMap<>(tableDescs.size());

    for (var pair : tableDescs.entrySet()) {
      final Table table = schema.table(pair.getValue().name);
      final InputNode input = InputNode.mk(table, table.name());
      model.assign(pair.getKey(), input);
      inputs.put(table.name(), input);
    }

    for (var pair : attrsDescs.entrySet()) {
      if (pair.getValue().table != null) {
        final InputNode input = inputs.get(pair.getValue().table);
        final String attrName = pair.getValue().name;
        final Value value = find(input.values(), it -> it.name().equals(attrName));
        model.assign(pair.getKey(), singletonList(value));

      } else {
        final Value value = Value.mk(queryNameSeq.next(), attrNameSeq.next(), Expr.mk(mkColRef()));
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
}
