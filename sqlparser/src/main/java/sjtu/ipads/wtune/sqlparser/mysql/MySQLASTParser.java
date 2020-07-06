package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLLexer;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;

import java.util.Properties;
import java.util.function.Function;

import static sjtu.ipads.wtune.sqlparser.mysql.MySQLRecognizerCommon.NoMode;

public class MySQLASTParser implements SQLParser {
  private long serverVersion = 0;
  private int sqlMode = NoMode;

  public void setServerVersion(long serverVersion) {
    this.serverVersion = serverVersion;
  }

  public void setSqlMode(int sqlMode) {
    this.sqlMode = sqlMode;
  }

  public <T extends ParserRuleContext> T parse0(String str, Function<MySQLParser, T> rule) {
    final MySQLLexer lexer = new MySQLLexer(CharStreams.fromString(str));
    lexer.setServerVersion(serverVersion);
    lexer.setSqlMode(sqlMode);

    final MySQLParser parser = new MySQLParser(new CommonTokenStream(lexer));
    parser.setServerVersion(serverVersion);
    parser.setSqlMode(sqlMode);

    final T ret = rule.apply(parser);
    //    lexer.getInterpreter().clearDFA();
    //    parser.getInterpreter().clearDFA();
    return ret;
  }

  public SQLNode parse(String str, Function<MySQLParser, ParserRuleContext> rule) {
    final SQLNode node = parse0(str, rule).accept(new MySQLASTBuilder());
    if (node != null) {
      node.relinkAll();
      node.setDbTypeRec(SQLNode.MYSQL);
    }
    return node;
  }

  @Override
  public SQLNode parse(String str) {
    return parse(str, MySQLParser::query);
  }

  @Override
  public SQLNode parse(String string, Properties props) {
    setServerVersion((int) props.getOrDefault("serverVersion", this.serverVersion));
    setSqlMode((int) props.getOrDefault("sqlMode", this.sqlMode));

    return parse(string);
  }
}
