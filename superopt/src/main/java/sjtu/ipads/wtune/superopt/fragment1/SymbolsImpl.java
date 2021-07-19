package sjtu.ipads.wtune.superopt.fragment1;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.common.utils.Lazy;

import java.util.Collection;

class SymbolsImpl implements Symbols {
  private final Lazy<ListMultimap<Op, Symbol>> tables, attrs, preds;

  SymbolsImpl() {
    tables = Lazy.mk(SymbolsImpl::initMap);
    attrs = Lazy.mk(SymbolsImpl::initMap);
    preds = Lazy.mk(SymbolsImpl::initMap);
  }

  @Override
  public void bindSymbol(Op op) {
    switch (op.type()) {
      case INPUT -> add(op, Symbol.Kind.TABLE);
      case IN_SUB_FILTER, PROJ -> add(op, Symbol.Kind.ATTRS);
      case SIMPLE_FILTER -> {
        add(op, Symbol.Kind.ATTRS);
        add(op, Symbol.Kind.PRED);
      }
      case INNER_JOIN, LEFT_JOIN -> {
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

  private static ListMultimap<Op, Symbol> initMap() {
    return MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
  }

  private ListMultimap<Op, Symbol> getMap(Symbol.Kind kind) {
    return switch (kind) {
      case TABLE -> tables.get();
      case ATTRS -> attrs.get();
      case PRED -> preds.get();
    };
  }

  private void add(Op op, Symbol.Kind kind) {
    getMap(kind).get(op).add(Symbol.mk(kind));
  }
}
