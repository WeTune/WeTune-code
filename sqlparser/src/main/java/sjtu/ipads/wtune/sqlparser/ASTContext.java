package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.internal.ASTContextImpl;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface ASTContext extends MultiVersion {
  System.Logger LOG = System.getLogger("wetune.sqlparser");

  String dbType();

  Schema schema();

  void setSchema(Schema schema);

  <M> M manager(Class<M> mgrClazz);

  <M> void addManager(Class<? super M> cls, M mgr);

  static ASTNode manage(String dbType, ASTNode root) {
    final ASTContext ctx = ASTContextImpl.build(dbType);
    root.accept(ASTVistor.topDownVisit(it -> it.setContext(ctx)));
    return root;
  }

  static ASTNode manage(ASTNode root, Schema schema) {
    manage(schema.dbType(), root);
    root.context().setSchema(schema);
    return root;
  }

  static ASTNode unmanage(ASTNode root) {
    if (root == null) return null;

    root.accept(ASTVistor.topDownVisit(it -> it.setContext(null)));
    return root;
  }
}
