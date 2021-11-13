package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast1.constants.JoinKind;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.TableSource;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.TableName_Table;
import static sjtu.ipads.wtune.sqlparser.ast1.TableSourceKind.*;

public interface TableSourceFields {
  //// Simple
  FieldKey<SqlNode> Simple_Table = SimpleSource.nodeField("Table");
  FieldKey<List<String>> Simple_Partition = SimpleSource.field("Partitions", List.class);
  FieldKey<String> Simple_Alias = SimpleSource.textField("Alias");
  // mysql only
  FieldKey<SqlNodes> Simple_Hints = SimpleSource.nodesField("Hints");
  //// Joined
  FieldKey<SqlNode> Joined_Left = JoinedSource.nodeField("Left");
  FieldKey<SqlNode> Joined_Right = JoinedSource.nodeField("Right");
  FieldKey<JoinKind> Joined_Kind = JoinedSource.field("Kind", JoinKind.class);
  FieldKey<SqlNode> Joined_On = JoinedSource.nodeField("On");
  FieldKey<List<String>> Joined_Using = JoinedSource.field("Using", List.class);
  //// Derived
  FieldKey<SqlNode> Derived_Subquery = DerivedSource.nodeField("Subquery");
  FieldKey<String> Derived_Alias = DerivedSource.textField("Alias");
  FieldKey<Boolean> Derived_Lateral = DerivedSource.boolField("Lateral");
  FieldKey<List<String>> Derived_InternalRefs = DerivedSource.field("InternalRefs", List.class);

  static String tableSourceNameOf(SqlNode node) {
    if (!TableSource.isInstance(node)) return null;

    if (SimpleSource.isInstance(node))
      return coalesce(node.$(Simple_Alias), node.$(Simple_Table).$(TableName_Table));
    else if (DerivedSource.isInstance(node)) return node.$(Derived_Alias);

    return null;
  }

  static String tableNameOf(SqlNode node) {
    return !SimpleSource.isInstance(node) ? null : node.$(Simple_Table).$(TableName_Table);
  }
}
