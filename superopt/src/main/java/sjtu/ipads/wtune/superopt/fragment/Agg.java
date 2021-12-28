package sjtu.ipads.wtune.superopt.fragment;

public interface Agg extends Op {
  // Agg <grpAttrs aggAttrs havingPred>
  Symbol groupByAttrs();

  Symbol aggregateAttrs();

  Symbol havingPred();

  // Symbol aggFunc(); remain to be considered

  @Override
  default OpKind kind() {
    return OpKind.AGG;
  }
}
