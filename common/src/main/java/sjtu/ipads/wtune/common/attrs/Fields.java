package sjtu.ipads.wtune.common.attrs;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Supplier;

public interface Fields {
  Map<FieldKey, Object> directAttrs();

  default <T> T get(FieldKey<T> key) {
    return key.get(this);
  }

  default <T> T getOr(FieldKey<T> key, T obj) {
    final T t = get(key);
    return t != null ? t : obj;
  }

  default <T> T set(FieldKey<T> key, T obj) {
    return key.set(this, obj);
  }

  default <T> T setIfAbsent(FieldKey<T> key, T obj) {
    return key.setIfAbsent(this, obj);
  }

  default <T> T setIfAbsent(FieldKey<T> key, Supplier<T> obj) {
    return key.setIfAbsent(this, obj);
  }

  default <T> T unset(FieldKey<T> key) {
    return key.unset(this);
  }

  default void flag(FieldKey<Boolean> key) {
    key.set(this, Boolean.TRUE);
  }

  default boolean isFlag(FieldKey<Boolean> key) {
    return key.get(this) == Boolean.TRUE;
  }

  default <E extends Enum<E>> void flag(FieldKey<EnumSet<E>> key, E element) {
    setIfAbsent(key, (Supplier<EnumSet<E>>) () -> EnumSet.noneOf(element.getDeclaringClass()))
        .add(element);
  }

  default <E extends Enum<E>> boolean isFlag(FieldKey<EnumSet<E>> key, E element) {
    final EnumSet<E> es = get(key);
    return es != null && es.contains(element);
  }

  default String stringify(boolean singleLine) {
    final var builder = new StringBuilder();

    if (singleLine) builder.append("{ ");
    for (var e : directAttrs().entrySet())
      builder
          .append("\"")
          .append(e.getKey().name())
          .append("\" = ")
          .append(e.getValue())
          .append(singleLine ? ", " : "\n");

    if (singleLine) builder.delete(builder.length() - 2, Integer.MAX_VALUE).append(" }");

    return builder.toString();
  }

  @SuppressWarnings("unchecked")
  default <T> T unwrap(Class<T> cls) {
    return cls.isInstance(this) ? (T) this : null;
  }
}
