package sjtu.ipads.wtune.superopt.fragment;

import java.util.function.Consumer;

public interface OperatorVisitor {
  static OperatorVisitor traverse(Consumer<Operator> consumer) {
    return new OperatorVisitor() {
      @Override
      public boolean enter(Operator op) {
        consumer.accept(op);
        return true;
      }
    };
  }

  default void enterEmpty(Operator parent, int idx) {}

  default boolean enter(Operator op) {
    return true;
  }

  default void leave(Operator op) {}

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
