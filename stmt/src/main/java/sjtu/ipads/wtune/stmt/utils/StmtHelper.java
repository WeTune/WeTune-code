package sjtu.ipads.wtune.stmt.utils;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;

public class StmtHelper {
  public static String simpleName(String name) {
    if (name == null) return null;
    return unquoted(unquoted(unquoted(name.toLowerCase(), '\''), '`'), '"');
  }

  public static String quoteName(String name, String dbType) {
    if (MYSQL.equals(dbType)) return String.format("`%s`", name);
    else return String.format("\"%s\"", name);
  }
}
