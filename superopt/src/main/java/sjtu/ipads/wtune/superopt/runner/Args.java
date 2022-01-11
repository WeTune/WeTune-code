package sjtu.ipads.wtune.superopt.runner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.apache.commons.lang3.ClassUtils.isPrimitiveWrapper;
import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.ListSupport.elemAt;

record Arg(String key, String value) {}

class Args extends AbstractList<Arg> {
  private final Map<String, String> pairs = new LinkedHashMap<>();
  private final List<Arg> args = new ArrayList<>();

  static Args parse(String[] args, int begin) {
    final Args ret = new Args();

    int index = 0;
    for (String arg : args) {
      if (index++ < begin) continue;

      if (arg.startsWith("-")) {
        final int splitIndex = arg.indexOf('=');
        if (splitIndex == -1) ret.add(arg.substring(1), null);
        else ret.add(arg.substring(1, splitIndex), arg.substring(splitIndex + 1));
      } else {
        ret.add(null, arg);
      }
    }

    return ret;
  }

  @Override
  public Arg get(int index) {
    return elemAt(args, index);
  }

  String get(String key) {
    if (key.startsWith("-")) return pairs.get(key.substring(1));
    else return pairs.get(key);
  }

  int countPositional() {
    int count = 0;
    for (int i = args.size() - 1; i >= 0; --i) {
      if (args.get(i).key() == null) ++count;
      else break;
    }
    return count;
  }

  void add(String key, String value) {
    args.add(new Arg(key, value));
    if (key != null) pairs.put(key, value);
  }

  <T> T getRequired(String key, Class<T> cls) {
    final String v = get(key);
    if (v != null) return convertTo(v, cls);
    else if (cls == boolean.class || cls == Boolean.class) {
      return (T) Boolean.valueOf(pairs.containsKey(key));
    } else {
      throw new IllegalArgumentException("missing required argument: " + key);
    }
  }

  <T> T getPositional(int index, Class<T> cls) {
    final Arg arg;
    final int count = countPositional();

    if (index >= 0) {
      if (index >= count) throw new IllegalArgumentException("missing positional argument");
      arg = args.get(args.size() - count + index);
    } else {
      if (-index > count) throw new IllegalArgumentException("missing positional argument");
      arg = elemAt(args, index);
    }

    assert arg != null && arg.key() == null;

    return convertTo(arg.value(), cls);
  }

  <T> T getOptional(String key, Class<T> cls, T defaultVal) {
    final String v = get(key);
    if (v != null) return convertTo(v, cls);
    else if (pairs.containsKey(key) && (cls == boolean.class || cls == Boolean.class))
      return (T) Boolean.valueOf(true);
    else return defaultVal;
  }

  private static <T> T convertTo(String value, Class<T> cls) {
    Class<?> destClass = cls.isPrimitive() ? primitiveToWrapper(cls) : cls;
    if (destClass == String.class) {
      return (T) value;

    } else if (isPrimitiveWrapper(destClass)) {
      final Method convertMethod;
      try {
        convertMethod = destClass.getMethod("valueOf", String.class);
        return (T) convertMethod.invoke(null, value);

      } catch (NoSuchMethodException e) {
        return assertFalse();

      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }

    } else {
      throw new IllegalArgumentException("unsupported type: " + cls);
    }
  }

  @Override
  public int size() {
    return args.size();
  }
}
