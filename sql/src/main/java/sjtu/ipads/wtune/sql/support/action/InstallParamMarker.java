package sjtu.ipads.wtune.sql.support.action;

import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.support.resolution.ParamDesc;
import sjtu.ipads.wtune.sql.support.resolution.Params;

import static sjtu.ipads.wtune.sql.ast.ExprFields.Param_Number;
import static sjtu.ipads.wtune.sql.ast.ExprKind.Param;
import static sjtu.ipads.wtune.sql.support.resolution.Params.PARAMS;

class InstallParamMarker {
  public static void normalize(SqlNode root) {
    final Params params = root.context().getAdditionalInfo(PARAMS);
    params.forEach(InstallParamMarker::installParamMarker);
  }

  private static void installParamMarker(ParamDesc param) {
    final SqlContext context = param.node().context();
    final SqlNode paramMarker = SqlNode.mk(context, Param);
    paramMarker.$(Param_Number, param.index());
    context.displaceNode(param.node().nodeId(), paramMarker.nodeId());
  }
}
