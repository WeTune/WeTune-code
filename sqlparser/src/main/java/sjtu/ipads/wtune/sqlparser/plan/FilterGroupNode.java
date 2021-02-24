package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;

public interface FilterGroupNode extends FilterNode {
  List<FilterNode> filters();

  @Override
  default OperatorType type() {
    return OperatorType.PlainFilter;
  }

}
