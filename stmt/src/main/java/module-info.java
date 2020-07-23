module sjtu.ipads.wtune.stmt {
  exports sjtu.ipads.wtune.stmt.schema;
  exports sjtu.ipads.wtune.stmt.statement;
  exports sjtu.ipads.wtune.stmt.attrs;
  exports sjtu.ipads.wtune.stmt;
  exports sjtu.ipads.wtune.stmt.resolver;
  exports sjtu.ipads.wtune.stmt.analyzer;
  exports sjtu.ipads.wtune.stmt.utils;
  exports sjtu.ipads.wtune.stmt.scriptgen;
  exports sjtu.ipads.wtune.stmt.context;
  exports sjtu.ipads.wtune.stmt.dao;
  exports sjtu.ipads.wtune.stmt.similarity.output;
  exports sjtu.ipads.wtune.stmt.mutator;

  requires transitive sjtu.ipads.wtune.common;
  requires transitive sjtu.ipads.wtune.sqlparser;
  requires sqlite.jdbc;
  requires java.sql;

  requires com.google.common;
}
