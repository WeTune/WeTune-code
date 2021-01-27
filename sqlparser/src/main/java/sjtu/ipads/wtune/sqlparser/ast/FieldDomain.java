package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;

import java.util.List;

public interface FieldDomain {
  String name();

  boolean isInstance(SQLNode node);

  <T, R extends T> FieldKey<R> attr(String name, Class<T> clazz);

  default FieldKey<String> strAttr(String name) {
    return attr(name, String.class);
  }

  default FieldKey<Boolean> boolAttr(String name) {
    return attr(name, Boolean.class);
  }

  default FieldKey<SQLNode> nodeAttr(String name) {
    return attr(name, SQLNode.class);
  }

  default FieldKey<List<SQLNode>> nodesAttr(String name) {
    return attr(name, List.class);
  }
}
