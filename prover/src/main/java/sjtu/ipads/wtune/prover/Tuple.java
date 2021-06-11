package sjtu.ipads.wtune.prover;

/**
 * A tuple that is either a variable or a projection on another tuple, e.g. 't', 't.x', 't.x.y'.
 *
 * <p><b>Note: </b> This is a immutable value-based type.
 */
public interface Tuple {
  /**
   * If the tuple is produced by projection on another tuple, then `base()` is that tuple, otherwise
   * `base()` is null (in which case, the tuple itself is a variable).
   */
  Tuple base();

  /**
   * `name()` represent the variable's name if base() == null, otherwise the attribute's name.
   *
   * <p>e.g., Tuple('t').name == 't'; Tuple('t.a').name == 'a';
   */
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
}
