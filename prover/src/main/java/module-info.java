module sjtu.ipads.wtune.prover {
  exports sjtu.ipads.wtune.prover;
  exports sjtu.ipads.wtune.prover.uexpr;
  exports sjtu.ipads.wtune.prover.normalform;
  exports sjtu.ipads.wtune.prover.logic;

  requires sjtu.ipads.wtune.common;
  requires sjtu.ipads.wtune.stmt;
  requires sjtu.ipads.wtune.sqlparser;
  requires com.google.common;
  requires org.apache.commons.lang3;
  requires commons.math3;
  requires annotations;
  requires trove4j;
  requires z3;
}
