package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.util.List;

import static sjtu.ipads.wtune.common.attrs.Attrs.Key.checkAgainst;

public interface AttrDomain {
  String name();

  boolean isInstance(SQLNode node);

  String attrPrefix();

  default <T> Attrs.Key<T> attr(String name, Class<T> clazz) {
    final Attrs.Key<T> attr = Attrs.key(attrPrefix() + name().toLowerCase() + "." + name, clazz);
    attr.setCheck(checkAgainst(SQLNode.class, this::isInstance));
    return attr;
  }

  default <T> Attrs.Key<T> attr2(String name, Class<?> clazz) {
    final Attrs.Key<T> attr =
        Attrs.key2(attrPrefix() + name().toLowerCase() + "." + name, clazz);
    attr.setCheck(checkAgainst(SQLNode.class, this::isInstance));
    return attr;
  }

  default Attrs.Key<String> strAttr(String name) {
    return attr(name, String.class);
  }

  default Attrs.Key<Boolean> boolAttr(String name) {
    return attr(name, Boolean.class);
  }

  default Attrs.Key<SQLNode> nodeAttr(String name) {
    return attr(name, SQLNode.class);
  }

  default Attrs.Key<List<SQLNode>> nodesAttr(String name) {
    return attr2(name, List.class);
  }
}
