package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.schema.Column;

class ColumnValue implements Value {
  private String qualification;
  private final Column column;

  ColumnValue(Column column) {
    this.column = column;
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public String name() {
    return column.name();
  }

  @Override
  public Column column() {
    return column;
  }

  @Override
  public Expr expr() {
    return null;
  }

  @Override
  public String wildcardQualification() {
    return null;
  }

  @Override
  public void setQualification(String qualification) {
    this.qualification = qualification;
  }

  @Override
  public String toString() {
    return qualification + '.' + column.name();
  }

  // equivalence by identity
}
