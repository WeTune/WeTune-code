package sjtu.ipads.wtune.superopt.interpret;

import sjtu.ipads.wtune.superopt.interpret.impl.ThreadLocalInterpreter;

public interface Interpreter {
  Interpretation interpretation();

  String interpreterName();

  void setInterpretation(Interpretation interpretation);

  static Interpreter global() {
    return ThreadLocalInterpreter.create();
  }
}
