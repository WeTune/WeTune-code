package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

final class TableTermImpl extends UExprBase implements TableTerm {
  private final Name table;
  private Tuple tuple;

  TableTermImpl(Name table, Tuple tuple) {
    this.table = requireNonNull(table);
    this.tuple = requireNonNull(tuple);
  }

  @Override
  public void subst(Tuple v1, Tuple v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    tuple = tuple.subst(v1, v2);
  }

  @Override
  public boolean uses(Tuple v) {
    return tuple.uses(v);
  }

  @Override
  public Tuple tuple() {
    return tuple;
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
