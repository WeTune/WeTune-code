package wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import wtune.sql.plan.PlanContext;
import wtune.sql.plan.PlanNode;

public interface Transformer {
  void setFields(PlanNode planNode, PlanContext planCtx, Context z3Context);

  void dropFields();

  AlgeNode transform();
}
