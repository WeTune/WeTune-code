package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.function.Function;

public interface MySQLASTParser {
  static <T extends ParserRuleContext> T parse0(String str, Function<MySQLParser, T> rule) {
    final var charStream = CharStreams.fromString(str);
    final var lexer = new MySQLLexer();
    lexer.setInputStream(charStream);
    return rule.apply(new MySQLParser(new CommonTokenStream(lexer)));
  }

  static SQLNode parse(String str, Function<MySQLParser, ParserRuleContext> rule) {
    return parse0(str, rule).accept(new MySQLASTBuilder());
  }
}
