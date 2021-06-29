package sjtu.ipads.wtune.prover.expr;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;
import java.util.Arrays;
import sjtu.ipads.wtune.prover.utils.Util;

final class FuncTuple implements Tuple {
  private final Name funcName;
  private final Tuple[] args;

  FuncTuple(Name funcName, Tuple[] args) {
    this.funcName = requireNonNull(funcName);
    this.args = requireNonNull(args);
  }

  public Tuple[] args() {
    return args;
  }

  @Override
  public Tuple[] base() {
    return args;
  }

  @Override
  public Name name() {
    return funcName;
  }

  @Override
  public Tuple subst(Tuple target, Tuple replacement) {
    requireNonNull(target);
    requireNonNull(replacement);

    if (this.equals(target)) return replacement;

    final Tuple[] subst = Util.subst(args, target, replacement);

    if (subst == args) return this;
    else return new FuncTuple(funcName, subst);
  }

  @Override
  public boolean uses(Tuple v) {
    for (Tuple arg : args) if (arg.uses(v)) return true;
    return false;
  }

  @Override
  public Tuple root() {
    return null;
  }

  @Override
  public String toString() {
    return Util.interpolateToString(funcName.toString(), args);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FuncTuple)) return false;
    final FuncTuple that = (FuncTuple) o;
    return Objects.equal(funcName, that.funcName) && Arrays.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    return funcName.hashCode() * 31 + Arrays.hashCode(args);
  }
}
