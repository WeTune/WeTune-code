package sjtu.ipads.wtune.superopt.fragment1;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.common.utils.Lazy;

import java.util.Collection;
import java.util.Map;

class SymbolsImpl implements Symbols {
  private final Lazy<ListMultimap<Op, Symbol>> tables, attrs, preds;

  SymbolsImpl() {
    tables = Lazy.mk(SymbolsImpl::initMap);
    attrs = Lazy.mk(SymbolsImpl::initMap);
    preds = Lazy.mk(SymbolsImpl::initMap);
  }

  SymbolsImpl(
      ListMultimap<Op, Symbol> tables,
      ListMultimap<Op, Symbol> attrs,
      ListMultimap<Op, Symbol> preds) {
    this.tables = Lazy.mk(tables);
    this.attrs = Lazy.mk(attrs);
    this.preds = Lazy.mk(preds);
  }

  static Symbols merge(Symbols symbols0, Symbols symbols1) {
    final SymbolsImpl s0 = (SymbolsImpl) symbols0, s1 = (SymbolsImpl) symbols1;
    final ListMultimap<Op, Symbol> tables = merge(s0.tables.get(), s1.tables.get());
    final ListMultimap<Op, Symbol> attrs = merge(s0.attrs.get(), s1.attrs.get());
    final ListMultimap<Op, Symbol> preds = merge(s0.preds.get(), s1.preds.get());
    return new SymbolsImpl(tables, attrs, preds);
  }

  private static ListMultimap<Op, Symbol> merge(
      ListMultimap<Op, Symbol> map0, ListMultimap<Op, Symbol> map1) {
    final ListMultimap<Op, Symbol> newMap = initMap();
    newMap.putAll(map0);
    newMap.putAll(map1);
    return newMap;
  }

  @Override
  public void bindSymbol(Op op) {
    switch (op.type()) {
      case INPUT -> add(op, Symbol.Kind.TABLE);
      case IN_SUB_FILTER -> add(op, Symbol.Kind.ATTRS);
      case SIMPLE_FILTER -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.PRED);
      }
      case INNER_JOIN, LEFT_JOIN, PROJ -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.ATTRS);
      }
    }
  }

  @Override
  public Symbol symbolAt(Op op, Symbol.Kind kind, int oridinal) {
    return getMap(kind).get(op).get(oridinal);
  }

  @Override
  public Collection<Symbol> symbolsOf(Symbol.Kind kind) {
    return getMap(kind).values();
  }

  @Override
  public Op ownerOf(Symbol.Kind kind, Symbol symbol) {
    for (Map.Entry<Op, Symbol> entry : getMap(kind).entries()) {
      if (entry.getValue() == symbol) return entry.getKey();
    }
    return null;
  }

  private static ListMultimap<Op, Symbol> initMap() {
    return MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
  }

  private ListMultimap<Op, Symbol> getMap(Symbol.Kind kind) {
    return switch (kind) {
      case TABLE -> tables.get();
      case ATTRS -> attrs.get();
      case PRED -> preds.get();
      default -> throw new IllegalArgumentException();
    };
  }

  private void add(Op op, Symbol.Kind kind) {
    getMap(kind).get(op).add(Symbol.mk(kind));
  }
}
