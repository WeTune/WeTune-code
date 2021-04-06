package sjtu.ipads.wtune.sqlparser.ast;

import java.util.Map;
import sjtu.ipads.wtune.common.multiversion.MultiVersion;

public interface AttributeManager<T> extends MultiVersion {
  T get(ASTNode owner);

  T set(ASTNode owner, T value);

  T unset(ASTNode owner);

  Map<ASTNode, T> attributes();

  Class<?> key();
}
