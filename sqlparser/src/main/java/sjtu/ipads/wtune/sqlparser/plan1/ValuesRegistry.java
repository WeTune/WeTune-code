package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface ValuesRegistry {
  Values valuesOf(int nodeId);

  Expression exprOf(Value value);

  Values valueRefsOf(Expression expr);

  void bindValueRefs(Expression expr, List<Value> valueRefs);

  ValuesRegistry copy();
}
