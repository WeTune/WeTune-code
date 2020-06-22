package sjtu.ipads.wtune.common.attrs;

import sjtu.ipads.wtune.common.utils.IPredicate;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public interface Attrs<A extends Attrs<A>> {
  class Key<T> {
    private final String name;
    private final Class<T> type;
    private IPredicate<Attrs<?>> check;

    private Key(String name, Class<T> type) {
      this.name = name;
      this.type = type;
    }

    public static <T> Key<T> of(String name, Class<T> cls) {
      return new Key<>(name, cls);
    }

    @SuppressWarnings("unchecked")
    public static <T> Key<T> of2(String name, Class<?> cls) {
      return new Key<>(name, (Class<T>) cls);
    }

    @SuppressWarnings("unchecked")
    public static <A extends Attrs<A>> IPredicate<Attrs<?>> checkAgainst(
        Class<A> cls, IPredicate<A> check) {
      return it -> cls.isInstance(it) && check.test((A) it);
    }

    public static IPredicate<Attrs<?>> checkEquals(Key<?> conditionKey, Object conditionVal) {
      return it -> Objects.equals(it.get(conditionKey), conditionVal);
    }

    public void setCheck(IPredicate<Attrs<?>> check) {
      this.check = check;
    }

    public void addCheck(IPredicate<Attrs<?>> check) {
      if (this.check == null) this.check = check;
      else this.check = this.check.and(check);
    }

    public IPredicate<? extends Attrs<?>> check() {
      return check;
    }

    public boolean doCheck(Attrs<?> attrs) {
      return check == null || check.test(attrs);
    }

    public String name() {
      return name;
    }

    public Class<T> type() {
      return type;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Key<?> attrKey = (Key<?>) o;
      return name.equals(attrKey.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  static <T> Key<T> key(String name, Class<T> cls) {
    return new Key<>(name, cls);
  }

  @SuppressWarnings("unchecked")
  static <T> Key<T> key2(String name, Class<?> cls) {
    return new Key<>(name, (Class<T>) cls);
  }

  default Map<String, Object> directAttrs() {
    return Holders.get(this);
  }

  default <T> T put(String attrName, T obj) {
    directAttrs().put(attrName, obj);
    return obj;
  }

  default <T> T putIfAbsent(String attrName, T obj) {
    final T val = get(attrName);
    return val == null ? put(attrName, obj) : val;
  }

  default <T> T supplyIfAbsent(String attrName, Supplier<T> obj) {
    final T val = get(attrName);
    return val == null ? put(attrName, obj.get()) : val;
  }

  @SuppressWarnings("unchecked")
  default <T> T get(String attrName) {
    return (T) directAttrs().get(attrName);
  }

  @SuppressWarnings("unchecked")
  default <T> T get(String attrName, Class<T> expected) {
    final Object val = directAttrs().get(attrName);
    return expected.isInstance(val) ? (T) val : null;
  }

  default <T> T getOr(String attrName, T defaultVal) {
    final T val = get(attrName);
    return val == null ? defaultVal : val;
  }

  default <T> T getOr(String attrName, Class<T> expected, T defaultVal) {
    final T val = get(attrName, expected);
    return val == null ? defaultVal : val;
  }

  @SuppressWarnings("unchecked")
  default <T> Map<String, T> ofPrefix(String prefix, Class<T> expected) {
    final Map<String, T> ret = new HashMap<>();

    for (var e : directAttrs().entrySet()) {
      final String k = e.getKey();
      final Object v = e.getValue();

      if (k.startsWith(prefix) && expected.isInstance(v)) ret.put(k, (T) v);
    }

    return ret;
  }

  default Map<String, Object> ofPrefix(String prefix) {
    final Map<String, Object> ret = new HashMap<>();

    for (var e : directAttrs().entrySet()) {
      final String k = e.getKey();
      if (k.startsWith(prefix)) ret.put(k, e.getValue());
    }

    return ret;
  }

  default void flag(String attrName) {
    put(attrName, true);
  }

  default void unFlag(String attrName) {
    put(attrName, false);
  }

  default boolean isFlagged(String attrName) {
    final Object o = directAttrs().get(attrName);
    return (o instanceof Boolean) && ((Boolean) o);
  }

  default <T> T checkFailed(Key<T> key) {
    return null;
  }

  default <T> T put(Key<T> key, T obj) {
    if (!key.doCheck(this)) return checkFailed(key);
    return put(key.name(), obj);
  }

  default <T> T putIfAbsent(Key<T> attrName, T obj) {
    if (!attrName.doCheck(this)) return checkFailed(attrName);
    return putIfAbsent(attrName.name(), obj);
  }

  default <T> T supplyIfAbsent(Key<T> attrName, Supplier<T> obj) {
    if (!attrName.doCheck(this)) return checkFailed(attrName);
    return supplyIfAbsent(attrName.name(), obj);
  }

  default <T> T get(Key<T> key) {
    return get(key.name(), key.type());
  }

  default <T> T getOr(Key<T> key, T defaultVal) {
    return getOr(key.name(), key.type(), defaultVal);
  }

  default void flag(Key<?> attrName) {
    if (!attrName.doCheck(this)) checkFailed(attrName);
    else flag(attrName.name());
  }

  default void unFlag(Key<?> attrName) {
    if (!attrName.doCheck(this)) checkFailed(attrName);
    else unFlag(attrName.name());
  }

  default <E extends Enum<E>> void flag(Key<EnumSet<E>> enumSetKey, E element) {
    if (!enumSetKey.doCheck(this)) checkFailed(enumSetKey);
    else supplyIfAbsent(enumSetKey, () -> EnumSet.noneOf(element.getDeclaringClass())).add(element);
  }

  default <E extends Enum<E>> void unFlag(Key<EnumSet<E>> enumSetKey, E element) {
    if (!enumSetKey.doCheck(this)) checkFailed(enumSetKey);
    else {
      final var enumSet = get(enumSetKey);
      if (enumSet != null) enumSet.remove(element);
    }
  }

  default boolean isFlagged(Key<?> attrName) {
    return isFlagged(attrName.name());
  }

  default <E extends Enum<E>> boolean isFlagged(Key<EnumSet<E>> enumSetKey, E element) {
    final var enumSet = get(enumSetKey);
    return enumSet != null && enumSet.contains(element);
  }

  default String stringify(boolean singleLine) {
    final var builder = new StringBuilder();

    if (singleLine) builder.append("{ ");
    for (var e : directAttrs().entrySet())
      builder
          .append("\"")
          .append(e.getKey())
          .append("\" = ")
          .append(e.getValue())
          .append(singleLine ? ", " : "\n");

    if (singleLine) builder.delete(builder.length() - 2, Integer.MAX_VALUE).append(" }");

    return builder.toString();
  }

  // only for debug
  static int cacheUsed() {
    return Holders.size();
  }
}
