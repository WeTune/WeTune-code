package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.SymbolNaming;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;

class ConstraintStringifier {
  private final SymbolNaming naming;
  private final boolean canonical;
  private final StringBuilder builder;

  ConstraintStringifier(SymbolNaming naming, boolean canonical, StringBuilder builder) {
    this.naming = naming;
    this.canonical = canonical;
    this.builder = builder;
  }

  StringBuilder stringify(Constraint c) {
    builder.append(c.kind().name()).append('(');
    if (!canonical) {
      Commons.joining(",", asList(c.symbols()), builder, naming::nameOf);
    } else {
      final List<String> symNames = ListSupport.map(asList(c.symbols()), naming::nameOf);
      if (c.kind().isEq()) symNames.sort(Commons::compareStringLengthFirst);
      Commons.joining(",", symNames, builder);
    }
    builder.append(')');
    return builder;
  }

  StringBuilder stringify(Constraints C) {
    if (!canonical) {
      for (Constraint c : C) {
        stringify(c);
        builder.append(';');
      }
    } else {
      final List<String> strings = new ArrayList<>(C.size());
      for (Constraint c : C) strings.add(c.canonicalStringify(naming));
      strings.sort(String::compareTo);
      for (String string : strings) builder.append(string).append(';');
    }

    appendInstantiation(C, TABLE);
    appendInstantiation(C, ATTRS);
    appendInstantiation(C, PRED);
    appendInstantiation(C, SCHEMA);
    removeTrailing();
    return builder;
  }

  private void appendInstantiation(Constraints C, Symbol.Kind kind) {
    final String name = Constraint.Kind.eqOfSymbol(kind).name();
    for (Symbol sym : C.targetSymbols().symbolsOf(kind)) {
      builder.append(name).append('(');
      builder.append(naming.nameOf(sym)).append(',');
      builder.append(naming.nameOf(C.instantiationOf(sym))).append(')');
      builder.append(';');
    }
  }

  private void removeTrailing() {
    final int lastIndex = builder.length() - 1;
    if (builder.charAt(lastIndex) == ';') builder.deleteCharAt(lastIndex);
  }
}
