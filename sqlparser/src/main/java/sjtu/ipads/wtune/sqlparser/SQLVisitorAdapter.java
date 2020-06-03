package sjtu.ipads.wtune.sqlparser;

public abstract class SQLVisitorAdapter implements SQLVisitor {
  @Override
  public boolean enterCreateTable(SQLNode createTable) {
    return true;
  }

  @Override
  public void leaveCreateTable(SQLNode createTable) {}

  @Override
  public boolean enterTableName(SQLNode tableName) {
    return true;
  }

  @Override
  public void leaveTableName(SQLNode tableName) {}

  @Override
  public boolean enterColumnDef(SQLNode colDef) {
    return true;
  }

  @Override
  public void leaveColumnDef(SQLNode colDef) {}

  @Override
  public boolean enterReferences(SQLNode ref) {
    return true;
  }

  @Override
  public void leaveReferences(SQLNode ref) {}

  @Override
  public boolean enterColumnName(SQLNode colName) {
    return false;
  }

  @Override
  public void leaveColumnName(SQLNode colName) {}

  @Override
  public boolean enterIndexDef(SQLNode indexDef) {
    return true;
  }

  @Override
  public void leaveIndexDef(SQLNode indexDef) {}

  @Override
  public boolean enterKeyPart(SQLNode keyPart) {
    return true;
  }

  @Override
  public void leaveKeyPart(SQLNode keyPart) {}
}
