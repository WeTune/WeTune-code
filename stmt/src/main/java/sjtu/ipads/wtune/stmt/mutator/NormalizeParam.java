package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.Params;
import sjtu.ipads.wtune.stmt.resolver.Resolution;

import static sjtu.ipads.wtune.sql.ast.ExprFields.PARAM_MARKER_NUMBER;
import static sjtu.ipads.wtune.sql.ast.constants.ExprKind.PARAM_MARKER;

class NormalizeParam {
  public static ASTNode normalize(ASTNode root) {
    Params mgr = root.manager(Params.class);
    if (mgr == null) mgr = Resolution.resolveParamSimple(root);

    for (ParamDesc param : mgr.params()) {
      final ASTNode node = param.node();
      final ASTNode marker = ASTNode.expr(PARAM_MARKER);
      marker.set(PARAM_MARKER_NUMBER, param.index());

      node.update(marker);
    }

    return root;
  }
}
