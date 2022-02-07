module sjtu.ipads.wtune.stmt {
  requires sjtu.ipads.wtune.common;
  requires sjtu.ipads.wtune.sql;
  requires sqlite.jdbc;
  requires java.sql;
  requires com.google.common;
  requires org.apache.commons.lang3;

  exports wtune.stmt;
  exports wtune.stmt.support;
}
