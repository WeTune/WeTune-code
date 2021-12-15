module sjtu.ipads.wtune.spes {
  exports sjtu.ipads.wtune.spes.AlgeNode;
  exports sjtu.ipads.wtune.spes.AlgeNodeParser;
  exports sjtu.ipads.wtune.spes.AlgeRule;
  exports sjtu.ipads.wtune.spes.RexNodeHelper;
  exports sjtu.ipads.wtune.spes.SymbolicRexNode;
  exports sjtu.ipads.wtune.spes.Z3Helper;

  requires com.google.common;
  requires org.apache.commons.lang3;
  requires commons.math3;
  requires annotations;
  requires trove4j;
  requires z3;
  requires calcite.core;
}
