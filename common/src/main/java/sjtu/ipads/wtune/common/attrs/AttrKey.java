package sjtu.ipads.wtune.common.attrs;

import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.FuncUtils.dumb;

@FunctionalInterface
public interface AttrKey<T> {
  String name();

  default boolean isPresent(Attrs owner) {
    return isPresent(owner, this);
  }

  default T get(Attrs owner) {
    return get(owner, this);
  }

  default T set(Attrs owner, T obj) {
    if (validate(owner, obj)) return set(owner, this, obj);
    else return rescue(owner, obj);
  }

  default T unset(Attrs owner) {
    return unset(owner, this);
  }

  default T setIfAbsent(Attrs owner, T obj) {
    return setIfAbsent(owner, this, obj);
  }

  default T setIfAbsent(Attrs owner, Supplier<T> supplier) {
    return setIfAbsent(owner, this, supplier);
  }

  default boolean validate(Object obj) {
    return true;
  }

  default boolean validate(Attrs owner, Object obj) {
    return validate(obj);
  }

  default T rescue(Attrs owner, Object obj) {
    return null;
  }

  static boolean isPresent(Attrs owner, AttrKey<?> key) {
    return owner.directAttrs().containsKey(key);
  }

  @SuppressWarnings("unchecked")
  static <T> T get(Attrs owner, AttrKey<T> key) {
    return (T) owner.directAttrs().get(key);
  }

  @SuppressWarnings("unchecked")
  static <T> T set(Attrs owner, AttrKey<T> key, T obj) {
    return (T) owner.directAttrs().put(key, obj);
  }

  @SuppressWarnings("unchecked")
  static <T> T unset(Attrs owner, AttrKey<T> key) {
    return (T) owner.directAttrs().remove(key);
  }

  @SuppressWarnings("unchecked")
  static <T> T setIfAbsent(Attrs owner, AttrKey<T> key, T obj) {
    return (T) owner.directAttrs().putIfAbsent(key, obj);
  }

  @SuppressWarnings("unchecked")
  static <T> T setIfAbsent(Attrs owner, AttrKey<T> key, Supplier<T> supplier) {
    return (T) owner.directAttrs().computeIfAbsent(key, dumb(supplier));
  }

  static <T> AttrKey<T> make(String name) {
    return () -> name;
  }

  static <T> AttrKey<T> checked(String name, Class<T> cls) {
    return new AttrKey<>() {
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
