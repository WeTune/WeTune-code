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
  UVar srcOutVar, tgtOutVar;

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

  public UVar sourceOutVar() {
    return srcOutVar;
  }

  public UVar targetOutVar() {
    return tgtOutVar;
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

  public String tableNameOf(Symbol sym) {
    final TableDesc tableDesc = symToTable.get(sym);
    return tableDesc == null ? null : tableDesc.term().tableName().toString();
  }

  public String attrsNameOf(Symbol sym) {
    final AttrsDesc attrsDesc = symToAttrs.get(sym);
    return attrsDesc == null ? null : attrsDesc.name().toString();
  }

  public String predNameOf(Symbol sym) {
    final PredDesc predDesc = symToPred.get(sym);
    return predDesc == null ? null : predDesc.name().toString();
  }
}
