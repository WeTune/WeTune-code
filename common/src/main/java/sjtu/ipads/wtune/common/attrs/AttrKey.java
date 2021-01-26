package sjtu.ipads.wtune.common.attrs;

import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.FuncUtils.dumb;

@FunctionalInterface
public interface AttrKey<T> {
  String name();

  @SuppressWarnings("unchecked")
  default T get(Attrs owner) {
    return (T) owner.directAttrs().get(this);
  }

  @SuppressWarnings("unchecked")
  default T getOr(Attrs owner, T defaultVal) {
    return (T) owner.directAttrs().getOrDefault(this, defaultVal);
  }

  @SuppressWarnings("unchecked")
  default T set(Attrs owner, Object obj) {
    if (validate(owner, obj)) return (T) owner.directAttrs().put(this, obj);
    else return rescue(owner, obj);
  }

  default void unset(Attrs owner) {
    owner.directAttrs().remove(name());
  }

  @SuppressWarnings("unchecked")
  default T setIfAbsent(Attrs owner, Object obj) {
    return (T) owner.directAttrs().putIfAbsent(this, obj);
  }

  @SuppressWarnings("unchecked")
  default T setIfAbsent(Attrs owner, Supplier<T> supplier) {
    return (T) owner.directAttrs().computeIfAbsent(this, dumb(supplier));
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
