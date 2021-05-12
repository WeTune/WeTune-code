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
    boolean inComment = false;
    boolean escape = false;
    boolean hyphen = false;
    int start = 0;

    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);

      if (inComment) {
        assert !inSql;
        if (c == '\n' || c == '\r') inComment = false;
        continue;
      }

      if (!inSql) {
        if (Character.isSpaceChar(c) || c == '\n' || c == '\r') continue;
        else {
          inSql = true;
          start = i;
        }
      }

      if (c != '-') hyphen = false;

      switch (c) {
        case '\\':
          escape = true;
          continue;

        case '`':
        case '"':
        case '\'':
          if (!escape) inQuote = !inQuote;
          break;

        case '-':
          if (!inQuote) {
            if (!hyphen) hyphen = true;
            else {
              if (start < i - 1) list.add(str.substring(start, i - 1));
              inComment = true;
              inSql = false;
              hyphen = false;
            }
          }
          continue;

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
