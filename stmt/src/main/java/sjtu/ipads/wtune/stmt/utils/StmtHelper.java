package sjtu.ipads.wtune.stmt.utils;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class StmtHelper {
  public static String simpleName(String name) {
    if (name == null) return null;
    return unquoted(unquoted(unquoted(name.toLowerCase(), '\''), '`'), '"');
  }

  public static void log(
      System.Logger logger,
      System.Logger.Level level,
      String prefix,
      Statement stmt,
      Object... remaining) {
    logger.log(
        level,
        prefix + "\n<{0}, {1}>\n{2}",
        stmt.appName(),
        stmt.stmtId(),
        stmt.parsed().toString(false),
        remaining);
  }

  public static <T> T newInstance(Class<T> cls) {
    try {
      return cls.getDeclaredConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean nodeEquals(SQLNode n0, SQLNode n1) {
    return (n0 == null && n1 == null)
        || (n0 != null
            && n1 != null
            && Objects.equals(n0.getOr(NODE_ID, -1L), n1.getOr(NODE_ID, -2L)));
  }

  public static int nodeHash(SQLNode n0) {
    return Objects.hashCode(n0.get(NODE_ID));
  }
}
