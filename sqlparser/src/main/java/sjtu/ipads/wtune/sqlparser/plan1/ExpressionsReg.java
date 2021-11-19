package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface ExpressionsReg {
  void bindValueRefs(Expression expr, List<Value> valueRefs);

  Values valueRefsOf(Expression expr);
}
