package sjtu.ipads.wtune.solver.core.impl;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.solver.core.Match;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class MatchImpl<K, V> implements Match<K, V> {
  private final MatchImpl<K, V> parent;
  private final Map<K, V> matches = new HashMap<>();
  private final LinkedList<Match<K, V>> derivations = new LinkedList<>();

  protected MatchImpl(MatchImpl<K, V> parent) {
    this.parent = parent;
  }

  public static <K, V> Match<K, V> create() {
    return new MatchImpl<>(null);
  }

  @Override
  public boolean match(K k, V v) {
    requireNonNull(k);
    requireNonNull(v);

    if (derivations.isEmpty()) {
      if (isConflict(k, v)) return false;
      matches.put(k, v);
      return true;
    }

    derivations.removeIf(m -> !m.match(k, v));
    return !derivations.isEmpty();
  }

  @Override
  public V getMatch(K k) {
    V v = matches.get(k);
    if (v != null) return v;
    for (Match<K, V> derivation : derivations) if ((v = derivation.getMatch(k)) != null) return v;
    return null;
  }

  @Override
  public Match<K, V> derive() {
    final Match<K, V> derived = create0();
    derivations.add(derived);
    return derived;
  }

  @Override
  public void unDerive() {
    derivations.pop();
  }

  @Override
  public void compact() {
    derivations.forEach(Match::compact);
    if (derivations.size() == 1) {
      matches.putAll(derivations.get(0).matches());
      derivations.clear();
    }
  }

  @Override
  public List<Map<K, V>> flatten() {
    compact();
    final List<Map<K, V>> ret =
        derivations.stream()
            .map(Match::flatten)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    if (ret.isEmpty()) return Collections.singletonList(matches);
    ret.forEach(it -> it.putAll(matches));
    return ret;
  }

  @Override
  public Map<K, V> matches() {
    return matches;
  }

  @Override
  public Set<K> keys() {
    final Set<K> subKeys =
        derivations.stream().map(Match::keys).reduce(Sets::union).orElse(Collections.emptySet());

    return Sets.union(subKeys, matches.keySet());
  }

  private boolean isConflict(K k, V v) {
    if (parent != null && parent.isConflict(k, v)) return true;
    final V existing = matches.get(k);
    return existing != null && !v.equals(existing);
  }

  protected Match<K, V> create0() {
    return new MatchImpl<>(this);
  }

  @Override
  public String toString() {
    return matches.toString();
  }
}
