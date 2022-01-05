package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.schema.Column;

public interface Attribute {
  String name();

  Relation owner();

  SqlNode expr();

  Column column();
}
