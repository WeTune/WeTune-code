package sjtu.ipads.wtune.common.attrs;

import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.FuncUtils.deaf;

@FunctionalInterface
public interface FieldKey<T> {
  String name();

  default T get(Fields owner) {
    return get0(owner, this);
  }

  default T set(Fields owner, T obj) {
    if (validate(owner, obj)) return set0(owner, this, obj);
    else return rescue(owner, obj);
  }

  default T unset(Fields owner) {
    return unset0(owner, this);
  }

  default T setIfAbsent(Fields owner, T obj) {
    return setIfAbsent0(owner, this, obj);
  }

  default T setIfAbsent(Fields owner, Supplier<T> supplier) {
    return setIfAbsent0(owner, this, supplier);
  }

  default boolean validate(Object obj) {
    return true;
  }

  default boolean validate(Fields owner, Object obj) {
    return validate(obj);
  }

  default T rescue(Fields owner, Object obj) {
    return null;
  }

  @SuppressWarnings("unchecked")
  static <T> T get0(Fields owner, FieldKey<T> key) {
    return (T) owner.directAttrs().get(key);
  }

  @SuppressWarnings("unchecked")
  static <T> T set0(Fields owner, FieldKey<T> key, T obj) {
    return (T) owner.directAttrs().put(key, obj);
  }

  @SuppressWarnings("unchecked")
  static <T> T unset0(Fields owner, FieldKey<T> key) {
    return (T) owner.directAttrs().remove(key);
  }

  @SuppressWarnings("unchecked")
  static <T> T setIfAbsent0(Fields owner, FieldKey<T> key, T obj) {
    return (T) owner.directAttrs().putIfAbsent(key, obj);
  }

  @SuppressWarnings("unchecked")
  static <T> T setIfAbsent0(Fields owner, FieldKey<T> key, Supplier<T> supplier) {
    return (T) owner.directAttrs().computeIfAbsent(key, deaf(supplier));
  }

  static <T> FieldKey<T> make(String name) {
    return () -> name;
  }

  static <T> FieldKey<T> checked(String name, Class<T> cls) {
    return new FieldKey<>() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public boolean validate(Object obj) {
        return cls.isInstance(obj);
      }
    };
  }
}
