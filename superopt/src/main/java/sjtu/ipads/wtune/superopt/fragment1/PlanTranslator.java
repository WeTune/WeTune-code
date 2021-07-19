package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

import java.util.*;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.common.utils.NameSequence.mkIndexed;
import static sjtu.ipads.wtune.superopt.fragment1.Symbol.Kind.*;

class PlanTranslator {
  private Fragment fragment;
  private Constraints constraints;
  private final Map<Symbol, TableDesc> tableDescs;
  private final Map<Symbol, AttrsDesc> attrsDescs;
  private final Map<Symbol, PredDesc> predDescs;

  private final NameSequence tableNameSeq;
  private final NameSequence attrNameSeq;
  private final NameSequence predNameSeq;

  private PlanTranslator(Fragment fragment, Constraints constraints) {
    this.fragment = fragment;
    this.constraints = constraints;

    tableDescs = new LinkedHashMap<>(4);
    attrsDescs = new LinkedHashMap<>(8);
    predDescs = new LinkedHashMap<>(4);
    tableNameSeq = mkIndexed("T", 0);
    attrNameSeq = mkIndexed("a", 0);
    predNameSeq = mkIndexed("p", 0);
  }

  void translate() {
    final Symbols symbols = fragment.symbols();

    assign(symbols.symbolsOf(TABLE), tableDescs, TableDesc::new);
    assign(symbols.symbolsOf(ATTRS), attrsDescs, AttrsDesc::new);
    assign(symbols.symbolsOf(PRED), predDescs, PredDesc::new);

    applyAttrsFrom(symbols.symbolsOf(ATTRS));
    applyAttrsSub(symbols.symbolsOf(ATTRS));

    resolveTables();
    resolveAttrs();
    resolvePreds();
  }

  private <T> void assign(Collection<Symbol> syms, Map<Symbol, T> descs, Supplier<T> supplier) {
    for (Symbol sym : syms) {
      final T desc = descs.get(sym);
      if (desc != null) continue;

      final T newDesc = supplier.get();
      for (Symbol eqSym : constraints.eqClassOf(sym)) descs.put(eqSym, newDesc);
    }
  }

  private void applyAttrsFrom(Collection<Symbol> attrsSyms) {
    for (Symbol attrSym : attrsSyms) {
      final Symbol tableSym = constraints.sourceOf(attrSym);
      if (tableSym == null) continue;

      tableDescs.get(tableSym).attrs.get().add(attrsDescs.get(attrSym));
    }
  }

  private void applyAttrsSub(Collection<Symbol> attrsSyms) {
    for (Symbol xSym : attrsSyms)
      for (Symbol ySym : attrsSyms)
        if (xSym != ySym && constraints.isSubOf(xSym, ySym)) {
          final AttrsDesc xDesc = attrsDescs.get(xSym), yDesc = attrsDescs.get(ySym);
          xDesc.nextLevel = yDesc;
          yDesc.prevLevel = xDesc;
        }
  }

  private void resolveTables() {
    for (TableDesc desc : tableDescs.values()) desc.name = tableNameSeq.next();
  }

  private void resolvePreds() {
    for (PredDesc desc : predDescs.values()) desc.name = predNameSeq.next();
  }

  private void resolveAttrs() {
    for (AttrsDesc attrDesc : attrsDescs.values()) assignCount(attrDesc);

    for (TableDesc tableDesc : tableDescs.values()) {
      final List<String> attrNames = tableDesc.attrNames = new ArrayList<>(4);

      for (AttrsDesc attrsDesc : tableDesc.attrs.get()) {
        final int base = attrNames.size();
        for (int i = 0; i < attrsDesc.count; ++i) attrNames.add(attrNameSeq.next());
        attrsDesc.names = attrNames.subList(base, base + attrsDesc.count);
      }
    }
  }

  private int assignCount(AttrsDesc desc) {
    if (desc == null) return 0;
    if (desc.count != 0) return desc.count;
    return desc.count = assignCount(desc.prevLevel) + 1;
  }

  private static class TableDesc {
    private final Lazy<Set<AttrsDesc>> attrs = Lazy.mk(HashSet::new);
    private String name;
    private List<String> attrNames;
  }

  private static class AttrsDesc {
    private AttrsDesc nextLevel, prevLevel;
    private int count;
    private List<String> names;
  }

  private static class PredDesc {
    private String name;
  }
}
