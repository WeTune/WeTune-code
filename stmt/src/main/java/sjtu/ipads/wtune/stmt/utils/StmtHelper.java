package sjtu.ipads.wtune.stmt.utils;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;

public class StmtHelper {
  public static String simpleName(String name) {
    return unquoted(unquoted(unquoted(name, '\''), '`'), '"');
  }
}
