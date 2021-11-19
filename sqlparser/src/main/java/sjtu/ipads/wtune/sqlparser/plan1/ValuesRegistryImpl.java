package sjtu.ipads.wtune.sqlparser.plan1;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.COW;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.*;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.common.utils.ListSupport.join;

class ValuesRegistryImpl implements ValuesRegistry {
  private int nextId;
  private final PlanContext ctx;
  private final COW<TIntObjectMap<Values>> nodeValues;
  private final COW<TIntObjectMap<Expression>> valueExprs;
  private final COW<Map<Expression, Values>> exprRefs;

  protected ValuesRegistryImpl(PlanContext ctx) {
    this.nextId = 0;
    this.ctx = ctx;
    this.nodeValues = new COW<>(new TIntObjectHashMap<>(ctx.maxNodeId()), null);
    this.valueExprs = new COW<>(new TIntObjectHashMap<>(), null);
    this.exprRefs = new COW<>(new HashMap<>(), null);
  }

  protected ValuesRegistryImpl(ValuesRegistryImpl toCopy) {
    this.nextId = toCopy.nextId;
    this.ctx = toCopy.ctx;
    this.nodeValues = new COW<>(toCopy.nodeValues.forRead(), TIntObjectHashMap::new);
    this.valueExprs = new COW<>(toCopy.valueExprs.forRead(), TIntObjectHashMap::new);
    this.exprRefs = new COW<>(toCopy.exprRefs.forRead(), HashMap::new);
  }

  @Override
  public Values valuesOf(int nodeId) {
    if (nodeId == NO_SUCH_NODE) return Values.mk(emptyList());

    Values values = nodeValues.forRead().get(nodeId);
    if (values != null) return values;

    List<Expression> exprs = null;
    switch (ctx.kindOf(nodeId)) {
      case Filter:
      case InSub:
      case Exists:
      case SetOp:
      case Limit:
      case Sort:
        return valuesOf(ctx.childOf(nodeId, 0));

      case Join:
        return Values.mk(join(valuesOf(ctx.childOf(nodeId, 0)), valuesOf(ctx.childOf(nodeId, 1))));

      case Proj:
      case Agg:
        final Pair<Values, List<Expression>> pair =
            mkValuesOfExporter((Exporter) ctx.nodeAt(nodeId));
        values = pair.getLeft();
        exprs = pair.getRight();
        break;

      case Input:
        values = mkValuesOfInput((InputNode) ctx.nodeAt(nodeId));
        break;

      default:
        throw new PlanException("unknown plan node kind: " + ctx.kindOf(nodeId));
    }

    registerValue(nodeId, values);
    if (exprs != null) zipForEach(values, exprs, this::registerExpr);

    return values;
  }

  @Override
  public Expression exprOf(Value value) {
    return valueExprs.forRead().get(value.id());
  }

  @Override
  public void bindValueRefs(Expression expr, List<Value> valueRefs) {
    final Values vs;
    if (valueRefs instanceof Values) vs = (Values) valueRefs;
    else vs = Values.mk(valueRefs);
    exprRefs.forWrite().put(expr, vs);
  }

  @Override
  public Values valueRefsOf(Expression expr) {
    return exprRefs.forRead().get(expr);
  }

  @Override
  public ValuesRegistry copy() {
    return new ValuesRegistryImpl(this);
  }

  private Values mkValuesOfInput(InputNode input) {
    final Collection<Column> columns = input.table().columns();
    final String qualification = input.qualification();

    final Values values = Values.mk();
    for (Column column : columns) values.add(Value.mk(++nextId, qualification, column.name()));

    return values;
  }

  private Pair<Values, List<Expression>> mkValuesOfExporter(Exporter exporter) {
    final String qualification = exporter.qualification();
    final List<String> attrNames = exporter.attrNames();

    final Values values = Values.mk();
    for (String attrName : attrNames) values.add(Value.mk(++nextId, qualification, attrName));

    return Pair.of(values, exporter.attrExprs());
  }

  private void registerValue(int nodeId, Values values) {
    nodeValues.forWrite().put(nodeId, values);
  }

  private void registerExpr(Value value, Expression expr) {
    valueExprs.forWrite().put(value.id(), expr);
  }
}
