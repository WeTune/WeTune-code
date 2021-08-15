package sjtu.ipads.wtune.superopt.fragment1;

public interface Complexity extends Comparable<Complexity> {
  int[] opCounts();

  int compareTo(Complexity other, boolean preferInnerJoin);
}
