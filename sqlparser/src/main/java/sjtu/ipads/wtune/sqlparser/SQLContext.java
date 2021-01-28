package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.internal.SQLContextImpl;
import sjtu.ipads.wtune.sqlparser.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public interface SQLContext extends MultiVersion {
  System.Logger LOG = System.getLogger("wetune.sqlparser");

  String dbType();

  Schema schema();

  void setSchema(Schema schema);

  <M> M manager(Class<M> mgrClazz);

  <M> void addManager(Class<? super M> cls, M mgr);

  static SQLNode manage(String dbType, SQLNode root) {
    final SQLContext ctx = SQLContextImpl.build(dbType);
    root.accept(SQLVisitor.topDownVisit(it -> it.setContext(ctx)));
    return root;
  }
}
