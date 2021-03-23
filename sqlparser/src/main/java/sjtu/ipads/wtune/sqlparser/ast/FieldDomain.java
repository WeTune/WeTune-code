package sjtu.ipads.wtune.sqlparser.ast;

import java.util.List;
import sjtu.ipads.wtune.common.attrs.FieldKey;

public interface FieldDomain {
  String name();

  boolean isInstance(ASTNode node);

  List<FieldKey> fields();

  <T, R extends T> FieldKey<R> attr(String name, Class<T> clazz);

  default FieldKey<String> strAttr(String name) {
    return attr(name, String.class);
  }

  default FieldKey<Boolean> boolAttr(String name) {
    return attr(name, Boolean.class);
  }

  default FieldKey<ASTNode> nodeAttr(String name) {
    return attr(name, ASTNode.class);
  }

  default FieldKey<List<ASTNode>> nodesAttr(String name) {
    return attr(name, List.class);
  }
}
