package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast1.SqlNode;

import java.util.List;

public interface ParamDesc {
  int index();

  SqlNode node();

  List<ParamModifier> modifiers();

  void setIndex(int idx);
}
