package sjtu.ipads.wtune.reconfiguration;

import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.schema.Constraint;
import sjtu.ipads.wtune.stmt.schema.Table;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.isEmpty;

public class IndexHints implements Iterable<List<Column>> {
  private final Set<List<Column>> candidates;

  public IndexHints(Set<List<Column>> candidates) {
    this.candidates = candidates;
  }

  public static IndexHints hint(ColumnMatching matching) {
    final Set<Column> fast = matching.fast();
    final Set<Column> slow = matching.slow();

    if (fast.isEmpty() || slow.isEmpty() || matching.matchingCount() == 0) return null;

    final List<Constraint> fastIndexes = indexesOnRelatedTable(fast);
    if (isEmpty(fastIndexes)) return null;

    final Set<List<Column>> candidates = suggestByMatch(fastIndexes, matching);
    if (isEmpty(candidates)) return null;

    return new IndexHints(candidates);
  }

  private static List<Constraint> indexesOnRelatedTable(Set<Column> fast) {
    return fast.stream()
        .map(Column::table)
        .distinct()
        .map(Table::constraints)
        .flatMap(Collection::stream)
        .filter(Predicate.not(Constraint::fromPatch))
        .filter(Constraint::isIndex)
        .collect(Collectors.toList());
  }

  private static Set<List<Column>> suggestByMatch(List<Constraint> base, ColumnMatching match) {
    return base.stream()
        .map(fastIndex -> suggestByMatch(fastIndex, match))
        .reduce(Sets::union)
        .orElse(null);
  }

  private static Set<List<Column>> suggestByMatch(Constraint base, ColumnMatching matching) {
    final List<Set<Column>> matches =
        base.columns().stream()
            .map(matching::matchOf)
            .takeWhile(Predicate.not(Commons::isEmpty))
            .collect(Collectors.toList());
    return Sets.cartesianProduct(matches);
  }

  @Override
  public Iterator<List<Column>> iterator() {
    return new HintIter(candidates.iterator());
  }

  private static class HintIter implements Iterator<List<Column>> {
    private final Iterator<List<Column>> candidatesIter;
    private List<Column> currentCandidate;
    private List<Column> currentHint;
    private int currentLength;

    private HintIter(Iterator<List<Column>> candidatesIter) {
      this.candidatesIter = candidatesIter;
      this.currentLength = -1;
      advance();
    }

    private void advance() {
      assert currentLength != 0;
      while (!advanceLength() && (currentLength != 0 || advanceCandidate()))
        ;
    }

    private boolean advanceLength() {
      if (currentCandidate == null || currentLength >= currentCandidate.size()) {
        currentLength = 0;
        return false;
      }

      ++currentLength;
      currentHint = currentCandidate.subList(0, currentLength);
      return Index.validate(currentHint);
    }

    private boolean advanceCandidate() {
      if (candidatesIter.hasNext()) {
        currentCandidate = candidatesIter.next();
        return true;
      }
      return false;
    }

    @Override
    public boolean hasNext() {
      return currentLength != 0;
    }

    @Override
    public List<Column> next() {
      if (currentLength == 0) throw new NoSuchElementException();

      final List<Column> ret = this.currentHint;
      advance();
      return ret;
    }
  }
}
