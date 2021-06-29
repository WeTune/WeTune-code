package sjtu.ipads.wtune.prover.expr;

/**
 * A tuple that is either a variable or a projection on another tuple, e.g. 't', 't.x', 't.x.y'.
 *
 * <p><b>Note: </b> This is a immutable value-based type.
 */
public interface Tuple {
  Tuple[] base();

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
  Tuple subst(Tuple target, Tuple replacement);

  Tuple root();

  boolean uses(Tuple v);

  default boolean isBase() {
    return this instanceof BaseTuple;
  }

  default boolean isProjected() {
    return this instanceof ProjectedTuple;
  }

  default boolean isFunc() {
    return this instanceof FuncTuple;
  }

  default boolean isConstant() {
    return this instanceof ConstTuple;
  }

  default Tuple proj(String attribute) {
    return new ProjectedTuple(this, new NameImpl(attribute));
  }

  static Tuple make(String name) {
    return new BaseTuple(new NameImpl(name));
  }

  static Tuple constant(String expr) {
    return new ConstTuple(new NameImpl(expr));
  }

  static Tuple func(String name, Tuple... args) {
    return new FuncTuple(new NameImpl(name), args);
  }
}
