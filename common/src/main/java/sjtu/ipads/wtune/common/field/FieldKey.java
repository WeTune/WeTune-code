package sjtu.ipads.wtune.common.field;

public interface FieldKey<T> {
  String name();

  T getFrom(Fields target);

  void setTo(Fields target, T value);
}
