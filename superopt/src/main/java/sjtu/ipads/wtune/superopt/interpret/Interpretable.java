package sjtu.ipads.wtune.superopt.interpret;

public interface Interpretable<T> {
  T unwrap();

  Interpreter interpreter();

  void setInterpreter(Interpreter interpreter);

  boolean checkEquals(Interpretable<T> t);
}
