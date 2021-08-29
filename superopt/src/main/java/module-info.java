module sjtu.ipads.wtune.superopt {
  exports sjtu.ipads.wtune.superopt;

  requires com.google.common;
  requires org.apache.commons.lang3;
  requires java.logging;
  requires sjtu.ipads.wtune.common;
  requires sjtu.ipads.wtune.sqlparser;
  requires sjtu.ipads.wtune.stmt;
  requires sjtu.ipads.wtune.prover;
  requires trove4j;
  requires java.sql;
  requires mysql.connector.java;
  requires org.postgresql.jdbc;
  requires com.zaxxer.hikari;
}
