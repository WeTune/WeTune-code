package sjtu.ipads.wtune.superopt.optimization.internal;

public interface FilterDistributor {
  void setNext(FilterDistributor next);

  void distribute(FilterDistribution dist);
}
