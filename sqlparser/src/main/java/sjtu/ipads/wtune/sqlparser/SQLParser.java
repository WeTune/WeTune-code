package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.mysql.MySQLASTParser;
import sjtu.ipads.wtune.sqlparser.pg.PGASTParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.POSTGRESQL;

public interface SQLParser {

  SQLNode parse(String string, boolean managed);

  default void setProperties(Properties props) {}

  default SQLNode parseRaw(String string) {
    return parse(string, false);
  }

  default SQLNode parse(String string) {
    return parse(string, true);
  }

  static SQLParser ofDb(String dbType) {
    if (MYSQL.equals(dbType)) return new MySQLASTParser();
    else if (POSTGRESQL.equals(dbType)) return new PGASTParser();
    else throw new IllegalArgumentException();
  }

  static SQLParser mysql() {
    return ofDb(MYSQL);
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
          if (!inQuote) list.add(str.substring(start, i));
          inSql = false;
          break;
      }
      escape = false;
    }

    if (inSql) list.add(str.substring(start));

    return list;
  }
}
