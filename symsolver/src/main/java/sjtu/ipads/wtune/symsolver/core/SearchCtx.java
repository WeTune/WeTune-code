package sjtu.ipads.wtune.symsolver.core;

import java.util.List;

public interface SearchCtx extends Tracer, Prover {
  List<Summary> search(Iterable<DecisionTree> trees);

  void record();
}
