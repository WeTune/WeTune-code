module sjtu.ipads.wtune.sql {
  exports sjtu.ipads.wtune.sql;
  exports sjtu.ipads.wtune.sql.util;
  exports sjtu.ipads.wtune.sql.ast;
  exports sjtu.ipads.wtune.sql.ast.constants;
  exports sjtu.ipads.wtune.sql.support;
  exports sjtu.ipads.wtune.sql.support.normalize;
  exports sjtu.ipads.wtune.sql.schema;
  exports sjtu.ipads.wtune.sql.relational;
  exports sjtu.ipads.wtune.sql.plan;
  exports sjtu.ipads.wtune.sql.ast1;
  exports sjtu.ipads.wtune.sql.ast1.constants;
    exports sjtu.ipads.wtune.sql.support.locator;

    requires sjtu.ipads.wtune.common;
  requires org.antlr.antlr4.runtime;
  requires org.apache.commons.lang3;
  requires com.google.common;
  requires trove4j;
}
