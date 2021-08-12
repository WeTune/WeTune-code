package sjtu.ipads.wtune.prover.logic;

import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import sjtu.ipads.wtune.prover.normalform.Conjunction;
import sjtu.ipads.wtune.prover.normalform.Disjunction;
import sjtu.ipads.wtune.prover.uexpr.*;
import sjtu.ipads.wtune.prover.uexpr.UExpr.Kind;

import java.util.*;

import static com.google.common.collect.MultimapBuilder.hashKeys;

class SymbolLookup {
  private final Map<Var, Name> varInvIndex = new HashMap<>();
  private final Multimap<Name, Var> varIndex = hashKeys(8).arrayListValues(2).build();
  private final SetMultimap<Var, Name> attrs = hashKeys(8).hashSetValues().build();
  private final TObjectIntMap<Name> funcs = new TObjectIntHashMap<>(2);
  private final TObjectIntMap<Name> preds = new TObjectIntHashMap<>(2);
  private final Set<Name> consts = new HashSet<>();

  private SymbolLookup() {}

  static SymbolLookup mk(Disjunction d) {
    final SymbolLookup usage = new SymbolLookup();
    usage.gather(d);
    return usage;
  }

  Set<Name> funcs() {
    return funcs.keySet();
  }

  Set<Name> preds() {
    return preds.keySet();
  }

  Set<Name> tables() {
    return varIndex.keySet();
  }

  Set<Var> vars() {
    return Sets.union(attrs.keySet(), varInvIndex.keySet());
  }

  Set<Name> consts() {
    return consts;
  }

  Name tableOf(Var v) {
    return varInvIndex.get(v);
  }

  Set<Name> attrsOf(Name table) {
    return varIndex.get(table).stream()
        .map(attrs::get)
        .reduce(Sets::union)
        .orElse(Collections.emptySet());
  }

  Set<Name> attrsOf(Var v) {
    return attrs.get(v);
  }

  int predArityOf(Name name) {
    return preds.get(name);
  }

  int funcArityOf(Name name) {
    return funcs.get(name);
  }

  private void gather(Disjunction d) {
    d.forEach(this::gather);
  }

  private void gather(Conjunction c) {
    for (UExpr e : c.tables()) {
      final TableTerm table = (TableTerm) e;
      varIndex.put(table.name(), table.var());
      varInvIndex.put(table.var(), table.name());
    }

    for (UExpr pred : c.preds())
      if (pred.kind() == Kind.EQ_PRED) {
        final EqPredTerm eq = (EqPredTerm) pred;
        gather(eq.lhs());
        gather(eq.rhs());
      } else {
        final UninterpretedPredTerm p = (UninterpretedPredTerm) pred;
        preds.put(p.name(), p.vars().length);

        for (Var var : p.vars()) gather(var);
      }

    if (c.squash() != null) gather(c.squash());
    if (c.neg() != null) gather(c.neg());
  }

  private void gather(Var var) {
    if (var.isBase()) return;

    if (var.isConstant()) {
      consts.add(var.name());
      return;
    }

    if (var.isFunc()) {
      funcs.put(var.name(), var.base().length);
      for (Var base : var.base()) gather(base);
    }

    if (var.isProjected()) {
      final Var base = var.base()[0];
      if (base.isBase()) {
        attrs.put(base, var.name());
        return;
      }
      gather(base);
    }
  }
}
