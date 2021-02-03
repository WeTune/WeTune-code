package sjtu.ipads.wtune.superopt.plan;

import java.util.function.Consumer;

public interface PlanVisitor {
  static PlanVisitor traverse(Consumer<PlanNode> consumer) {
    return new PlanVisitor() {
      @Override
      public boolean enter(PlanNode op) {
        consumer.accept(op);
        return true;
      }
    };
  }

  default void enterEmpty(PlanNode parent, int idx) {}

  default boolean enter(PlanNode op) {
    return true;
  }

  default void leave(PlanNode op) {}

  default boolean enterAgg(Agg op) {
    return true;
  }

  default void leaveAgg(Agg op) {}

  default boolean enterDistinct(Distinct op) {
    return true;
  }

  default void leaveDistinct(Distinct op) {}

  default boolean enterInnerJoin(InnerJoin op) {
    return true;
  }

  default void leaveInnerJoin(InnerJoin op) {}

  default boolean enterLeftJoin(LeftJoin op) {
    return true;
  }

  default void leaveLeftJoin(LeftJoin op) {}

  default boolean enterLimit(Limit op) {
    return true;
  }

  default void leaveLimit(Limit op) {}

  default boolean enterPlainFilter(PlainFilter op) {
    return true;
  }

  default void leavePlainFilter(PlainFilter op) {}

  default boolean enterProj(Proj op) {
    return true;
  }

  default void leaveProj(Proj op) {}

  default boolean enterSubqueryFilter(SubqueryFilter op) {
    return true;
  }

  default void leaveSubqueryFilter(SubqueryFilter op) {}

  default boolean enterUnion(Union op) {
    return true;
  }

  default void leaveUnion(Union op) {}

  default boolean enterSort(Sort op) {
    return true;
  }

  default void leaveSort(Sort op) {}

  default boolean enterInput(Input input) {
    return true;
  }

  default void leaveInput(Input input) {}
}
