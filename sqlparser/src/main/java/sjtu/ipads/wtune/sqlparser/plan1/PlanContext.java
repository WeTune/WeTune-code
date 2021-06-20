package sjtu.ipads.wtune.sqlparser.plan1;

public interface PlanContext {
  Value deRef(Ref ref);

  void registerValues(PlanNode node, ValueBag values);

  void registerRefs(PlanNode node, RefBag refs);

  PlanNode ownerOf(Value value);

  PlanNode ownerOf(Ref ref);

  void setRef(Ref ref, Value value);

  static PlanContext build() {
    return new PlanContextImpl();
  }
}
