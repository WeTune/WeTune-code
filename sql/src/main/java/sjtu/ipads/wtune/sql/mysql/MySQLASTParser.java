package sjtu.ipads.wtune.sql.mysql;

import static sjtu.ipads.wtune.sql.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sql.mysql.MySQLRecognizerCommon.NoMode;

import java.util.Properties;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sql.ASTContext;
import sjtu.ipads.wtune.sql.ASTParser;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.mysql.internal.MySQLLexer;
import sjtu.ipads.wtune.sql.mysql.internal.MySQLParser;

public class MySQLASTParser implements ASTParser {
  public static boolean IS_ERROR_MUTED = false;

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

    if (IS_ERROR_MUTED) {
      lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
      parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
    }

    return rule.apply(parser);
  }

  public ASTNode parse(String str, Function<MySQLParser, ParserRuleContext> rule) {
    return parse(str, true, rule);
  }

  public ASTNode parse(String str, boolean managed, Function<MySQLParser, ParserRuleContext> rule) {
    try {
      final ASTNode root = parse0(str, rule).accept(new MySQLASTBuilder());
      if (root != null && managed) {
        ASTContext.manage(root, ASTContext.build());
        root.context().setDbType(MYSQL);
      }
      return root;

    } catch (Exception exception) {
      return null;
    }
  }

  @Override
  public void setProperties(Properties props) {
    setServerVersion((int) props.getOrDefault("serverVersion", this.serverVersion));
    setSqlMode((int) props.getOrDefault("sqlMode", this.sqlMode));
  }

  @Override
  public ASTNode parse(String string, boolean managed) {
    return parse(string, managed, MySQLParser::query);
  }
}