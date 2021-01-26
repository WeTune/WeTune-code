package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.AttrKey;

import java.util.List;

public interface AttrDomain {
  String name();

  boolean isInstance(SQLNode node);

  <T, R extends T> AttrKey<R> attr(String name, Class<T> clazz);

  default AttrKey<String> strAttr(String name) {
    return attr(name, String.class);
  }

  default AttrKey<Boolean> boolAttr(String name) {
    return attr(name, Boolean.class);
  }

  default AttrKey<SQLNode> nodeAttr(String name) {
    return attr(name, SQLNode.class);
  }

  default AttrKey<List<SQLNode>> nodesAttr(String name) {
    return attr(name, List.class);
  }
}
