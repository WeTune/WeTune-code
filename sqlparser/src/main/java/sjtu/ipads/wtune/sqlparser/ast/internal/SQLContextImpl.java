package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.common.multiversion.MultiVersion;
import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.FieldManager;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.HashMap;
import java.util.Map;

public class SQLContextImpl implements SQLContext {
  private final String dbType;
  private final FieldManager attrMgr;

  private Schema schema;
  private Map<Class<?>, Object> mgrs;

  private Snapshot snapshot;
  private int versionNumber;

  private SQLContextImpl(String dbType) {
    this.dbType = dbType;
    this.attrMgr = FieldManager.empty();
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
    if (mgrClazz == FieldManager.class) return (M) attrMgr;
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
    ++versionNumber;
    snapshot = null;

    attrMgr.derive();

    if (mgrs != null)
      for (Object value : mgrs.values())
        if (value instanceof MultiVersion) ((MultiVersion) value).derive();
  }

  @Override
  public Snapshot snapshot() {
    if (snapshot != null && snapshot.versionNumber() == versionNumber) return snapshot;

    Snapshot snapshot = attrMgr.snapshot();

    if (mgrs != null)
      for (Object value : mgrs.values())
        if (value instanceof MultiVersion)
          snapshot = snapshot.merge(((MultiVersion) value).snapshot());

    snapshot.setVersionNumber(versionNumber);
    return this.snapshot = snapshot;
  }

  @Override
  public int versionNumber() {
    return versionNumber;
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    this.versionNumber = snapshot.versionNumber();
    this.snapshot = snapshot;

    attrMgr.setSnapshot(snapshot);

    if (mgrs != null)
      for (Object value : mgrs.values())
        if (value instanceof MultiVersion) ((MultiVersion) value).setSnapshot(snapshot);
  }
}
