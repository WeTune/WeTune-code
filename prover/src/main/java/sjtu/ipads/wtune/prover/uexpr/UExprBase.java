package sjtu.ipads.wtune.prover.uexpr;

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
  public boolean uses(Var v) {
    for (UExpr child : children) if (child.uses(v)) return true;
    return false;
  }

  @Override
  public UExpr copy() {
    final UExprBase copy = copy0();
    final UExpr[] children = this.children;
    for (int i = 0; i < children.length; i++) copy.setChild(i, children[i].copy());
    return copy;
  }

  @Override
  public String toString() {
    return stringify(new StringBuilder()).toString();
  }

  protected abstract UExprBase copy0();
}
