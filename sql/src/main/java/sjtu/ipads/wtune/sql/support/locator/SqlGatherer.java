package sjtu.ipads.wtune.sql.support.locator;

import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;

public interface SqlGatherer {
  TIntList gather(SqlNode root);

  default SqlNodes gatherNodes(SqlNode root) {
    return SqlNodes.mk(root.context(), gather(root));
  }

  TIntList nodeIds();
}
