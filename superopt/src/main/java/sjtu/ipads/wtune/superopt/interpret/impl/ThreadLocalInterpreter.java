package sjtu.ipads.wtune.superopt.interpret.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.interpret.Interpreter;

public class ThreadLocalInterpreter implements Interpreter {
  private final ThreadLocal<Interpretation> interpretation =
      ThreadLocal.withInitial(Interpretation::create);

  private ThreadLocalInterpreter() {}

  public static ThreadLocalInterpreter create() {
    return new ThreadLocalInterpreter();
  }

  @Override
  public String interpreterName() {
    return "global";
  }

  @Override
  public Interpretation interpretation() {
    return interpretation.get();
  }

  @Override
  public void setInterpretation(Interpretation interpretation) {
    this.interpretation.set(interpretation);
  }
}
