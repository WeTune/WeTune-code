package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface Exporter extends Qualified {
  boolean deduplicated();

  List<String> attrNames();

  List<Expression> attrExprs();
}
