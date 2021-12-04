package sjtu.ipads.wtune.superopt.uexpr;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.superopt.uexpr.UExprSupport.transformSubTerms;
import static sjtu.ipads.wtune.superopt.uexpr.UKind.*;

class UNormalization {
  private boolean isModified;

  private UNormalization() {
    isModified = false;
  }

  static UTerm normalizeTerm(UTerm expr) {
    return new UNormalization().normalizeTerm0(expr);
  }

  private UTerm normalizeTerm0(UTerm expr) {
    // Essential normalization.
    expr = mergeSummation(expr);
    // Not critical, just make `expr` more readable.
    expr = eliminateSquash(expr);
    return expr;
  }

  // squash(..squash(..)..) -> squash(..)
  private UTerm eliminateSquash(UTerm expr) {
    expr = eliminateSquash0(expr, false);
    expr = flatAddAndMul(expr);
    return expr;
  }

  private UTerm eliminateSquash0(UTerm expr, boolean isActivated) {
    final UKind kind = expr.kind();
    if (isActivated && kind == SQUASH) {
      isModified = true;
      return eliminateSquash0(((USquash) expr).body(), true);
    } else {
      final boolean activated = isActivated || kind == SQUASH || kind == NEGATION;
      return transformSubTerms(expr, t -> eliminateSquash0(t, activated));
    }
  }

  // add/mul[ .., add/mul[..], .. ] -> add/mul[..,..,..]
  private UTerm flatAddAndMul(UTerm expr) {
    expr = transformSubTerms(expr, this::flatAddAndMul);

    final UKind kind = expr.kind();
    if (kind != ADD && kind != MULTIPLY) return expr;

    final List<UTerm> subTerms = expr.subTerms();
    for (int i = 0; i < subTerms.size(); ++i) {
      final UTerm subTerm = subTerms.get(i);
      if (subTerm.kind() == kind) subTerms.addAll(subTerm.subTerms());
    }

    isModified = subTerms.removeIf(t -> t.kind() == kind);
    return expr;
  }

  // Sum[x](Sum[y](..)) -> Sum[x,y](..)
  private UTerm mergeSummation(UTerm expr) {
    expr = transformSubTerms(expr, this::mergeSummation);
    if (expr.kind() != SUMMATION) return expr;

    final USum summation = (USum) expr;
    final Set<UVar> boundedVars = summation.boundedVars();
    assert summation.body().kind() == MULTIPLY;

    // Sum[x](Prod(..,Sum[y](..),..) -> Sum[x,y](..,..,..)
    final List<UTerm> subTerms = summation.body().subTerms();
    for (int i = 0; i < subTerms.size(); i++) {
      final UTerm subTerm = subTerms.get(i);
      if (subTerm.kind() != SUMMATION) continue;
      final USum subSummation = (USum) subTerm;
      boundedVars.addAll(subSummation.boundedVars());
      subTerms.addAll(subSummation.body().subTerms());
      isModified = true;
    }
    subTerms.removeIf(it -> it.kind() == SUMMATION);

    return expr;
  }
}
