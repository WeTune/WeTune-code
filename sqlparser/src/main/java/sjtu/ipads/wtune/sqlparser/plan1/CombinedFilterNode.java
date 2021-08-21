package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface CombinedFilterNode extends SimpleFilterNode {
  List<FilterNode> filters();

  static CombinedFilterNode mk(List<FilterNode> filters) {
    return CombinedFilterNodeImpl.mk(filters);
  }
}
