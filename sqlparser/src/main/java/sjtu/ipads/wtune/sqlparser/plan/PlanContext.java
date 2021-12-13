package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.common.utils.TreeContext;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface PlanContext extends TreeContext<PlanContext> {
  Set<Ref> refs();

  Set<Value> values();

  Schema schema();

  Value deRef(Ref ref);

  void registerValues(PlanNode node, ValueBag values);

  void registerRefs(PlanNode node, RefBag refs);

  PlanNode ownerOf(Value value);

  PlanNode ownerOf(Ref ref);

  Value sourceOf(Value v);

  void setRedirection(Value redirected, Value destination);

  Value redirect(Value redirected);

  void clearRedirections();

  void setRef(Ref ref, Value value);

  void replaceValue(Value oldAttr, Value newAttr, Set<Ref> excludedRefs);

  boolean validate();

  default List<Value> deRef(List<Ref> refs) {
    return ListSupport.map((Iterable<Ref>) refs, (Function<? super Ref, ? extends Value>) this::deRef);
  }

  default void replaceValue(Value oldAttr, Value newAttr) {
    replaceValue(oldAttr, newAttr, Collections.emptySet());
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
