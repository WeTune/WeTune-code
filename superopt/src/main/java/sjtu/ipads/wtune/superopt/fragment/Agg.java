package sjtu.ipads.wtune.superopt.fragment;

public interface Agg extends Op {
  @Override
  default OpKind kind() {
    return OpKind.AGG;
  }
}
