package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLLexer;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;
import sjtu.ipads.wtune.sqlparser.parser.AstParser;
import sjtu.ipads.wtune.sqlparser.parser.ThrowingErrorListener;

import java.util.Properties;
import java.util.function.Function;

import static sjtu.ipads.wtune.sqlparser.ast1.SqlNode.MySQL;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLRecognizerCommon.NoMode;

public class MySQLAstParser1 implements AstParser {
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

    final MySQLParser parser = new MySQLParser(new CommonTokenStream(lexer));
    parser.setServerVersion(serverVersion);
    parser.setSqlMode(sqlMode);

    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.instance());
    parser.removeErrorListeners();
    parser.addErrorListener(ThrowingErrorListener.instance());

    return rule.apply(parser).accept(new MySQLAstBuilder1());
  }

  @Override
  public SqlNode parse(String string) {
    final SqlNode ast = parse(string, MySQLParser::query);
    ast.context().setDbType(MySQL);
    return ast;
  }

  @Override
  public void setProperties(Properties props) {
    setServerVersion((int) props.getOrDefault("serverVersion", this.serverVersion));
    setSqlMode((int) props.getOrDefault("sqlMode", this.sqlMode));
  }
}
