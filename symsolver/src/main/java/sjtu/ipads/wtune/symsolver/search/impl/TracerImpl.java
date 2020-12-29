package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Summary;
import sjtu.ipads.wtune.symsolver.search.Tracer;
import sjtu.ipads.wtune.symsolver.utils.DisjointSet;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static sjtu.ipads.wtune.symsolver.utils.Indexed.isCanonicalIndexed;

public class TracerImpl implements Tracer {
  private final TableSym[] tables;
  private final PickSym[] picks;

  private final List<Constraint> constraints;

  private final DisjointSet<TableSym> eqTables;
  private final DisjointSet<PickSym> eqPicks;
  private final Map<PickSym, PickSym> refs;
  private final Collection<TableSym>[] srcs;

  private final boolean useFastTableIndex;
  private final boolean useFastPickIndex;

  private boolean srcDiverged; // indicate if two different source is assigned to a single pick
  private boolean srcInferred; // indicate if `inferSrc` has been called since last `reset`
  private Summary summary;

  private TracerImpl(TableSym[] tables, PickSym[] picks) {
    tables = Arrays.copyOf(tables, tables.length);
    picks = Arrays.copyOf(picks, picks.length);

    this.tables = tables;
    this.picks = picks;

    constraints = new ArrayList<>();
    eqTables = DisjointSet.fromBoundedMembers(tables);
    eqPicks = DisjointSet.fromBoundedMembers(picks);
    refs = new HashMap<>();
    srcs = new Collection[picks.length];

    useFastTableIndex = isCanonicalIndexed(tables);
    useFastPickIndex = isCanonicalIndexed(picks);
  }

  public static Tracer build(TableSym[] tables, PickSym[] picks) {
    if (tables == null || picks == null) throw new IllegalArgumentException();
    return new TracerImpl(tables, picks);
  }

  private static <T> Collection<Collection<T>> group(T[] xs, int[] grouping) {
    final List<T>[] groups = new List[xs.length];
    for (int i = 0; i < grouping.length; i++) {
      final int groupId = grouping[i];

      List<T> group = groups[groupId];
      if (group == null) group = groups[groupId] = new ArrayList<>();
      group.add(xs[i]);
    }

    return Arrays.stream(groups).filter(Objects::nonNull).collect(toList());
  }

  @Override
  public void tableEq(Constraint constraint, TableSym tx, TableSym ty) {
    addConstraint(constraint);
    eqTables.connect(tx, ty);
  }

  @Override
  public void pickEq(Constraint constraint, PickSym px, PickSym py) {
    addConstraint(constraint);
    eqPicks.connect(px, py);
  }

  @Override
  public void pickFrom(Constraint constraint, PickSym p, Collection<TableSym> ts) {
    addConstraint(constraint);
    final int pIdx = indexOf(p);
    final Collection<TableSym> existing = srcs[pIdx];

    if (existing != null && !existing.equals(ts)) srcDiverged = true;
    else srcs[pIdx] = ts;
  }

  @Override
  public void reference(Constraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    addConstraint(constraint);
    pickFrom(constraint, px, singletonList(tx));
    pickFrom(constraint, py, singletonList(ty));
    refs.put(px, py);
  }

  private void addConstraint(Constraint constraint) {
    if (constraints.isEmpty() || constraints.get(constraints.size() - 1) != constraint)
      constraints.add(constraint);
  }

  @Override
  public void decide(Decision[] decisions) {
    reset();
    for (Decision decision : decisions) decision.decide(this);
  }

  private void reset() {
    constraints.clear();
    eqTables.reset();
    eqPicks.reset();
    refs.clear();
    Arrays.fill(srcs, null);
    srcDiverged = false;
    srcInferred = false;
    summary = null;
  }

  /**
   * Check if there are
   *
   * <ul>
   *   <li>two picks px, py such that: px == py && px.src != py.src
   *   <li>two different sources being assigned to a single pick
   * </ul>
   *
   * <p>Note: This method MUST be invoked after all constraints being added.
   */
  @Override
  public boolean isConflict() {
    inferSrc();

    if (srcDiverged) return true;

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final PickSym px = picks[i], py = picks[j];
        if (!eqPicks.isConnected(px, py)) continue;

        final Collection<TableSym> srcX = srcs[indexOf(px)], srcY = srcs[indexOf(py)];
        if (srcX != null && srcY != null && isMismatchedSource(srcX, srcY)) return true;
      }
    return false;
  }

  private boolean isMismatchedSource(Collection<TableSym> xs, Collection<TableSym> ys) {
    return xs.size() != ys.size()
        || xs.stream().anyMatch(tx -> ys.stream().noneMatch(ty -> eqTables.isConnected(tx, ty)))
        || ys.stream().anyMatch(ty -> xs.stream().noneMatch(tx -> eqTables.isConnected(tx, ty)));
  }

  /**
   * Check if there is a pick p such that <br>
   * (exists t in p.src && forAll t' in p.visibleTables, t != t')
   *
   * <p>Note: This method MUST be invoked after all constraints being added.
   */
  @Override
  public boolean isIncomplete() {
    inferSrc();
    for (PickSym pick : picks) {
      final Collection<TableSym> src = srcs[indexOf(pick)];
      if (src == null) continue;

      final Collection<TableSym> vs = pick.visibleSources();
      if (src.stream().anyMatch(st -> vs.stream().noneMatch(vt -> eqTables.isConnected(st, vt))))
        return true;
    }
    return false;
  }

  @Override
  public Summary summary() {
    if (summary != null) return summary;

    inferSrc();

    final int[] tGrouping = eqTables.grouping();
    final int[] pGrouping = eqPicks.grouping();

    final Collection<Collection<TableSym>> eqTables = group(tables, tGrouping);
    final Collection<Collection<PickSym>> eqPicks = group(picks, pGrouping);

    final Collection<TableSym>[] srcPivot = new Collection[srcs.length];
    for (int i = 0; i < srcs.length; i++)
      if (srcs[i] != null)
        srcPivot[i] = srcs[i].stream().map(t -> tables[tGrouping[indexOf(t)]]).collect(toList());

    return new SummaryImpl(
        new ArrayList<>(constraints), eqTables, eqPicks, srcPivot, new HashMap<>(refs));
  }

  private void inferSrc() {
    if (srcInferred) return;
    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final PickSym px = picks[i], py = picks[j];
        if (!eqPicks.isConnected(px, py)) continue;

        final Collection<TableSym> srcX = srcs[indexOf(px)], srcY = srcs[indexOf(py)];
        if (srcX == null && srcY != null) pickFrom(Constraint.pickFrom(px, srcY), px, srcY);
        else if (srcX != null && srcY == null) pickFrom(Constraint.pickFrom(py, srcX), py, srcX);
      }
    srcInferred = true;
  }

  private int indexOf(PickSym p) {
    return useFastPickIndex ? p.index() : Arrays.binarySearch(picks, p, Indexed.INDEX_CMP);
  }

  private int indexOf(TableSym t) {
    return useFastTableIndex ? t.index() : Arrays.binarySearch(tables, t, Indexed.INDEX_CMP);
  }

  private static final class SummaryImpl implements Summary {
    private final Collection<Constraint> constraints;
    private final Collection<Collection<TableSym>> eqTables;
    private final Collection<Collection<PickSym>> eqPicks;
    private final Collection<TableSym>[] srcs;
    private final Map<PickSym, PickSym> refs;

    private SummaryImpl(
        Collection<Constraint> constraints,
        Collection<Collection<TableSym>> eqTables,
        Collection<Collection<PickSym>> eqPicks,
        Collection<TableSym>[] srcs,
        Map<PickSym, PickSym> refs) {
      this.constraints = constraints;
      this.eqTables = eqTables;
      this.eqPicks = eqPicks;
      this.srcs = srcs;
      this.refs = refs;
    }

    private static <T> boolean impliesEq(
        Collection<Collection<T>> xs, Collection<Collection<T>> ys) {
      return ys.stream().allMatch(y -> xs.stream().anyMatch(x -> x.containsAll(y)));
    }

    private static boolean impliesRef(Map<PickSym, PickSym> refsX, Map<PickSym, PickSym> refsY) {
      for (var refY : refsY.entrySet())
        if (refsX.get(refY.getKey()) != refY.getValue()) return false;
      return true;
    }

    @Override
    public Collection<Constraint> constraints() {
      return constraints;
    }

    @Override
    public Collection<Collection<TableSym>> eqTables() {
      return eqTables;
    }

    @Override
    public Collection<Collection<PickSym>> eqPicks() {
      return eqPicks;
    }

    @Override
    public Collection<TableSym>[] srcs() {
      return srcs;
    }

    @Override
    public Map<PickSym, PickSym> refs() {
      return refs;
    }

    @Override
    public boolean implies(Summary other) {
      return impliesEq(eqTables(), other.eqTables())
          && impliesEq(eqPicks(), other.eqPicks())
          && impliesRef(refs(), other.refs());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SummaryImpl summary = (SummaryImpl) o;
      return Objects.equals(eqTables, summary.eqTables)
          && Objects.equals(eqPicks, summary.eqPicks)
          && Arrays.equals(srcs, summary.srcs)
          && Objects.equals(refs, summary.refs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(eqTables, eqPicks, refs) + Arrays.hashCode(srcs);
    }

    @Override
    public String toString() {
      return "eqTables: "
          + eqTables
          + ", eqPicks: "
          + eqPicks
          + ", refs: "
          + refs
          + ", srcs: "
          + Arrays.toString(srcs);
    }
  }

  public static void main(String[] args) {
    List<Integer> xs = emptyList(), ys = singletonList(1);
    System.out.println(ys.stream().anyMatch(y -> xs.stream().noneMatch(x -> x.equals(y))));
  }
}
