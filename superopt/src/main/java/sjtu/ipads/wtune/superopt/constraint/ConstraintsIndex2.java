package sjtu.ipads.wtune.superopt.constraint;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.fragment.Symbols;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.*;

abstract class ConstraintsIndex2 implements List<Constraint> {
  private final Fragment source, target;

  protected ConstraintsIndex2(Fragment source, Fragment target) {
    this.source = source;
    this.target = target;
  }

  Collection<Symbol> viableSourcesOf(Symbol attrs) {
    return Collections.emptyList();
  }

  int beginIndexOfKind(Constraint.Kind kind) {
    return 0;
  }

  int endIndexOfKind(Constraint.Kind kind) {
    return 0;
  }

  int beginIndexOfEq(Symbol.Kind kind) {
    switch (kind) {
      case TABLE:
        return beginIndexOfKind(TableEq);
      case ATTRS:
        return beginIndexOfKind(AttrsEq);
      case PRED:
        return beginIndexOfKind(PredicateEq);
      default:
        throw new IllegalArgumentException();
    }
  }

  int endIndexOfEq(Symbol.Kind kind) {
    switch (kind) {
      case TABLE:
        return endIndexOfKind(TableEq);
      case ATTRS:
        return endIndexOfKind(AttrsEq);
      case PRED:
        return endIndexOfKind(PredicateEq);
      default:
        throw new IllegalArgumentException();
    }
  }

  int beginIndexOfInstantiation(Symbol.Kind kind) {
    return -1;
  }

  int endIndexOfInstantiation(Symbol.Kind kind) {
    return -1;
  }

  int indexOfInstantiation(Symbol from, Symbol to) {
    return -1;
  }

  int indexOfEq(Symbol sym0, Symbol sym1) {
    return -1;
  }

  Substitution mkRule(boolean[] enabled) {
    return null; // TODO
  }

  Symbols sourceSymbols() {
    return source.symbols();
  }

  Symbols targetSymbols() {
    return target.symbols();
  }
}
