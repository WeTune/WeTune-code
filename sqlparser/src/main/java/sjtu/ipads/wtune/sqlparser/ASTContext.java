package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;
import sjtu.ipads.wtune.sqlparser.ast.internal.ASTContextImpl;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.lang.System.Logger.Level;

public interface ASTContext extends MultiVersion {
  System.Logger LOG = System.getLogger("wetune.sql");

  String dbType();

  Schema schema();

  void setDbType(String dbType);

  void setSchema(Schema schema);

  <M> M manager(Class<M> mgrClazz);

  void addManager(AttributeManager<?> mgr);

  static ASTContext build() {
    return ASTContextImpl.build();
  }

  static ASTNode manage(ASTNode root, Schema schema) {
    if (root.parent() != null)
      ASTContext.LOG.log(Level.WARNING, "set context of non-root node will be override");

    final ASTContext context = ASTContext.build();
    context.setSchema(schema);
    context.setDbType(schema.dbType());

    root.setContext(context);
    return root;
  }

  static ASTNode manage(ASTNode root, ASTContext context) {
    root.setContext(context);
    return root;
  }
}
