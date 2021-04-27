package sjtu.ipads.wtune.sqlparser;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.POSTGRESQL;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.mysql.MySQLASTParser;
import sjtu.ipads.wtune.sqlparser.pg.PGASTParser;

public interface ASTParser {

  ASTNode parse(String string, boolean managed);

  default void setProperties(Properties props) {}

  default ASTNode parseRaw(String string) {
    return parse(string, false);
  }

  default ASTNode parse(String string) {
    return parse(string, true);
  }

  static ASTParser ofDb(String dbType) {
    if (MYSQL.equals(dbType)) return new MySQLASTParser();
    else if (POSTGRESQL.equals(dbType)) return new PGASTParser();
    else throw new IllegalArgumentException();
  }

  static ASTParser mysql() {
    return ofDb(MYSQL);
  }

  static ASTParser postgresql() {
    return ofDb(POSTGRESQL);
  }

  static List<String> splitSql(String str) {
    final List<String> list = new ArrayList<>(str.length() / 100);

    boolean inSql = false;
    boolean inQuote = false;
    boolean escape = false;
    int start = 0;

    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);

      if (!inSql) {
        if (Character.isSpaceChar(c) || c == '\n' || c == '\r') continue;
        else {
          inSql = true;
          start = i;
        }
      }

      switch (c) {
        case '\\':
          escape = true;
          continue;
        case '`':
        case '"':
        case '\'':
          if (!escape) inQuote = !inQuote;
          break;
        case ';':
          if (!inQuote) {
            final String sql = str.substring(start);
            if (!sql.startsWith("--")) list.add(sql);
          }
          inSql = false;
          break;
      }
      escape = false;
    }

    if (inSql) {
      final String sql = str.substring(start);
      if (!sql.startsWith("--")) list.add(sql);
    }

    return list;
  }
}
