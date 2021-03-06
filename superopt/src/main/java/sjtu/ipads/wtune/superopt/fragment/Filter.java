package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public interface Filter extends Operator {
  Placeholder fields();
}
