package sjtu.ipads.wtune.sql.mysql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.mysql.internal.MySQLLexer;
import sjtu.ipads.wtune.sql.mysql.internal.MySQLParser;
import sjtu.ipads.wtune.sql.parser.AstParser;
import sjtu.ipads.wtune.sql.parser.ThrowingErrorListener;

import java.util.Properties;
import java.util.function.Function;

import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.mysql.MySQLRecognizerCommon.NoMode;

public class MySQLAstParser implements AstParser {
  private long serverVersion = 0;
  private int sqlMode = NoMode;

  public void setServerVersion(long serverVersion) {
    this.serverVersion = serverVersion;
  }

  public void setSqlMode(int sqlMode) {
    this.sqlMode = sqlMode;
  }

  public SqlNode parse(String str, Function<MySQLParser, ParserRuleContext> rule) {
    final MySQLLexer lexer = new MySQLLexer(CharStreams.fromString(str));
    lexer.setServerVersion(serverVersion);
    lexer.setSqlMode(sqlMode);
    if (str.contains("OVER (")) lexer.setServerVersion(80000);

      final MySQLParser parser = new MySQLParser(new CommonTokenStream(lexer));
    parser.setServerVersion(serverVersion);
    parser.setSqlMode(sqlMode);
    if (str.contains("OVER (")) parser.setServerVersion(80000);

    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.instance());
    parser.removeErrorListeners();
    parser.addErrorListener(ThrowingErrorListener.instance());

    return rule.apply(parser).accept(new MySQLAstBuilder());
  }

  @Override
  public SqlNode parse(String string) {
    final SqlNode ast = parse(string, MySQLParser::query);
    if (ast == null) return null;
    ast.context().setDbType(MySQL);
    return ast;
  }

  @Override
  public void setProperties(Properties props) {
    setServerVersion((int) props.getOrDefault("serverVersion", this.serverVersion));
    setSqlMode((int) props.getOrDefault("sqlMode", this.sqlMode));
  }
}
