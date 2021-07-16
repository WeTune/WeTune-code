package sjtu.ipads.wtune.prover.uexpr;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;
import java.util.Arrays;
import sjtu.ipads.wtune.prover.utils.Util;

final class FuncVar implements Var {
  private final Name funcName;
  private final Var[] args;

  FuncVar(Name funcName, Var[] args) {
    this.funcName = requireNonNull(funcName);
    this.args = requireNonNull(args);
  }

  public Var[] args() {
    return args;
  }

  @Override
  public Var[] base() {
    return args;
  }

  @Override
  public Name name() {
    return funcName;
  }

  @Override
  public Var subst(Var target, Var replacement) {
    requireNonNull(target);
    requireNonNull(replacement);

    if (this.equals(target)) return replacement;

    final Var[] subst = Util.substVar(args, target, replacement);

    if (subst == args) return this;
    else return new FuncVar(funcName, subst);
  }

  @Override
  public boolean uses(Var v) {
    for (Var arg : args) if (arg.uses(v)) return true;
    return false;
  }

  @Override
  public Var root() {
    return null;
  }

  @Override
  public String toString() {
    return stringify(new StringBuilder()).toString();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    return Util.interpolateVars(funcName.toString(), args, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FuncVar)) return false;
    final FuncVar that = (FuncVar) o;
    return Objects.equal(funcName, that.funcName) && Arrays.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    return funcName.hashCode() * 31 + Arrays.hashCode(args);
  }
}
