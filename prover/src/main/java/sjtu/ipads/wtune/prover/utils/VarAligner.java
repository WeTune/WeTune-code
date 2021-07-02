package sjtu.ipads.wtune.prover.utils;

import static java.lang.Math.abs;
import static java.util.Arrays.binarySearch;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.prover.utils.Util.substBoundedVars;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.math3.util.Combinations;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.Conjunction;

public class VarAligner implements Iterator<VarAlignment> {
  private static final int ALIGN_C0 = 1;
  private static final int ALIGN_C1 = 2;
  private static final int FLIP = 4;

  private final Conjunction c0, c1;
  private final int mode;
  private final int numVars;
  private final List<Tuple> vars;
  private final int[][] viable;
  private Iterator<int[]> combinations;
  private int[] assignments0;
  private int[] assignments1;
  private boolean hasNext;

  private VarAligner(Conjunction c0, Conjunction c1, int mode, int numVars, List<Tuple> vars) {
    this.c0 = c0;
    this.c1 = c1;
    this.mode = mode;
    this.numVars = numVars;
    this.vars = vars;
    this.viable = calcViableAssignments();
    this.hasNext = forward();
  }

  public static Iterable<VarAlignment> alignVars(Conjunction c0, Conjunction c1, List<Tuple> vars) {
    if (vars.isEmpty()) return singletonList(new VarAlignment(c0.copy(), c1.copy()));

    final boolean flip = c0.vars().size() > c1.vars().size();
    final Conjunction theC0 = flip ? c1 : c0;
    final Conjunction theC1 = flip ? c0 : c1;

    final int numVars = vars.size();
    if (numVars > theC0.vars().size()) {
      throw new IllegalArgumentException("too many vars");
    }

    return () ->
        new VarAligner(theC0, theC1, ALIGN_C0 | ALIGN_C1 | (flip ? FLIP : 0), numVars, vars);
  }

  public static Iterable<VarAlignment> alignVars(Conjunction c0, Conjunction c1, int numVars) {
    if (numVars == 0) return singletonList(new VarAlignment(c0.copy(), c1.copy()));

    final boolean flip = c0.vars().size() > c1.vars().size();
    final Conjunction theC0 = flip ? c1 : c0;
    final Conjunction theC1 = flip ? c0 : c1;

    if (numVars > theC0.vars().size()) {
      throw new IllegalArgumentException("too many vars");
    }
    final List<Tuple> vars = flip ? c1.vars() : c0.vars();

    return () -> new VarAligner(theC0, theC1, flip ? (FLIP | ALIGN_C0) : ALIGN_C1, numVars, vars);
  }

  private static String[] buildVarIndex(List<Tuple> vars, List<UExpr> tables) {
    final String[] index = new String[vars.size()];
    for (int i = 0, iBound = vars.size(); i < iBound; i++) {
      final Tuple var = vars.get(i);
      final TableTerm table = (TableTerm) find(it -> ((TableTerm) it).tuple().equals(var), tables);
      if (table != null) {
        index[i] = table.name().toString();
      }
    }
    return index;
  }

  private int[][] calcViableAssignments() {
    final List<Tuple> vars0 = c0.vars(), vars1 = c1.vars();
    final List<UExpr> tables0 = c0.tables(), tables1 = c1.tables();

    final String[] varIdx0 = buildVarIndex(vars0, tables0);
    final String[] varIdx1 = buildVarIndex(vars1, tables1);

    final int[][] ret = new int[vars0.size()][];

    for (int i = 0, iBound = vars0.size(); i < iBound; i++) {
      final String table0 = varIdx0[i];
      final TIntList list = new TIntArrayList(varIdx1.length);
      for (int j = 0, jBound = varIdx1.length; j < jBound; j++) {
        final String table1 = varIdx1[j];
        if (table0 == null || table1 == null || table0.equals(table1)) {
          list.add(j);
        }
      }
      ret[i] = list.toArray();
    }

    return ret;
  }

  private boolean forwardAssignment(int idx) {
    if (idx < 0) {
      return false;
    }

    final int prevVal0 = assignments0[idx];
    final int prevVal1 = assignments1[idx];

    if (prevVal1 == -1 && idx > 0) {
      if (!forwardAssignment(idx - 1)) {
        return false;
      }
    }

    final int[] viable = this.viable[prevVal0];
    final int curPos = binarySearch(viable, prevVal1);
    final int nextPos = abs(curPos + 1);

    if ((assignments1[idx] = findNextAssignable(viable, nextPos)) >= 0) {
      return true;
    } else {
      return forwardAssignment(idx - 1) && (assignments1[idx] = findNextAssignable(viable, 0)) >= 0;
    }
  }

  private boolean forwardMask() {
    if (!combinations.hasNext()) {
      return false;
    }
    assignments0 = combinations.next();
    Arrays.fill(assignments1, -1);
    return true;
  }

  private boolean forward() {
    if (assignments0 == null) {
      // initial step.
      combinations = new Combinations(c0.vars().size(), numVars).iterator();
      assignments0 = combinations.next();
      assignments1 = new int[assignments0.length];
      Arrays.fill(assignments1, -1);
    }

    if (forwardAssignment(assignments0.length - 1)) {
      return true;
    }
    while (forwardMask()) {
      if (forwardAssignment(assignments0.length - 1)) {
        return true;
      }
    }

    return false;
  }

  private boolean hasBeenAssigned(int i) {
    for (int assignment : assignments1) {
      if (assignment == i) {
        return true;
      }
    }
    return false;
  }

  private int findNextAssignable(int[] viable, int cursor) {
    while (cursor < viable.length) {
      final int viableVal = viable[cursor];
      if (!hasBeenAssigned(viableVal)) {
        return viableVal;
      }
      ++cursor;
    }
    return -1;
  }

  private Conjunction makeAligned(Conjunction original, int[] assignments) {
    final List<Tuple> newVars = new ArrayList<>(original.vars());
    for (int i = 0; i < assignments.length; i++) {
      newVars.set(assignments[i], vars.get(i));
    }
    return substBoundedVars(original, newVars);
  }

  private VarAlignment makeAlignment() {
    final Conjunction newC0 = ((mode & ALIGN_C0) != 0) ? makeAligned(c0, assignments0) : c0.copy();
    final Conjunction newC1 = ((mode & ALIGN_C1) != 0) ? makeAligned(c1, assignments1) : c1.copy();
    return ((mode & FLIP) != 0) ? new VarAlignment(newC1, newC0) : new VarAlignment(newC0, newC1);
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public VarAlignment next() {
    final VarAlignment ret = makeAlignment();
    hasNext = forward();
    return ret;
  }
}
