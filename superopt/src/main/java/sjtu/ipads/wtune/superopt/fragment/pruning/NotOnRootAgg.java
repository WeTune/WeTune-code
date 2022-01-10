package sjtu.ipads.wtune.superopt.fragment.pruning;

import sjtu.ipads.wtune.superopt.fragment.Agg;

/** Rule that matches an Agg not the root of template. */
public class NotOnRootAgg extends BaseMatchingRule{
  @Override
  public boolean enterAgg(Agg op) {
    if (op.successor() != null) {
      matched = true;
      return false;
    }

    return true;
  }
}