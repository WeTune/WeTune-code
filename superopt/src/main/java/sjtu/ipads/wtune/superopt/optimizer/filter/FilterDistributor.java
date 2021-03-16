package sjtu.ipads.wtune.superopt.optimizer.filter;

public interface FilterDistributor {
  void setNext(FilterDistributor next);

  void distribute(FilterDistribution dist);
}
