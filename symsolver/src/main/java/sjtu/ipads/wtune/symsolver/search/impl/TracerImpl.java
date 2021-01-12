package sjtu.ipads.wtune.symsolver.search.impl;

import sjtu.ipads.wtune.symsolver.DecidableConstraint;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.Tracer;
import sjtu.ipads.wtune.symsolver.utils.DisjointSet;
import sjtu.ipads.wtune.symsolver.core.Indexed;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.symsolver.core.Indexed.isCanonicalIndexed;

public class TracerImpl implements Tracer {
  private final TableSym[] tables;
  private final PickSym[] picks;
  private final PredicateSym[] preds;

  private final List<DecidableConstraint> constraints;

  private final DisjointSet<TableSym> eqTables;
  private final DisjointSet<PickSym> eqPicks;
  private final DisjointSet<PredicateSym> eqPreds;
  private final Map<PickSym, PickSym> refs;
  private final TableSym[][] srcs;

  private final boolean useFastTableIndex;
  private final boolean useFastPickIndex;

  private int fastRejection;

  private int modCount;
  private boolean srcInferred; // indicate if `inferSrc` has been called since last `reset`
  private boolean isConflict;
  private boolean isIncomplete;
  private Summary summary;

  private TracerImpl(TableSym[] tables, PickSym[] picks, PredicateSym[] preds) {
    tables = Arrays.copyOf(tables, tables.length);
    picks = Arrays.copyOf(picks, picks.length);

    this.tables = tables;
    this.picks = picks;
    this.preds = preds;

    constraints = new ArrayList<>();

    eqTables = DisjointSet.fromBoundedMembers(tables);
    eqPicks = DisjointSet.fromBoundedMembers(picks);
    eqPreds = DisjointSet.fromBoundedMembers(preds);

    refs = new HashMap<>();
    srcs = new TableSym[picks.length][];

    useFastTableIndex = isCanonicalIndexed(tables);
    useFastPickIndex = isCanonicalIndexed(picks);
  }

  public static Tracer build(TableSym[] tables, PickSym[] picks, PredicateSym[] preds) {
    if (tables == null || picks == null || preds == null) throw new IllegalArgumentException();
    return new TracerImpl(tables, picks, preds);
  }

  @Override
  public void tableEq(DecidableConstraint constraint, TableSym tx, TableSym ty) {
    addConstraint(constraint);
    eqTables.connect(tx, ty);
  }

  @Override
  public void pickEq(DecidableConstraint constraint, PickSym px, PickSym py) {
    addConstraint(constraint);
    eqPicks.connect(px, py);
  }

  @Override
  public void predicateEq(DecidableConstraint constraint, PredicateSym px, PredicateSym py) {
    addConstraint(constraint);
    eqPreds.connect(px, py);
  }

  @Override
  public void pickFrom(DecidableConstraint constraint, PickSym p, TableSym... src) {
    if (!isSameScoped(p, src)) throw new IllegalArgumentException();

    // sort the array for the convenience of following process
    // according to doc, if `ts` is sorted beforehand, the overhead is only |ts| comparisons.
    Arrays.sort(src, Indexed::compareTo);

    // Check if there is an existing but different assignment
    final int pIdx = indexOf(p);
    final TableSym[] existing = srcs[pIdx];
    if (existing != null) isConflict = isConflict || !Arrays.equals(existing, src);
    else {
      srcs[pIdx] = src;
      addConstraint(constraint);
    }
  }

  @Override
  public void reference(
      DecidableConstraint constraint, TableSym tx, PickSym px, TableSym ty, PickSym py) {
    pickFrom(DecidableConstraint.pickFrom(px, tx), px, tx);
    pickFrom(DecidableConstraint.pickFrom(py, ty), py, ty);
    refs.put(px, py);
    addConstraint(constraint);
  }

  @Override
  public void decide(Decision... decisions) {
    if (fastCheckConflict(decisions)) {
      fastRejection++;
      isConflict = true;
      return;
    }
    reset();
    for (Decision decision : decisions) decision.decide(this);
  }

  /**
   * Check if conflict condition is satisfied: <br>
   * &nbsp;&nbsp;{@code (exists px,py. isEq(px,py) && isMismatch(px.src,py.src)) ||} <br>
   * &nbsp;&nbsp;{@code (exists p. forAll ts in p.viableSources. isMismatch(p.src,ts))} <br>
   *
   * <p>Note that (px === py) => isEq(px,py). So the first branch also rules out the situation that
   * two mismatched sources are assigned to a single pick.
   *
   * <p>Note: This method MUST be invoked after all constraints being added.
   *
   * @see sjtu.ipads.wtune.symsolver.search.Reactor#pickFrom(DecidableConstraint, PickSym,
   *     TableSym...)
   * @see #isMatched(TableSym[], TableSym[])
   */
  @Override
  public boolean isConflict() {
    if (isConflict) return true;

    inferSrc();
    return isConflict;
  }

  /**
   * Check if incomplete condition is satisfied: <br>
   * &nbsp;&nbsp;{@code exists p,p' isEq(p,p') && (not exists t. pickFrom(t,p)) && (not exists t.
   * pickFrom(t,p'))}.
   *
   * <p>This situation happens when exists two picks px,py, they are assigned to be eq, both are not
   * assigned with a source.<br>
   * &nbsp&nbsp;{@code q0: SELECT p0 FROM t0 JOIN t1} <br>
   * &nbsp&nbsp;{@code q1: SELECT p1 FROM t2} <br>
   * Assume we have constraint PickEq(p0,p1) but NO PickFrom(p0,xx) and NO PickFrom(p1,yy), then
   * PickEq(p0,p1) is actually meaningless.
   *
   * <p>Note: This method MUST be invoked after all constraints being added.
   */
  @Override
  public boolean isIncomplete() {
    if (isIncomplete) return true;

    inferSrc();
    return isIncomplete;
  }

  @Override
  public Summary summary() {
    if (summary != null) return summary;

    inferSrc();

    final List<DecidableConstraint> constraints = new ArrayList<>(this.constraints.size() << 1);

    for (int i = 0, bound = tables.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final TableSym tx = tables[i], ty = tables[j];
        if (isEq(tx, ty)) constraints.add(DecidableConstraint.tableEq(tx, ty));
      }

    for (int i = 0, bound = preds.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final PredicateSym px = preds[i], py = preds[j];
        if (isEq(px, py)) constraints.add(DecidableConstraint.predicateEq(px, py));
      }

    for (int i = 0, bound = picks.length; i < bound; i++) {
      final PickSym px = picks[i];
      for (int j = i + 1; j < bound; j++) {
        final PickSym py = picks[j];
        if (isEq(px, py)) constraints.add(DecidableConstraint.pickEq(px, py));
      }

      final TableSym[] srcX = srcs[i];
      if (srcX != null) constraints.add(DecidableConstraint.pickFrom(px, srcX));

      final DecidableConstraint refConstraint = refConstraintOf(px);
      if (refConstraint != null) constraints.add(refConstraint);
    }

    return new SummaryImpl(
        tables, picks, preds, constraints.toArray(DecidableConstraint[]::new), this);
  }

  int modCount() {
    return modCount;
  }

  void inflateSummary(SummaryImpl summary) {
    final int[] tGrouping = eqTables.grouping();
    final int[] cGrouping = eqPicks.grouping();
    final int[] pGrouping = eqPreds.grouping();

    summary.tableGroups = group(tables, tGrouping);
    summary.pickGroups = group(picks, cGrouping);
    summary.predGroups = group(preds, pGrouping);

    final TableSym[][] pivotedSrcs = new TableSym[srcs.length][];
    for (int i = 0; i < srcs.length; i++)
      if (srcs[i] != null)
        pivotedSrcs[i] = arrayMap(t -> tables[tGrouping[indexOf(t)]], TableSym.class, srcs[i]);
    summary.pivotedSources = pivotedSrcs;

    summary.references = refs;
  }

  @Override
  public int numFastRejection() {
    return fastRejection;
  }

  private void reset() {
    ++modCount;

    constraints.clear();
    eqTables.reset();
    eqPicks.reset();
    refs.clear();
    Arrays.fill(srcs, null);

    srcInferred = false;
    isConflict = false;
    isIncomplete = false;
    summary = null;
  }

  private boolean isEq(PredicateSym px, PredicateSym py) {
    return eqPreds.isConnected(px, py);
  }

  private boolean isEq(PickSym px, PickSym py) {
    return eqPicks.isConnected(px, py);
  }

  private boolean isEq(TableSym tx, TableSym ty) {
    return eqTables.isConnected(tx, ty);
  }

  private int indexOf(PickSym p) {
    return useFastPickIndex ? p.index() : Arrays.binarySearch(picks, p, Indexed::compareTo);
  }

  private int indexOf(TableSym t) {
    return useFastTableIndex ? t.index() : Arrays.binarySearch(tables, t, Indexed::compareTo);
  }

  private void inferSrc() {
    // infer a pick's src by the equal ones
    // forAll pick px, py
    //  if is_eq(px, py) && px.src == undefined && py.src == ts
    //   set px.src = ts
    // meanwhile, detect incomplete and conflict property
    if (srcInferred) return;

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i + 1; j < bound; j++) {
        final PickSym px = picks[i], py = picks[j];
        if (!isEq(px, py)) continue;

        TableSym[] srcX = srcs[indexOf(px)], srcY = srcs[indexOf(py)];

        if (srcX != null && srcY == null) tryPickFrom(py, srcX);
        else if (srcX == null && srcY != null) tryPickFrom(px, srcY);
        //
        //        if (srcX == null && srcY == null) isIncomplete = true;
        //        else if (srcX != null && srcY != null) isConflict = isConflict || !isMatched(srcX,
        // srcY);
        //        else if (srcX != null /* && srcY == null */) tryPickFrom(py, srcX);
        //        else /* if (srcX == null && srcY != null) */ tryPickFrom(px, srcY);
      }

    for (int i = 0, bound = picks.length; i < bound; i++)
      for (int j = i; j < bound; j++) {
        final PickSym px = picks[i], py = picks[j];
        if (!isEq(px, py)) continue;

        TableSym[] srcX = srcs[indexOf(px)], srcY = srcs[indexOf(py)];

        if (srcX == null && srcY == null) isIncomplete = true;
        else if (srcX != null && srcY != null)
          isConflict =
              isConflict || !isViable(px, srcX) || !isViable(py, srcY) || !isMatched(srcX, srcY);
        else assert isConflict;
      }

    srcInferred = true;
  }

  private void tryPickFrom(PickSym p, TableSym[] assign) {
    // not a viable source
    final TableSym[] src = find(v -> isMatched(v, assign), p.viableSources());
    if (src == null) isConflict = true;
    else pickFrom(DecidableConstraint.pickFrom(p, src), p, src);
  }

  private void addConstraint(DecidableConstraint constraint) {
    if (!constraint.ignorable()
        && (constraints.isEmpty() || constraints.get(constraints.size() - 1) != constraint))
      constraints.add(constraint);
  }

  private boolean isSameScoped(PickSym p, TableSym... source) {
    if (source.length == 0) return true;

    final Object pickScope = p.scope();
    for (TableSym t : source) if (t.scope() != pickScope) return false;
    return true;
  }

  private boolean isMatched(TableSym[] xs, TableSym[] ys) {
    // Check if two sources are mismatched under current constraints.
    // Matched sources produce the same set of tuples, namely "matching property"
    //
    // Two sources are matched iff:
    //  1. both are of the same length
    //  2. if come from same scope (i.e. from same query), then they must be identical
    //     else, then the following condition must be satisfied:
    //      forAll x in xs, exists y in ys. isEq(x, y) &&
    //      forAll y in ys, exists x in xs. isEq(y, x)
    // Memo: Why we care about the scope?
    //  1. For the same scope, "table sources" are never equivalent, even if the tables are equal.
    //     Put it differently, they always have different "identity". e.g.
    //       q: SELECT * FROM t AS t0 JOIN t AS t1 ON t0.id = t1.child_id
    //    `t0` and `t1` are both `t`. But for q, they play different roles.
    //    Thus, in the case, sources are required to be identical to enforce matching property.
    //  2. For different scope, identity doesn't matter.
    //     Surjection is enough to enforce matching property. (Re-think: is injection necessary?)
    if (xs == ys) return true;
    if (xs.length != ys.length) return false;
    if (xs.length == 0) return true;

    return xs[0].scope() == ys[0].scope()
        ? Arrays.equals(xs, ys) // xs, ys must be sorted, see `pickFrom`.
        : (stream(xs).allMatch(tx -> stream(ys).anyMatch(ty -> isEq(tx, ty)))
            && stream(ys).allMatch(ty -> stream(xs).anyMatch(tx -> isEq(tx, ty))));
  }

  private boolean isViable(PickSym p, TableSym... source) {
    return stream(p.viableSources()).anyMatch(it -> Arrays.equals(it, source));
  }

  private DecidableConstraint refConstraintOf(PickSym px) {
    final TableSym[] srcX = srcs[indexOf(px)];
    if (srcX == null || srcX.length != 1) return null;
    final PickSym py = refs.get(px);
    return py == null ? null : DecidableConstraint.reference(srcX[0], px, srcs[indexOf(py)][0], py);
  }

  private static <T> Collection<Collection<T>> group(T[] xs, int[] grouping) {
    final List<T>[] groups = new List[xs.length];
    for (int i = 0; i < grouping.length; i++) {
      final int groupId = grouping[i];

      List<T> group = groups[groupId];
      if (group == null) group = groups[groupId] = new ArrayList<>();
      group.add(xs[i]);
    }

    return stream(groups).filter(Objects::nonNull).collect(toList());
  }

  private static boolean fastCheckConflict(Decision... constraints) {
    int pickEqStart = -1, pickEqEnd = -1, pickFromStart = -1, pickFromEnd = -1;
    for (int i = 0; i < constraints.length - 1; i++) {
      final Decision c0 = constraints[i], c1 = constraints[i + 1];
      if (c0 instanceof PickFrom
          && c1 instanceof PickFrom
          && ((PickFrom<?, ?>) c0).p() == ((PickFrom<?, ?>) c1).p()) return true;
      if (pickEqStart == -1 && c0 instanceof PickEq) pickEqStart = i;
      if (pickEqEnd == -1 && !(c1 instanceof PickEq)) pickEqEnd = i + 1;
      if (pickFromStart == -1 && c0 instanceof PickFrom) pickFromStart = i;
      if (pickFromEnd == -1 && !(c1 instanceof PickFrom)) pickFromEnd = i + 1;
    }

    if (pickEqStart < 0 || pickFromStart < 0) return false;

    for (int i = pickFromStart; i < pickFromEnd - 1; ++i) {
      final PickFrom<?, ?> fromX = (PickFrom<?, ?>) constraints[i];
      final PickFrom<?, ?> fromY = (PickFrom<?, ?>) constraints[i + 1];

      if (fromX.ts().length != fromY.ts().length)
        for (int j = pickEqStart; j < pickEqEnd; ++j) {
          final PickEq<?> pickEq = (PickEq<?>) constraints[j];
          if (pickEq.px() == fromX.p() && pickEq.py() == fromY.p()) return true;
        }
    }

    return false;
  }
}
