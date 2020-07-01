module sjtu.ipads.wtune.stmt {
  exports sjtu.ipads.wtune.stmt.schema;
  exports sjtu.ipads.wtune.stmt.statement;
  exports sjtu.ipads.wtune.stmt.attrs;
  exports sjtu.ipads.wtune.stmt;
  exports sjtu.ipads.wtune.stmt.resovler;

  requires sjtu.ipads.wtune.common;
  requires sjtu.ipads.wtune.sqlparser;
  requires sqlite.jdbc;
  requires java.sql;
}
