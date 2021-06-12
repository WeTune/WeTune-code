package sjtu.ipads.wtune.prover.expr;

import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class TableTermImpl extends UExprBase implements TableTerm {
  private final Name table;
  private Tuple tuple;

  TableTermImpl(Name table, Tuple tuple) {
    this.table = requireNonNull(table);
    this.tuple = requireNonNull(tuple);
  }

  @Override
  public Set<Tuple> rootTuples() {
    return Collections.singleton(tuple.root());
  }

  @Override
  public void replace(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    tuple = tuple.subst(v1, v2);
  }

  @Override
  public Name name() {
    return table;
  }

  @Override
  public UExprBase copy0() {
    return new TableTermImpl(table, tuple);
  }

  @Override
  public String toString() {
    return table + "(" + tuple + ")";
  }
}
