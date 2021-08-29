package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.schema.Column;

class WildcardValue implements Value {
  private final String wildcardQualification;
  private String qualification;

  WildcardValue(String wildcardQualification) {
    this.wildcardQualification = wildcardQualification;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public Column column() {
    return null;
  }

  @Override
  public Expr expr() {
    return null;
  }

  @Override
  public String wildcardQualification() {
    return wildcardQualification;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    if (wildcardQualification == null) return "*";
    else return wildcardQualification + '.' + "*";
  }
}
