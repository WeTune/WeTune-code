package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.Collections;
import java.util.List;

public class ProjNodeImpl implements ProjNode {
  private final boolean deduplicated;
  private final List<String> attrNames;
  private final List<Expression> expressions;
  private String qualification;

  public ProjNodeImpl(boolean deduplicated, List<String> attrNames, List<Expression> expressions) {
    this.deduplicated = deduplicated;
    this.attrNames = Collections.unmodifiableList(attrNames);
    this.expressions = Collections.unmodifiableList(expressions);
  }

  @Override
  public boolean deduplicated() {
    return deduplicated;
  }

  @Override
  public List<String> attrNames() {
    return attrNames;
  }

  @Override
  public List<Expression> attrExprs() {
    return expressions;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }
}
