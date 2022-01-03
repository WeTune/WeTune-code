package sjtu.ipads.wtune.sql.ast.internal;

import java.util.HashMap;
import java.util.Map;
import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sql.ASTContext;
import sjtu.ipads.wtune.sql.ast.AttributeManager;
import sjtu.ipads.wtune.sql.ast.FieldManager;
import sjtu.ipads.wtune.sql.schema.Schema;

public class ASTContextImpl implements ASTContext {
  private String dbType;
  private Schema schema;

  private FieldManager fieldMgr;
  private Map<Class<?>, Object> mgrs;

  private ASTContextImpl() {}

  public static ASTContext build() {
    return new ASTContextImpl();
  }

  @Override
  public String dbType() {
    return dbType;
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public void setDbType(String dbType) {
    this.dbType = dbType;
  }

  @Override
  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T manager(Class<T> mgrClazz) {
    if (mgrClazz == FieldManager.class) return (T) fieldMgr;
    else if (mgrs == null) return null;
    else return (T) mgrs.get(mgrClazz);
  }

  @Override
  public void addManager(AttributeManager<?> mgr) {
    if (mgrs == null) mgrs = new HashMap<>();
    mgrs.put(mgr.key(), mgr);
  }

  @Override
  public Snapshot derive() {
    final Snapshot snapshot = Snapshot.make();
    snapshot.put(FieldManager.class, fieldMgr);

    fieldMgr = FieldManager.make(fieldMgr);

    if (mgrs != null)
      for (Object value : mgrs.values()) {
        final AttributeManager<?> mgr = (AttributeManager<?>) value;
        snapshot.put(mgr.key(), mgr.derive());
      }

    return snapshot;
  }

  @Override
  public void rollback(Snapshot snapshot) {
    fieldMgr = (FieldManager) snapshot.get(FieldManager.class);

    if (mgrs == null) return;

    final Map<Class<?>, Object> currentMgrs = this.mgrs;
    this.mgrs = new HashMap<>();

    for (Class<?> key : snapshot.objs().keySet()) {
      final AttributeManager<?> mgr = (AttributeManager<?>) currentMgrs.get(key);
      mgr.rollback(snapshot);
      mgrs.put(key, mgr);
    }
  }
}
