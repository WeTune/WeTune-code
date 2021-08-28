package sjtu.ipads.wtune.sqlparser.plan;

public interface Ref {
  /*
   * Note: the qualification and name of a `Ref` instance is merely
   * what appears in the original query.
   * Once the ref-binding is done, the correct approach stringifying a ref is
   * `context.deRef(ref).toString()`
   */

  String intrinsicQualification();

  String intrinsicName();
}
