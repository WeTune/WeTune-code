package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttr.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.*;

public interface TableSourceAttr {
  //// Simple
  AttrKey<SQLNode> SIMPLE_TABLE = SIMPLE_SOURCE.nodeAttr("table");
  AttrKey<List<String>> SIMPLE_PARTITIONS = SIMPLE_SOURCE.attr("partitions", List.class);
  AttrKey<String> SIMPLE_ALIAS = SIMPLE_SOURCE.strAttr("alias");
  // mysql only
  AttrKey<List<SQLNode>> SIMPLE_HINTS = SIMPLE_SOURCE.nodesAttr("hints");
  //// Joined
  AttrKey<SQLNode> JOINED_LEFT = JOINED.nodeAttr("left");
  AttrKey<SQLNode> JOINED_RIGHT = JOINED.nodeAttr("right");
  AttrKey<JoinType> JOINED_TYPE = JOINED.attr("type", JoinType.class);
  AttrKey<SQLNode> JOINED_ON = JOINED.nodeAttr("on");
  AttrKey<List<String>> JOINED_USING = JOINED.attr("using", List.class);
  //// Derived
  AttrKey<SQLNode> DERIVED_SUBQUERY = DERIVED_SOURCE.nodeAttr("subquery");
  AttrKey<String> DERIVED_ALIAS = DERIVED_SOURCE.strAttr("alias");
  AttrKey<Boolean> DERIVED_LATERAL = DERIVED_SOURCE.boolAttr("lateral");
  AttrKey<List<String>> DERIVED_INTERNAL_REFS = DERIVED_SOURCE.attr("internalRefs", List.class);

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
