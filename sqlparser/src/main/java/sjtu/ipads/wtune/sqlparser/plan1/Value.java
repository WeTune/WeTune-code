package sjtu.ipads.wtune.sqlparser.plan1;

public interface Value extends Qualified {
  int id();

  String name();

  static Value mk(int id, String qualification, String name) {
    return new ValueImpl(id, qualification, name);
  }
}
