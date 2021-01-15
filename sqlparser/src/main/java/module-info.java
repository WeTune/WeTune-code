module sjtu.ipads.wtune.sqlparser {
  exports sjtu.ipads.wtune.sqlparser;
  exports sjtu.ipads.wtune.sqlparser.rel;
  exports sjtu.ipads.wtune.sqlparser.ast;
  exports sjtu.ipads.wtune.sqlparser.ast.constants;

  requires sjtu.ipads.wtune.common;
  requires org.antlr.antlr4.runtime;
  requires org.apache.commons.lang3;
  requires com.google.common;
}
