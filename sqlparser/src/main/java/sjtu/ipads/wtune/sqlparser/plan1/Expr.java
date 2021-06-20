package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

/**
 * Represents an arbitrary scalar expression.
 *
 * <p>1. {@link #refs()} are the columns used in the expression.<br>
 * {@link #template()} is the AST with all the refs replaced by placeholders.
 *
 * <p><b>Example</b>
 *
 * <p>users.id = 1<br>
 * &nbsp;=> refs: [user.id], template: ?.? = 1
 *
 * <p>users.salary + 1000<br>
 * &nbsp;=> refs: [users.salary], template: ?.? + 1000
 *
 * <p>2. {@link #interpolate(ValueBag)} interpolate the placeholder with the values.
 *
 * <p><b>Example</b><br>
 * Expr(user.id = 1).interpolate([user_role.user_id])<br>
 * &nbsp;=> Expr(user_role.user_id = 1)
 *
 * <p>3. Two {@link Expr}s are equal if their templates are identical.
 */
public interface Expr {
  RefBag refs();

  ASTNode template();

  ASTNode interpolate(ValueBag values);

  void setRefs(RefBag refs);

  default void setRefs(List<Ref> refs) {
    setRefs(new RefBagImpl(refs));
  }
}
