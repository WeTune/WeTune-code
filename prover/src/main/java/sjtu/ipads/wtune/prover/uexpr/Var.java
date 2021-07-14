package sjtu.ipads.wtune.prover.uexpr;

import sjtu.ipads.wtune.common.utils.Showable;

/**
 * A tuple that is either a variable or a projection on another tuple, e.g. 't', 't.x', 't.x.y'.
 *
 * <p><b>Note: </b> This is a immutable value-based type.
 */
public interface Var extends Showable {
  Var[] base();

  Name name();

  /**
   * Substitute given tuple expression in this tuple.
   *
   * <ul>
   *   <b>Examples</b>
   *   <li>Tuple('t').replace('t', 's') => Tuple('s')
   *   <li>Tuple('t.x').replace('t', 's') => Tuple('s.x')
   *   <li>Tuple('t.x.y').replace('t.x', 's.z') => Tuple('s.z.y')
   * </ul>
   */
  Var subst(Var target, Var replacement);

  Var root();

  boolean uses(Var v);

  default boolean isBase() {
    return this instanceof BaseVar;
  }

  default boolean isProjected() {
    return this instanceof ProjectedVar;
  }

  default boolean isFunc() {
    return this instanceof FuncVar;
  }

  default boolean isConstant() {
    return this instanceof ConstVar;
  }

  default Var proj(String attribute) {
    return new ProjectedVar(this, new NameImpl(attribute));
  }

  static Var mkBase(String name) {
    return new BaseVar(new NameImpl(name));
  }

  static Var mkConstant(String expr) {
    return new ConstVar(new NameImpl(expr));
  }

  static Var mkFunc(String name, Var... args) {
    return new FuncVar(new NameImpl(name), args);
  }
}
