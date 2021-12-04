package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.ArraySupport;
import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.ArraySupport.map;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

class ConstraintImpl implements Constraint {
  private final Kind kind;
  private final Symbol[] symbols;

  ConstraintImpl(Kind kind, Symbol[] symbols) {
    this.kind = kind;
    this.symbols = symbols;
  }

  static Constraint parse(String str, SymbolNaming naming) {
    final String[] fields = str.split("[(),\\[\\] ]+");
    final Kind kind = Kind.valueOf(fields[0].replace("Pick", "Attrs") /* backward compatible */);

    if (fields.length != kind.numSyms() + 1)
      throw new IllegalArgumentException("invalid serialized constraint: " + str);

    final Symbol[] symbols =
        ArraySupport.map(asList(fields).subList(1, fields.length), naming::symbolOf, Symbol.class);

    return new ConstraintImpl(kind, symbols);
  }

  @Override
  public Kind kind() {
    return kind;
  }

  @Override
  public Symbol[] symbols() {
    return symbols;
  }

  @Override
  public String canonicalStringify(SymbolNaming naming) {
    final List<String> symNames = listMap(symbols, naming::nameOf);
    if (kind.isEq()) {
      symNames.sort(
          (x, y) -> {
            if (x.length() < y.length()) return -1;
            if (x.length() > y.length()) return 1;
            return x.compareTo(y);
          });
    }

    final StringBuilder builder = new StringBuilder();
    builder.append(kind.name()).append('(');
    Commons.joining(",", symNames, builder);
    builder.append(')');
    return builder.toString();
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    builder.append(kind.name()).append('(');
    Commons.joining(",", asList(symbols), builder, naming::nameOf);
    builder.append(')');
    return builder;
  }

  @Override
  public String toString() {
    return kind.name();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Constraint)) return false;
    final Constraint that = (Constraint) o;
    return kind == that.kind() && Arrays.equals(symbols, that.symbols());
  }

  @Override
  public int hashCode() {
    int result = kind.hashCode();
    result = 31 * result + Arrays.hashCode(symbols);
    return result;
  }
}
