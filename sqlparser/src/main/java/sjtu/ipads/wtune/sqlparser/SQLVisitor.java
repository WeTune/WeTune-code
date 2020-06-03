package sjtu.ipads.wtune.sqlparser;

public interface SQLVisitor {
  boolean enterTableName(SQLNode tableName);

  void leaveTableName(SQLNode tableName);

  boolean enterColumnName(SQLNode colName);

  void leaveColumnName(SQLNode colName);

  boolean enterCreateTable(SQLNode createTable);

  void leaveCreateTable(SQLNode createTable);

  boolean enterColumnDef(SQLNode colDef);

  void leaveColumnDef(SQLNode colDef);

  boolean enterReferences(SQLNode ref);

  void leaveReferences(SQLNode ref);

  boolean enterIndexDef(SQLNode indexDef);

  void leaveIndexDef(SQLNode indexDef);

  boolean enterKeyPart(SQLNode keyPart);

  void leaveKeyPart(SQLNode keyPart);
}
