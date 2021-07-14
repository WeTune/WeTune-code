package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

final class TableTermImpl extends UExprBase implements TableTerm {
  private final Name table;
  private Var var;

  TableTermImpl(Name table, Var var) {
    this.table = requireNonNull(table);
    this.var = requireNonNull(var);
  }

  @Override
  public void subst(Var v1, Var v2) {
    requireNonNull(v1);
    requireNonNull(v2);
    var = var.subst(v1, v2);
  }

  @Override
  public boolean uses(Var v) {
    return var.uses(v);
  }

  @Override
  public Var tuple() {
    return var;
  }

  @Override
  public Name name() {
    return table;
  }

  @Override
  public UExprBase copy0() {
    return new TableTermImpl(table, var);
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    builder.append(table).append('(');
    var.stringify(builder);
    return builder.append(')');
  }
}
