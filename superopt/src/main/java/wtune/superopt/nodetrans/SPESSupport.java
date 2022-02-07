package wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.spes.AlgeRule.AlgeRule;
import wtune.sql.plan.PlanContext;

public class SPESSupport {
  public static boolean prove(PlanContext plan1, PlanContext plan2) {
    try (final Context ctx = new Context()) {
      BaseTransformer.resetEnv();
      AlgeNode algeNode0 = AlgeRule.normalize(plan2AlgeNode(plan1, ctx));
      AlgeNode algeNode1 = AlgeRule.normalize(plan2AlgeNode(plan2, ctx));

      if (algeNode0 == null || algeNode1 == null) return false;

      return algeNode0.isEq(algeNode1);
    } catch (Exception e) {
      // e.printStackTrace();
      return false;
    }
  }

  public static AlgeNode plan2AlgeNode(PlanContext planCtx, Context z3Context) {
    return BaseTransformer.transformNode(planCtx.planRoot(), planCtx, z3Context);
  }
}