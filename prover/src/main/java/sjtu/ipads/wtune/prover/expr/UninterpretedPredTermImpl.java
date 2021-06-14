package sjtu.ipads.wtune.prover.expr;

import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class UninterpretedPredTermImpl extends UExprBase implements UninterpretedPredTerm {
  private final Name name;
  private Tuple tuple;

  public UninterpretedPredTermImpl(Name name, Tuple tuple) {
    this.name = requireNonNull(name);
    this.tuple = requireNonNull(tuple);
  }

  @Override
  public Set<Tuple> rootTuples() {
    return Collections.singleton(tuple.root());
  }

  @Override
  public void subst(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    tuple = tuple.subst(v1, v2);
  }

  @Override
  protected UExprBase copy0() {
    return new UninterpretedPredTermImpl(name, tuple);
  }

  @Override
  public Name name() {
    return name;
  }

  @Override
  public Tuple tuple() {
    return tuple;
  }

  @Override
  public String toString() {
    return "%s<%s>".formatted(name, tuple);
  }
}
