package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Prover;

import java.util.Collection;

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
  public void decide(Decision[] decisions) {
    for (Prover prover : provers) prover.decide(decisions);
  }

  @Override
  public boolean prove() {
    for (Prover prover : provers) if (!prover.prove()) return false;
    return true;
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    for (Prover prover : provers) prover.tableEq(constraint, tx, ty);
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    for (Prover prover : provers) prover.pickEq(constraint, px, py);
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> ts) {
    for (Prover prover : provers) prover.pickFrom(constraint, p, ts);
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    for (Prover prover : provers) prover.reference(constraint, tx, px, ty, py);
  }
}
