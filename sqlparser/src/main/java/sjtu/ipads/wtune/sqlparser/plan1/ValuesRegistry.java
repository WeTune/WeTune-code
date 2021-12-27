package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.List;

public interface ValuesRegistry {
  Values valuesOf(int nodeId);

  int initiatorOf(Value value);

  Column columnOf(Value value);

  Expression exprOf(Value value);

  Values valueRefsOf(Expression expr);

  void bindValueRefs(Expression expr, List<Value> valueRefs);
}
