package sjtu.ipads.wtune.superopt.fragment;

public interface Limit extends Op {
  @Override
  default OpKind kind() {
    return OpKind.LIMIT;
  }
}
