package sjtu.ipads.wtune.sqlparser.plan1;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface PlanContext {
  Schema schema();

  Value deRef(Ref ref);

  void registerValues(PlanNode node, ValueBag values);

  void registerRefs(PlanNode node, RefBag refs);

  PlanNode ownerOf(Value value);

  PlanNode ownerOf(Ref ref);

  void setRef(Ref ref, Value value);

  boolean isSameSource(Value v0, Value v1);

  boolean validate();

  default List<Value> deRef(List<Ref> refs) {
    return listMap(refs, this::deRef);
  }

  static PlanContext mk(Schema schema) {
    return new PlanContextImpl(schema);
  }

  static void install(PlanContext ctx, PlanNode root) {
    root.setContext(ctx);
    for (PlanNode predecessor : root.predecessors()) install(ctx, predecessor);
  }
}
