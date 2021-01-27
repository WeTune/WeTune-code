package sjtu.ipads.wtune.sqlparser.multiversion.internal;

import sjtu.ipads.wtune.sqlparser.multiversion.Snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SnapshotImpl implements Snapshot {
  private final List<Object> keys;
  private int versionNumber;

  private SnapshotImpl(List<Object> keys) {
    this.keys = keys;
  }

  public static Snapshot build() {
    return EmptySnapshot.INSTANCE;
  }

  public static Snapshot build(Object key) {
    return new SingletonSnapshot(key);
  }

  public static Snapshot build(List<Object> keys) {
    return new SnapshotImpl(keys);
  }

  @Override
  public int versionNumber() {
    return versionNumber;
  }

  @Override
  public void setVersionNumber(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  @Override
  public Snapshot merge(Snapshot snapshot) {
    keys.addAll(snapshot.keys());
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> keyClazz) {
    for (Object key : keys) if (keyClazz.isInstance(key)) return (T) key;
    return null;
  }

  @Override
  public Collection<Object> keys() {
    return keys;
  }
}

class SingletonSnapshot implements Snapshot {
  private final Object key;
  private int versionNumber;

  SingletonSnapshot(Object key) {
    this.key = key;
  }

  @Override
  public int versionNumber() {
    return versionNumber;
  }

  @Override
  public void setVersionNumber(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  @Override
  public Snapshot merge(Snapshot snapshot) {
    if (snapshot instanceof EmptySnapshot) return this;
    if (!(snapshot instanceof SingletonSnapshot)) return snapshot.merge(this);

    final List<Object> keys = new ArrayList<>(2);
    keys.add(key);
    keys.addAll(snapshot.keys());
    return SnapshotImpl.build(keys);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> keyClazz) {
    return keyClazz.isInstance(key) ? (T) key : null;
  }

  @Override
  public Collection<Object> keys() {
    return Collections.singletonList(key);
  }
}

class EmptySnapshot implements Snapshot {
  static Snapshot INSTANCE = new EmptySnapshot();

  @Override
  public int versionNumber() {
    return 0;
  }

  @Override
  public void setVersionNumber(int versionNumber) {}

  @Override
  public Snapshot merge(Snapshot snapshot) {
    return snapshot;
  }

  @Override
  public <T> T get(Class<T> keyClazz) {
    return null;
  }

  @Override
  public Collection<Object> keys() {
    return Collections.emptyList();
  }
}
