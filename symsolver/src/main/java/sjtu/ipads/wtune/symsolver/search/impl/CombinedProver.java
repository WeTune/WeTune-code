package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;

public class CombinedProver implements Prover {
  private final Prover[] provers;

  private CombinedProver(Prover[] provers) {
    this.provers = provers;
  }

  public static Prover build(Prover[] provers) {
    return new CombinedProver(provers);
  }

  @Override
  public void prepare(Decision[] choices) {
    for (Prover prover : provers) prover.prepare(choices);
  }

  @Override
  public void decide(Decision... decisions) {
    for (Prover prover : provers) prover.decide(decisions);
  }

  @Override
  public Result prove() {
    for (Prover prover : provers) {
      final Result result = prover.prove();
      if (result == Result.UNKNOWN) return Result.UNKNOWN;
      else if (result == Result.NON_EQUIVALENT) return Result.NON_EQUIVALENT;
    }
    return Result.EQUIVALENT;
  }

  @Override
  public void tableEq(DecidableConstraint constraint, TableSym tx, TableSym ty) {
    for (Prover prover : provers) prover.tableEq(constraint, tx, ty);
  }

  @Override
  public void pickEq(DecidableConstraint constraint, PickSym px, PickSym py) {
    for (Prover prover : provers) prover.pickEq(constraint, px, py);
  }

  @Override
  public void predicateEq(DecidableConstraint constraint, PredicateSym px, PredicateSym py) {
    for (Prover prover : provers) prover.predicateEq(constraint, px, py);
  }

  @Override
  public void pickFrom(DecidableConstraint constraint, PickSym p, TableSym... src) {
    for (Prover prover : provers) prover.pickFrom(constraint, p, src);
  }

  @Override
  public void reference(DecidableConstraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    for (Prover prover : provers) prover.reference(constraint, tx, px, ty, py);
  }
}
