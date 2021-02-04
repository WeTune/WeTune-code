package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Param {
  private final ASTNode node;
  private final int index;
  private final List<ParamModifier> modifiers;

  public Param(ASTNode node, int index) {
    this.index = index;
    this.node = node;
    this.modifiers = null;
  }

  public Param(ASTNode node, int index, List<ParamModifier> modifiers) {
    this.index = index;
    this.node = node;
    this.modifiers = modifiers;
  }

  public int index() {
    return index;
  }

  public ASTNode node() {
    return node;
  }

  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  @Override
  public String toString() {
    return String.join(", ", listMap(ParamModifier::toString, modifiers));
  }
}
