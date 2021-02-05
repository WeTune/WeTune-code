package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast.constants.JoinType;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.*;

public interface TableSourceFields {
  //// Simple
  FieldKey<ASTNode> SIMPLE_TABLE = SIMPLE_SOURCE.nodeAttr("table");
  FieldKey<List<String>> SIMPLE_PARTITIONS = SIMPLE_SOURCE.attr("partitions", List.class);
  FieldKey<String> SIMPLE_ALIAS = SIMPLE_SOURCE.strAttr("alias");
  // mysql only
  FieldKey<List<ASTNode>> SIMPLE_HINTS = SIMPLE_SOURCE.nodesAttr("hints");
  //// Joined
  FieldKey<ASTNode> JOINED_LEFT = JOINED_SOURCE.nodeAttr("left");
  FieldKey<ASTNode> JOINED_RIGHT = JOINED_SOURCE.nodeAttr("right");
  FieldKey<JoinType> JOINED_TYPE = JOINED_SOURCE.attr("type", JoinType.class);
  FieldKey<ASTNode> JOINED_ON = JOINED_SOURCE.nodeAttr("on");
  FieldKey<List<String>> JOINED_USING = JOINED_SOURCE.attr("using", List.class);
  //// Derived
  FieldKey<ASTNode> DERIVED_SUBQUERY = DERIVED_SOURCE.nodeAttr("subquery");
  FieldKey<String> DERIVED_ALIAS = DERIVED_SOURCE.strAttr("alias");
  FieldKey<Boolean> DERIVED_LATERAL = DERIVED_SOURCE.boolAttr("lateral");
  FieldKey<List<String>> DERIVED_INTERNAL_REFS = DERIVED_SOURCE.attr("internalRefs", List.class);

  static String tableSourceName(ASTNode node) {
    if (!TABLE_SOURCE.isInstance(node)) return null;

    if (SIMPLE_SOURCE.isInstance(node))
      return coalesce(node.get(SIMPLE_ALIAS), node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE));
    else if (DERIVED_SOURCE.isInstance(node)) return node.get(DERIVED_ALIAS);

    return null;
  }

  static String tableNameOf(ASTNode node) {
    return !SIMPLE_SOURCE.isInstance(node) ? null : node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE);
  }
}
