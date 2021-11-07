package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import sjtu.ipads.wtune.prover.uexpr2.UExpr;
import sjtu.ipads.wtune.prover.uexpr2.UName;
import sjtu.ipads.wtune.prover.uexpr2.UVar;
import sjtu.ipads.wtune.superopt.fragment.Symbol;

import java.util.HashMap;
import java.util.Map;

public class TranslationResult {
  public final BiMap<Symbol, UName> symToName;
  public final Map<UName, UVar> tableToVar;
  UExpr expr;
  UVar freeVar;

  TranslationResult() {
    this.symToName = HashBiMap.create(16);
    this.tableToVar = new HashMap<>(8);
  }
}
