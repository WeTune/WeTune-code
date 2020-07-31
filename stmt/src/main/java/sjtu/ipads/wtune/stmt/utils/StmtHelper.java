package sjtu.ipads.wtune.stmt.utils;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sqlparser.SQLNode.MYSQL;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class StmtHelper {
  public static String simpleName(String name) {
    if (name == null) return null;
    return unquoted(unquoted(unquoted(name.toLowerCase(), '\''), '`'), '"');
  }

  public static String quoteName(String name, String dbType) {
    if (MYSQL.equals(dbType)) return String.format("`%s`", name);
    else return String.format("\"%s\"", name);
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

  private static final Map<Class<?>, Object> SINGLETONS = new HashMap<>();

  @SuppressWarnings("unchecked")
  private static <T> T getSingleton0(Class<T> cls) {
    try {
      return (T)
          MethodHandles.lookup().findStatic(cls, "singleton", MethodType.methodType(cls)).invoke();
    } catch (Throwable e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T getSingleton(Class<T> cls) {
    return (T) SINGLETONS.computeIfAbsent(cls, StmtHelper::getSingleton0);
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
