package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;

class SortNodeImpl implements SortNode {
  private final List<Expression> sortSpec;

  SortNodeImpl(List<Expression> sortSpec) {
    this.sortSpec = sortSpec;
  }

  @Override
  public List<Expression> sortSpec() {
    return sortSpec;
  }
}
