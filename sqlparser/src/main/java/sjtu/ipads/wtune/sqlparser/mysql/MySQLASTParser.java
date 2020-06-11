package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;

import java.util.function.Function;

public class MySQLASTParser implements SQLParser {
  static <T extends ParserRuleContext> T parse0(String str, Function<MySQLParser, T> rule) {
    return rule.apply(
        new MySQLParser(new CommonTokenStream(new MySQLLexer(CharStreams.fromString(str)))));
  }

  static SQLNode parse(String str, Function<MySQLParser, ParserRuleContext> rule) {
    return parse0(str, rule).accept(new MySQLASTBuilder());
  }

  @Override
  public SQLNode parse(String str) {
    final MySQLParser.QueryContext queryRoot = parse0(str, MySQLParser::query);
    final MySQLParser.SimpleStatementContext simpleStmt = queryRoot.simpleStatement();
    if (simpleStmt == null) return null;

    final MySQLParser.CreateStatementContext createStmt = simpleStmt.createStatement();
    if (createStmt != null) {
      final MySQLParser.CreateTableContext createTable = createStmt.createTable();
      if (createTable != null) return createTable.accept(new MySQLASTBuilder());
    }
    return null;
  }
}
