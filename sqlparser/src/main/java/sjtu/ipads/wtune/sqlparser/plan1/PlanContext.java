package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.utils.TreeContext;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public interface PlanContext extends TreeContext<PlanContext> {
  Schema schema();

  Value deRef(Ref ref);

  void registerValues(PlanNode node, ValueBag values);

  void registerRefs(PlanNode node, RefBag refs);

  PlanNode ownerOf(Value value);

  PlanNode ownerOf(Ref ref);

  Value sourceOf(Value v);

  void setRef(Ref ref, Value value);

  void changeIndirection(Value oldAttr, Value newAttr);

  boolean validate();

  default List<Value> deRef(List<Ref> refs) {
    return listMap(refs, this::deRef);
  }

  @Override
  default PlanContext dup() {
    return mk(schema());
  }

  static PlanContext mk(Schema schema) {
    return new PlanContextImpl(schema);
  }

  static void install(PlanContext ctx, PlanNode root) {
    root.setContext(ctx);
    for (PlanNode predecessor : root.predecessors()) install(ctx, predecessor);
  }
}
