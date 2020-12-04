package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.MatchImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Match<K, V> {
  boolean match(K i0, V i1);

  Match<K, V> derive();

  void unDerive();

  void compact();

  V getMatch(K k);

  List<Map<K, V>> flatten();

  Map<K, V> matches();

  Set<K> keys();

  static <K, V> Match<K, V> create() {
    return MatchImpl.create();
  }
}
