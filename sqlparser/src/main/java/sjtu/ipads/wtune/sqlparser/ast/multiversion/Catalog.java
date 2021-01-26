package sjtu.ipads.wtune.sqlparser.ast.multiversion;

public interface Catalog<K, V> extends MultiVersion {
  /*
   Spec:
   1. put(k,null) throws IllegalArgumentException
   2. !contains(k) <=> get(k) == null
   3. After remove(k) and before put(k,v) (v != null),
      any get(k) returns null, any contains(k) returns false

   Impl Note:
   To conform to the spec,
   1. `remove(k)` actually put a null value  into `current()` to mark an absent key (i.e. soft removal).
   2. `get(k)` explicit checks the containment via `current().contains(k)` to distinguish between a
       soft removal and true absence
   3. `put(k,v)` prohibits `v` from being null.
   4. `contains(k)` determines the containment by examining whether the mapped value is null.
  */

  boolean contains(K k);

  V get(K k);

  V put(K k, V v);

  V remove(K k);
}
