module sjtu.ipads.wtune.stmt {
  requires sjtu.ipads.wtune.common;
  requires sjtu.ipads.wtune.sqlparser;
  requires sqlite.jdbc;
  requires java.sql;
  requires com.google.common;
  requires org.apache.commons.lang3;

  exports sjtu.ipads.wtune.stmt;
  exports sjtu.ipads.wtune.stmt.support;
}
