package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.resolver.Param;
import sjtu.ipads.wtune.stmt.resolver.ParamManager;
import sjtu.ipads.wtune.stmt.resolver.Resolution;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.PARAM_MARKER_NUMBER;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.PARAM_MARKER;

public class NormalizeParam {
  public static SQLNode normalize(SQLNode root) {
    ParamManager mgr = root.manager(ParamManager.class);
    if (mgr == null) mgr = Resolution.resolveParamSimple(root);

    for (Param param : mgr.params()) {
      final SQLNode node = param.node();
      final SQLNode marker = SQLNode.expr(PARAM_MARKER);
      marker.set(PARAM_MARKER_NUMBER, param.index());

      node.update(marker);
    }

    return root;
  }
}
