package sjtu.ipads.wtune.superopt.plan.internal;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import sjtu.ipads.wtune.superopt.plan.Placeholder;
import sjtu.ipads.wtune.superopt.plan.Placeholders;
import sjtu.ipads.wtune.superopt.plan.PlanNode;

import java.util.Collection;
import java.util.List;

public class PlaceholdersImpl implements Placeholders {
  private ListMultimap<PlanNode, Placeholder> tables;
  private ListMultimap<PlanNode, Placeholder> picks;
  private ListMultimap<PlanNode, Placeholder> predicates;

  PlaceholdersImpl() {}

  private static Placeholder get(
      ListMultimap<PlanNode, Placeholder> map, PlanNode node, String tag, int ordinal) {
    final List<Placeholder> placeholders = map.get(node);
    while (placeholders.size() <= ordinal)
      placeholders.add(new PlaceholderImpl(node, tag, placeholders.size()));

    return placeholders.get(ordinal);
  }

  @Override
  public Placeholder getPick(PlanNode node, int ordinal) {
    if (picks == null)
      picks = MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
    return get(picks, node, "c", ordinal);
  }

  @Override
  public Placeholder getPredicate(PlanNode node, int ordinal) {
    if (predicates == null)
      predicates = MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
    return get(predicates, node, "p", ordinal);
  }

  @Override
  public Placeholder getTable(PlanNode node, int ordinal) {
    if (tables == null)
      tables = MultimapBuilder.ListMultimapBuilder.linkedHashKeys(4).arrayListValues(1).build();
    return get(tables, node, "t", ordinal);
  }

  @Override
  public Collection<Placeholder> tables() {
    return tables.values();
  }

  @Override
  public Collection<Placeholder> picks() {
    return picks.values();
  }

  @Override
  public Collection<Placeholder> predicates() {
    return predicates.values();
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
