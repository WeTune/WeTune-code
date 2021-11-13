package sjtu.ipads.wtune.common.field;

import sjtu.ipads.wtune.common.utils.MapLike;

import java.util.EnumSet;

public interface Fields extends MapLike<FieldKey<?>, Object> {
  <T> T getField(FieldKey<T> field);

  <T> void setField(FieldKey<T> field, T value);

  default <T> T $(FieldKey<T> field) {
    return getField(field);
  }

  default <T> void $(FieldKey<T> field, T value) {
    setField(field, value);
  }

  default <T extends Enum<T>> void flag(FieldKey<EnumSet<T>> key, T value) {
    EnumSet<T> set = $(key);
    if (set == null) $(key, set = EnumSet.noneOf(value.getDeclaringClass()));
    set.add(value);
  }

  default void flag(FieldKey<Boolean> key) {
    $(key, true);
  }

  default boolean isFlag(FieldKey<Boolean> key) {
    return $(key) == Boolean.TRUE;
  }

  default <T extends Enum<T>> boolean isFlag(FieldKey<EnumSet<T>> key, T value) {
    final EnumSet<T> set = $(key);
    return set != null && set.contains(value);
  }

  @Override
  default Object get(Object key) {
    if (!(key instanceof FieldKey))
      throw new IllegalArgumentException("only accept FieldKey as key");
    return getField((FieldKey<?>) key);
  }
}
