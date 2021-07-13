package sjtu.ipads.wtune.prover.normalform;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.prover.expr.UExpr;

class ConjunctionMask {
  private final Conjunction c;
  final boolean[] tableMask, predMask;
  boolean negMask, squashMask;

  ConjunctionMask(Conjunction c) {
    this.c = c;
    this.tableMask = new boolean[c.tables().size()];
    this.predMask = new boolean[c.preds().size()];
  }

  Conjunction getMasked() {
    final List<UExpr> tables = c.tables();
    final List<UExpr> ts = new ArrayList<>(tables.size());
    for (int i = 0, bound = tables.size(); i < bound; i++) {
      if (tableMask[i]) {
        ts.add(tables.get(i));
      }
    }

    final List<UExpr> preds = c.preds();
    final List<UExpr> ps = new ArrayList<>(preds.size());
    for (int i = 0, bound = preds.size(); i < bound; i++) {
      if (predMask[i]) {
        ps.add(preds.get(i));
      }
    }

    final Disjunction sq = squashMask ? c.squash() : null;
    final Disjunction neg = negMask ? c.neg() : null;

    if (ts.isEmpty() && ps.isEmpty() && sq == null && neg == null) {
      return Conjunction.empty();
    } else {
      return Conjunction.mk(c.vars(), ts, ps, sq, neg);
    }
  }

  Conjunction getComplement() {
    final List<UExpr> tables = c.tables();
    final List<UExpr> ts = new ArrayList<>(tables.size());
    for (int i = 0, bound = tables.size(); i < bound; i++) {
      if (!tableMask[i]) {
        ts.add(tables.get(i));
      }
    }

    final List<UExpr> preds = c.preds();
    final List<UExpr> ps = new ArrayList<>(preds.size());
    for (int i = 0, bound = preds.size(); i < bound; i++) {
      if (!predMask[i]) {
        ps.add(preds.get(i));
      }
    }

    final Disjunction sq = squashMask ? null : c.squash();
    final Disjunction neg = negMask ? null : c.neg();

    if (ts.isEmpty() && ps.isEmpty() && sq == null && neg == null) {
      return Conjunction.empty();
    } else {
      return Conjunction.mk(c.vars(), ts, ps, sq, neg);
    }
  }

  boolean isFullMasked() {
    for (boolean m : tableMask) {
      if (!m) {
        return false;
      }
    }
    for (boolean m : predMask) {
      if (!m) {
        return false;
      }
    }
    return (c.neg() == null || negMask) && (c.squash() == null || squashMask);
  }

  boolean isEmpty() {
    for (boolean m : tableMask) {
      if (m) {
        return false;
      }
    }
    for (boolean m : predMask) {
      if (m) {
        return false;
      }
    }
    return (c.neg() == null || !negMask) && (c.squash() == null || !squashMask);
  }
}
