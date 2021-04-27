package sjtu.ipads.wtune.stmt.resolver;

import java.util.Deque;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;

class ParamDescImpl implements ParamDesc {
  private int index;

  private final ASTNode expr, node;
  private final Deque<ParamModifier> modifiers;

  ParamDescImpl(ASTNode expr, ASTNode node, Deque<ParamModifier> modifiers) {
    this.expr = expr;
    this.node = node;
    this.modifiers = modifiers;
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public ASTNode node() {
    return node;
  }

  @Override
  public ASTNode expr() {
    return expr;
  }

  @Override
  public Deque<ParamModifier> modifiers() {
    return modifiers;
  }

  public boolean isCheckNull() {
    final Type lastModifierType = modifiers.getLast().type();
    return lastModifierType == Type.CHECK_NULL || lastModifierType == Type.CHECK_NULL_NOT;
  }

  @Override
  public String toString() {
    return "{%d,%s}".formatted(index, expr == null ? "param" : expr.toString());
  }
}
