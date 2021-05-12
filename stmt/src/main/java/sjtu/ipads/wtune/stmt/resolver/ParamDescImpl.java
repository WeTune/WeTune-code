package sjtu.ipads.wtune.stmt.resolver;

import static sjtu.ipads.wtune.common.utils.FuncUtils.any;

import java.io.Serializable;
import java.util.Deque;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;

class ParamDescImpl implements ParamDesc, Serializable {
  private int index;

  private final transient ASTNode node;
  private final transient Deque<ParamModifier> modifiers;
  private final String exprString;

  ParamDescImpl(ASTNode expr, ASTNode node, Deque<ParamModifier> modifiers) {
    this.node = node;
    this.modifiers = modifiers;
    this.exprString = expr == null ? "param" : expr.toString();
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
  public Deque<ParamModifier> modifiers() {
    return modifiers;
  }

  public boolean isCheckNull() {
    final Type lastModifierType = modifiers.getLast().type();
    return lastModifierType == Type.CHECK_NULL || lastModifierType == Type.CHECK_NULL_NOT;
  }

  @Override
  public boolean isElement() {
    return any(it -> it.type() == Type.ARRAY_ELEMENT || it.type() == Type.TUPLE_ELEMENT, modifiers);
  }

  @Override
  public String toString() {
    return "{%d,%s}".formatted(index, exprString);
  }
}
