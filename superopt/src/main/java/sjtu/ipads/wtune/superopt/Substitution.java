package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;

public interface Substitution {
  Graph source();

  Graph target();

  Interpretation interpretation();


}
