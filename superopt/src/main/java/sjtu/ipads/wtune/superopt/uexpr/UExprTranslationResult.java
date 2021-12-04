package sjtu.ipads.wtune.superopt.uexpr;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UExprTranslationResult {
  final Substitution rule;
  final Map<Symbol, TableDesc> symToTable;
  final Map<Symbol, AttrsDesc> symToAttrs;
  final Map<Symbol, PredDesc> symToPred;
  final TObjectIntMap<Symbol> symSchemas;
  final TObjectIntMap<UVar> varSchemas;
  UTerm srcExpr, tgtExpr;
  UVar srcFreeVar, tgtFreeVar;

  UExprTranslationResult(Substitution rule) {
    this.rule = rule;
    this.symToTable = new HashMap<>(8);
    this.symToAttrs = new HashMap<>(16);
    this.symToPred = new HashMap<>(8);
    this.symSchemas = new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE, 8, 0.75f, 0);
    this.varSchemas = new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE, 16, 0.75f, 0);
  }

  public Collection<TableDesc> tableTerms() {
    return symToTable.values();
  }

  public Substitution rule() {
    return rule;
  }

  public UTerm sourceExpr() {
    return srcExpr;
  }

  public UTerm targetExpr() {
    return tgtExpr;
  }

  public UVar sourceFreeVar() {
    return srcFreeVar;
  }

  public UVar targetFreeVar() {
    return tgtFreeVar;
  }

  public int schemaOf(UVar var) {
    return varSchemas.get(var);
  }

  public int schemaOf(Symbol sym) {
    return symSchemas.get(sym);
  }

  public TableDesc tableDescOf(Symbol sym) {
    return symToTable.get(sym);
  }

  public AttrsDesc attrsDescOf(Symbol sym) {
    return symToAttrs.get(sym);
  }

  public PredDesc predDescOf(Symbol sym) {
    return symToPred.get(sym);
  }
}
