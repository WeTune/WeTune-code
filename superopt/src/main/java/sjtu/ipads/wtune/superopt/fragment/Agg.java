package sjtu.ipads.wtune.superopt.fragment;

public interface Agg extends Op {
  // Agg <grpAttrs aggAttrs aggFunc havingPred>
  Symbol groupByAttrs();

  Symbol aggregateAttrs();

  Symbol havingPred();

  Symbol aggFunc();

  @Override
  default OpKind kind() {
    return OpKind.AGG;
  }
}
