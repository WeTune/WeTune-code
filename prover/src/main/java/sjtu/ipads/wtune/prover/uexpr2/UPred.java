package sjtu.ipads.wtune.prover.uexpr2;

public interface UPred extends UAtom {
  UVar var();

  static UPred mk(UVar var) {
    return new UPredImpl(var);
  }
}
