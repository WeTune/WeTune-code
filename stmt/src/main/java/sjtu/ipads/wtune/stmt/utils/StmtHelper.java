package sjtu.ipads.wtune.stmt.utils;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.lang.reflect.InvocationTargetException;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;

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
}
