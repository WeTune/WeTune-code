package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.sqlparser.mysql.MySQLASTParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static sjtu.ipads.wtune.sqlparser.SQLNode.MYSQL;

public interface SQLParser {
  SQLNode parse(String string);

  SQLNode parse(String string, Properties props);

  static SQLParser ofDb(String dbType) {
    if (MYSQL.equals(dbType)) return new MySQLASTParser();
    else return null;
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
        if (Character.isSpaceChar(c)) continue;
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

    if (str.charAt(str.length() - 1) != ';') list.add(str.substring(start));

    return list;
  }
}
