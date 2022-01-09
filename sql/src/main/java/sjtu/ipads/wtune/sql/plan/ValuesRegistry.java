package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.sql.schema.Column;

import java.util.List;
import java.util.Set;

public interface ValuesRegistry {
  Values valuesOf(int nodeId);

  int initiatorOf(Value value);

  Column columnOf(Value value);

  Expression exprOf(Value value);

  Values valueRefsOf(Expression expr);

  void bindValues(int nodeId, List<Value> values);

  void bindValueRefs(Expression expr, List<Value> valueRefs);

  void bindExpr(Value value, Expression expr);
}
