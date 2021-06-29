package sjtu.ipads.wtune.prover.decision;

import static java.util.Collections.emptySet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class PropositionMemo {
  private int nextId;
  private final Set<Set<Proposition>> disjunction;

  PropositionMemo() {
    this.nextId = 0;
    this.disjunction = new HashSet<>();
    this.disjunction.add(emptySet());
  }

  PropositionMemo(int nextId, Set<Set<Proposition>> disjunction) {
    this.nextId = nextId;
    this.disjunction = disjunction;
  }

  Proposition makeTerm() {
    return new Term("p" + nextId++);
  }

  PropositionMemo add(List<Proposition> props) {
    final Set<Set<Proposition>> newDisjunction = new HashSet<>();

    for (Set<Proposition> conjunction : disjunction) {
      for (Proposition prop : props) {
        if (conjunction.contains(prop)) continue;
        final Set<Proposition> newConjunction = new HashSet<>(conjunction);
        newConjunction.add(prop.not());
        newDisjunction.add(newConjunction);
      }
    }

    return new PropositionMemo(nextId, newDisjunction);
  }

  boolean checkTautology() {
    return disjunction.isEmpty();
  }

  public interface Proposition {
    Proposition not();
  }

  private static final class Term implements Proposition {
    private final String name;

    private Term(String name) {
      this.name = name;
    }

    @Override
    public Proposition not() {
      return new Neg(this);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Term)) return false;
      final Term term = (Term) o;
      return name.equals(term.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static final class Neg implements Proposition {
    private final Term term;

    private Neg(Term term) {
      this.term = term;
    }

    @Override
    public Proposition not() {
      return term;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Neg)) return false;
      final Neg neg = (Neg) o;
      return term.equals(neg.term);
    }

    @Override
    public int hashCode() {
      return term.hashCode() + 1;
    }

    @Override
    public String toString() {
      return "not(" + term + ')';
    }
  }
}
