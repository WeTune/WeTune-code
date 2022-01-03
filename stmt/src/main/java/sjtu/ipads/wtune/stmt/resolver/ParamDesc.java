package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sql.ast.ASTNode;

import java.util.List;

public interface ParamDesc {
  int index();

  void setIndex(int i);

  ASTNode node();

  boolean isCheckNull();

  boolean isElement();

  List<ParamModifier> modifiers();
}
