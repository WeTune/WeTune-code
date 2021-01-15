package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.*;

public interface TableSourceAttrs {
  String TABLE_SOURCE_ATTR_PREFIX = NodeAttrs.SQL_ATTR_PREFIX + "tableSource.";

  //// Simple
  Attrs.Key<SQLNode> SIMPLE_TABLE = SIMPLE_SOURCE.nodeAttr("table");
  Attrs.Key<List<String>> SIMPLE_PARTITIONS = SIMPLE_SOURCE.attr2("partitions", List.class);
  Attrs.Key<String> SIMPLE_ALIAS = SIMPLE_SOURCE.strAttr("alias");
  // mysql only
  Attrs.Key<List<SQLNode>> SIMPLE_HINTS = SIMPLE_SOURCE.nodesAttr("hints");
  //// Joined
  Attrs.Key<SQLNode> JOINED_LEFT = JOINED.nodeAttr("left");
  Attrs.Key<SQLNode> JOINED_RIGHT = JOINED.nodeAttr("right");
  Attrs.Key<JoinType> JOINED_TYPE = JOINED.attr("type", JoinType.class);
  Attrs.Key<SQLNode> JOINED_ON = JOINED.nodeAttr("on");
  Attrs.Key<List<String>> JOINED_USING = JOINED.attr2("using", List.class);
  //// Derived
  Attrs.Key<SQLNode> DERIVED_SUBQUERY = DERIVED_SOURCE.nodeAttr("subquery");
  Attrs.Key<String> DERIVED_ALIAS = DERIVED_SOURCE.strAttr("alias");
  Attrs.Key<Boolean> DERIVED_LATERAL = DERIVED_SOURCE.boolAttr("lateral");
  Attrs.Key<List<String>> DERIVED_INTERNAL_REFS = DERIVED_SOURCE.attr2("internalRefs", List.class);

  static String tableSourceName(SQLNode node) {
    if (!TABLE_SOURCE.isInstance(node)) return null;

    if (SIMPLE_SOURCE.isInstance(node))
      return coalesce(node.get(SIMPLE_ALIAS), node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE));
    else if (DERIVED_SOURCE.isInstance(node)) return node.get(DERIVED_ALIAS);

    return null;
  }

  static String tableNameOf(SQLNode node) {
    return !SIMPLE_SOURCE.isInstance(node) ? null : node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE);
  }
}
