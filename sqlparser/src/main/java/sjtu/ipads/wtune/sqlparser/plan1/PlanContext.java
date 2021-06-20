package sjtu.ipads.wtune.sqlparser.plan1;

public interface PlanContext {
  ValueBag values();

  Value deRef(Ref ref);

  void registerValues(PlanNode node, ValueBag values);

  void registerRefs(PlanNode node, RefBag refs);

  void setRef(Ref ref, Value value);
}
