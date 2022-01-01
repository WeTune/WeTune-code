package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;

public interface Exporter extends Qualified {
  boolean deduplicated();

  List<String> attrNames();

  List<Expression> attrExprs();
}
