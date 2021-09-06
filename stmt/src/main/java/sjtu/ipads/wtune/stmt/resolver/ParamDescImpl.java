package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type;

import java.io.Serializable;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;

class ParamDescImpl implements ParamDesc, Serializable {
  private int index;

  private final transient ASTNode node;
  private final transient List<ParamModifier> modifiers;
  private final String exprString;

  ParamDescImpl(ASTNode expr, ASTNode node, List<ParamModifier> modifiers) {
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
  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  public boolean isCheckNull() {
    final Type lastModifierType = tail(modifiers).type();
    return lastModifierType == Type.CHECK_NULL || lastModifierType == Type.CHECK_NULL_NOT;
  }

  @Override
  public boolean isElement() {
    return any(modifiers, it -> it.type() == Type.ARRAY_ELEMENT || it.type() == Type.TUPLE_ELEMENT);
  }

  @Override
  public String toString() {
    return "{%d,%s}".formatted(index, exprString);
  }
}
