package sjtu.ipads.wtune.sql.support.locator;

import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;

public interface SqlGatherer {
  TIntList gather(SqlNode root);

  default SqlNodes gatherNodes(SqlNode root) {
    return SqlNodes.mk(root.context(), gather(root));
  }

  TIntList nodeIds();
}
