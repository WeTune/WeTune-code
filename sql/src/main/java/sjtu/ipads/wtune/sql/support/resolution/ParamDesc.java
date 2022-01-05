package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast.SqlNode;

import java.util.List;

public interface ParamDesc {
  SqlNode node();

  List<ParamModifier> modifiers();

  int index();

  void setIndex(int idx);
}
