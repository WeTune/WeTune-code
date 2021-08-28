package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.schema.Column;

import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

class ColumnValue implements Value {
  private final Column column;
  private String qualification;
  private final String name;

  ColumnValue(Column column) {
    this.column = column;
    this.name = simpleName(column.name());
  }

  @Override
  public String qualification() {
    return qualification;
  }

  @Override
  public String name() {
    return name;
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
  public void setName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return qualification + '.' + name;
  }

  // equivalence by identity
}
