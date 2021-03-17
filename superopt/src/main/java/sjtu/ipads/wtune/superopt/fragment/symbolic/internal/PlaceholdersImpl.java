package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholders;

public class PlaceholdersImpl implements Placeholders {
  private ListMultimap<Operator, Placeholder> tables;
  private ListMultimap<Operator, Placeholder> picks;
  private ListMultimap<Operator, Placeholder> predicates;

  public PlaceholdersImpl() {}

  private static Placeholder get(
      ListMultimap<Operator, Placeholder> map, Operator node, String tag, int ordinal) {
    final List<Placeholder> placeholders = map.get(node);
    while (placeholders.size() <= ordinal)
      placeholders.add(new PlaceholderImpl(node, tag, placeholders.size()));

    return placeholders.get(ordinal);
  }

  @Override
  public Placeholder getPick(Operator node, int ordinal) {
    if (picks == null)
      picks = MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
    return get(picks, node, "c", ordinal);
  }

  @Override
  public Placeholder getPredicate(Operator node, int ordinal) {
    if (predicates == null)
      predicates = MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
    return get(predicates, node, "p", ordinal);
  }

  @Override
  public Placeholder getTable(Operator node, int ordinal) {
    if (tables == null)
      tables = MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
    return get(tables, node, "t", ordinal);
  }

  @Override
  public Collection<Placeholder> tables() {
    return tables == null ? Collections.emptyList() : tables.values();
  }

  @Override
  public Collection<Placeholder> picks() {
    return picks == null ? Collections.emptyList() : picks.values();
  }

  @Override
  public Collection<Placeholder> predicates() {
    return predicates == null ? Collections.emptyList() : predicates.values();
  }

  @Override
  public void remove(Operator op) {
    if (tables != null) tables.removeAll(op);
    if (predicates != null) predicates.removeAll(op);
    if (picks != null) picks.removeAll(op);
  }

  @Override
  public boolean contains(Placeholder placeholder) {
    return tables.containsKey(placeholder.owner())
        || picks.containsKey(placeholder.owner())
        || predicates.containsKey(placeholder.owner());
  }

  @Override
  public int count() {
    return tables.size() + picks.size() + predicates.size();
  }
}
