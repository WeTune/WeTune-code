package sjtu.ipads.wtune.superopt.optimizer.internal;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.optimizer.Fingerprint;

public class FragmentFingerprint implements OperatorVisitor {
  private final StringBuilder fingerprint = new StringBuilder();

  public static String make(Fragment g) {
    final FragmentFingerprint calculator = new FragmentFingerprint();
    g.acceptVisitor(calculator);
    return calculator.fingerprint.toString();
  }

  @Override
  public boolean enter(Operator op) {
    if (op instanceof Input) return false;
    fingerprint.append(Fingerprint.charOf(op.type()));
    return true;
  }
}
