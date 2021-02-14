package sjtu.ipads.wtune.common.multiversion;

import com.google.common.base.Equivalence;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class Catalog2DBase<R, C, V>
    extends MultiVersionBase<
        Table<Equivalence.Wrapper<R>, Equivalence.Wrapper<C>, Object>, Catalog2D<R, C, V>>
    implements Catalog2D<R, C, V> {
  private static final Object REMOVED = new Object();

  protected Catalog2DBase() {}

  protected Catalog2DBase(
      Table<Equivalence.Wrapper<R>, Equivalence.Wrapper<C>, Object> current,
      Catalog2D<R, C, V> prev) {
    super(current, prev);
  }

  @Override
  public boolean contains(R r, C c) {
    if (current == null) return fallbackContains(r, c);

    final Object t = current.get(Equivalence.identity().wrap(r), Equivalence.identity().wrap(c));
    if (t == REMOVED) return false;
    else if (t != null) return true;
    else if (prev == null) return fallbackContains(r, c);
    else return prev.contains(r, c);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(R r, C c) {
    if (current == null) return fallbackGet(r, c);

    final Object t = current.get(Equivalence.identity().wrap(r), Equivalence.identity().wrap(c));
    if (t == REMOVED) return null;
    else if (t != null) return (V) t;
    else if (prev == null) return fallbackGet(r, c);
    else return prev.get(r, c);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V put(R r, C c, V v) {
    if (current == null) return fallbackPut(r, c, v);

    final Object old =
        current.put(Equivalence.identity().wrap(r), Equivalence.identity().wrap(c), v);
    return old == REMOVED ? null : (V) old;
  }

  @Override
  @SuppressWarnings("unchecked")
  public V remove(R r, C c) {
    if (current == null) return fallbackRemove(r, c);

    final Object old =
        current.put(Equivalence.identity().wrap(r), Equivalence.identity().wrap(c), REMOVED);
    return old == REMOVED ? null : (V) old;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<C, V> row(R row) {
    final Map<C, V> map;

    if (prev == null) map = new IdentityHashMap<>(fallbackRow(row));
    else map = prev.row(row);

    if (current != null)
      for (var pair : current.row(Equivalence.identity().wrap(row)).entrySet()) {
        if (pair.getValue() == REMOVED) map.remove(pair.getKey().get());
        else map.put(pair.getKey().get(), (V) pair.getValue());
      }

    return map;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<R, V> column(C column) {
    final Map<R, V> map;

    if (prev == null) map = new IdentityHashMap<>(fallbackColumn(column));
    else map = prev.column(column);

    if (current != null)
      for (var pair : current.column(Equivalence.identity().wrap(column)).entrySet()) {
        if (pair.getValue() == REMOVED) map.remove(pair.getKey().get());
        else map.put(pair.getKey().get(), (V) pair.getValue());
      }

    return map;
  }

  @Override
  public boolean contains(Pair<R, C> pair) {
    return false;
  }

  protected boolean fallbackContains(R row, C column) {
    return false;
  }

  protected V fallbackGet(R row, C column) {
    return null;
  }

  protected V fallbackPut(R row, C column, V value) {
    return null;
  }

  protected V fallbackRemove(R row, C column) {
    return null;
  }

  protected Map<C, V> fallbackRow(R row) {
    return Collections.emptyMap();
  }

  protected Map<R, V> fallbackColumn(C column) {
    return Collections.emptyMap();
  }

  @Override
  protected Table<Equivalence.Wrapper<R>, Equivalence.Wrapper<C>, Object> makeCurrent() {
    return HashBasedTable.create();
  }

  @Override
  protected Catalog2D<R, C, V> makePrev(
      Table<Equivalence.Wrapper<R>, Equivalence.Wrapper<C>, Object> current,
      Catalog2D<R, C, V> prev) {
    return new Catalog2DBase<>(current, prev);
  }
}
