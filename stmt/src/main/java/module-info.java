module sjtu.ipads.wtune.stmt {
  exports sjtu.ipads.wtune.stmt.schema;
  exports sjtu.ipads.wtune.stmt.statement;
  exports sjtu.ipads.wtune.stmt.attrs;
  exports sjtu.ipads.wtune.stmt;
  exports sjtu.ipads.wtune.stmt.resovler;
  exports sjtu.ipads.wtune.stmt.analyzer;
  exports sjtu.ipads.wtune.stmt.utils;

  requires sjtu.ipads.wtune.common;
  requires sjtu.ipads.wtune.sqlparser;
  requires sqlite.jdbc;
  requires java.sql;
}
