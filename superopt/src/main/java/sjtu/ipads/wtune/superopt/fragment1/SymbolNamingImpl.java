package sjtu.ipads.wtune.superopt.fragment1;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

class SymbolNamingImpl implements SymbolNaming {
  private final BiMap<Symbol, String> names;
  private final NamingStrategy tableNaming, attrsNaming, predNaming;

  SymbolNamingImpl() {
    names = HashBiMap.create();
    tableNaming = new NamingStrategyImpl();
    attrsNaming = new NamingStrategyImpl();
    predNaming = new NamingStrategyImpl();
  }

  @Override
  public SymbolNaming name(Symbols symbols) {
    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.TABLE))
      names.computeIfAbsent(symbol, tableNaming::mkName);

    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.ATTRS))
      names.computeIfAbsent(symbol, attrsNaming::mkName);

    for (Symbol symbol : symbols.symbolsOf(Symbol.Kind.PRED))
      names.computeIfAbsent(symbol, predNaming::mkName);

    return this;
  }

  @Override
  public void setName(Symbol symbol, String name) {
    names.put(symbol, name);
  }

  @Override
  public String nameOf(Symbol symbol) {
    return names.get(symbol);
  }

  @Override
  public Symbol symbolOf(String name) {
    return names.inverse().get(name);
  }

  private interface NamingStrategy {
    String mkName(Symbol symbol);
  }

  private static class NamingStrategyImpl implements NamingStrategy {
    private int nextId;

    @Override
    public String mkName(Symbol symbol) {
      return ((char) (symbol.kind().name().charAt(0) + ('a' - 'A'))) + String.valueOf(nextId++);
    }
  }
}
