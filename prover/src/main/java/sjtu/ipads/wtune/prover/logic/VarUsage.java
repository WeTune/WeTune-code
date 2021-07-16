package sjtu.ipads.wtune.prover.logic;

import static com.google.common.collect.MultimapBuilder.hashKeys;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.EqPredTerm;
import sjtu.ipads.wtune.prover.uexpr.Name;
import sjtu.ipads.wtune.prover.uexpr.TableTerm;
import sjtu.ipads.wtune.prover.uexpr.UExpr;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;
import sjtu.ipads.wtune.prover.uexpr.UninterpretedPredTerm;
import sjtu.ipads.wtune.prover.uexpr.Var;

class VarUsage {
  private final Map<Var, Name> varInvIndex = new HashMap<>();
  private final Multimap<Name, Var> varIndex = hashKeys(8).arrayListValues(2).build();
  private final Multimap<Var, Name> usage = hashKeys(8).hashSetValues().build();

  private VarUsage() {}

  static VarUsage mk(Disjunction d) {
    final VarUsage usage = new VarUsage();
    usage.collect(d);
    return usage;
  }

  Set<Name> usageOf(Name table) {
    return varIndex.get(table).stream()
        .map(usage::get)
        .map(it -> (Set<Name>) it)
        .reduce(Sets::union)
        .orElse(Collections.emptySet());
  }

  Set<Name> usageOf(Var v) {
    return (Set<Name>) usage.get(v);
  }

  Name tableOf(Var v) {
    return varInvIndex.get(v);
  }

  private void collect(Disjunction d) {
    d.forEach(this::collect);
  }

  private void collect(Conjunction c) {
    for (UExpr e : c.tables()) {
      final TableTerm table = (TableTerm) e;
      varIndex.put(table.name(), table.var());
      varInvIndex.put(table.var(), table.name());
    }

    for (UExpr pred : c.preds())
      if (pred.kind() == Kind.EQ_PRED) {
        final EqPredTerm eq = (EqPredTerm) pred;
        collect(eq.lhs());
        collect(eq.rhs());
      } else {
        final UninterpretedPredTerm p = (UninterpretedPredTerm) pred;
        for (Var var : p.vars()) collect(var);
      }

    if (c.squash() != null) collect(c.squash());
    if (c.neg() != null) collect(c.neg());
  }

  private void collect(Var var) {
    if (var.isBase() || var.isConstant()) return;

    if (var.isFunc()) for (Var base : var.base()) collect(base);

    if (var.isProjected()) {
      final Var base = var.base()[0];
      if (base.isBase()) {
        usage.put(base, var.name());
        return;
      }
      collect(base);
    }
  }
}
