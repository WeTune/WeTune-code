module sjtu.ipads.wtune.sql {
  exports sjtu.ipads.wtune.sql;
  exports sjtu.ipads.wtune.sql.util;
  exports sjtu.ipads.wtune.sql.support;
  exports sjtu.ipads.wtune.sql.support.action;
  exports sjtu.ipads.wtune.sql.support.locator;
  exports sjtu.ipads.wtune.sql.support.resolution;
  exports sjtu.ipads.wtune.sql.schema;
  exports sjtu.ipads.wtune.sql.plan;
  exports sjtu.ipads.wtune.sql.ast;
  exports sjtu.ipads.wtune.sql.ast.constants;

  requires sjtu.ipads.wtune.common;
  requires org.antlr.antlr4.runtime;
  requires org.apache.commons.lang3;
  requires com.google.common;
  requires trove4j;
}
