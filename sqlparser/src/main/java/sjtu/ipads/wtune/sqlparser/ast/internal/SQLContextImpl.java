package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;
import sjtu.ipads.wtune.sqlparser.ast.multiversion.MultiVersion;
import sjtu.ipads.wtune.sqlparser.ast.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.rel.Schema;

import java.util.HashMap;
import java.util.Map;

public class SQLContextImpl implements SQLContext {
  private final String dbType;
  private final AttributeManager attrMgr;

  private Schema schema;
  private Map<Class<?>, Object> mgrs;

  private SQLContextImpl(String dbType) {
    this.dbType = dbType;
    this.attrMgr = AttributeManager.empty();
  }

  public static SQLContext build(String dbType) {
    return new SQLContextImpl(dbType);
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
  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <M> M manager(Class<M> mgrClazz) {
    if (mgrClazz == AttributeManager.class) return (M) attrMgr;
    else if (mgrs == null) return null;
    else return (M) mgrs.get(mgrClazz);
  }

  @Override
  public <M> void addManager(Class<? super M> cls, M mgr) {
    if (mgrs == null) mgrs = new HashMap<>();
    mgrs.put(cls, mgr);
  }

  @Override
  public void derive() {
    attrMgr.derive();

    if (mgrs != null)
      for (Object value : mgrs.values())
        if (value instanceof MultiVersion) ((MultiVersion) value).derive();
  }

  @Override
  public Snapshot snapshot() {
    Snapshot snapshot = attrMgr.snapshot();

    if (mgrs != null)
      for (Object value : mgrs.values())
        if (value instanceof MultiVersion)
          snapshot = snapshot.merge(((MultiVersion) value).snapshot());

    return snapshot;
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    attrMgr.setSnapshot(snapshot);

    if (mgrs != null)
      for (Object value : mgrs.values())
        if (value instanceof MultiVersion) ((MultiVersion) value).setSnapshot(snapshot);
  }
}
