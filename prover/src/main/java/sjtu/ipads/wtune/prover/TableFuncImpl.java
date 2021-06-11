package sjtu.ipads.wtune.prover;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Set;

class TableFuncImpl implements TableFunc {
  private final Name table;
  private Tuple tuple;

  TableFuncImpl(Name table, Tuple tuple) {
    this.table = requireNonNull(table);
    this.tuple = requireNonNull(tuple);
  }

  @Override
  public Set<Tuple> tuple() {
    return Collections.singleton(tuple);
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
}
