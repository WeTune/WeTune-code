package sjtu.ipads.wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanNode;

public interface Transformer {
  void setFields(PlanNode planNode, PlanContext planCtx, Context z3Context);

  void dropFields();

  AlgeNode transform();
}