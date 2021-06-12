package sjtu.ipads.wtune.prover.expr;

import java.util.List;
import java.util.Objects;

abstract class UExprBase implements UExpr {
  protected UExpr parent;
  protected final UExpr[] children;

  UExprBase() {
    parent = null;
    children = new UExpr[kind().numChildren];
  }

  @Override
  public UExpr parent() {
    return parent;
  }

  @Override
  public UExpr child(int i) {
    return children[i];
  }

  @Override
  public List<UExpr> children() {
    return List.of(children);
  }

  @Override
  public void setParent(UExpr parent) {
    if (this.parent == null) this.parent = parent;
    else throw new IllegalStateException("changing parent is disallowed");
  }

  @Override
  public void setChild(int i, UExpr child) {
    Objects.checkIndex(i, children.length);
    child.setParent(this);
    children[i] = child;
  }

  @Override
  public UExpr copy() {
    final UExprBase copy = copy0();
    System.arraycopy(children, 0, copy.children, 0, children.length);
    return copy;
  }

  protected abstract UExprBase copy0();
}
