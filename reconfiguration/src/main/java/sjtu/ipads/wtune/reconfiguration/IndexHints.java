package sjtu.ipads.wtune.reconfiguration;

import com.google.common.collect.Sets;
import com.google.common.graph.Graph;
import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.sqlparser.SQLNode;
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
    final Graph<Column> graph = matching.matching();

    if (fast.isEmpty() || slow.isEmpty() || matching.matchingCount() == 0) return null;

    final List<Constraint> fastIndexes = indexesOnRelatedTable(fast);
    if (isEmpty(fastIndexes)) return null;

    final Set<List<Column>> candidates = suggestByMatch(fastIndexes, graph);
    if (isEmpty(candidates)) return null;

    return new IndexHints(candidates);
  }

  private static List<Constraint> indexesOnRelatedTable(Set<Column> fast) {
    return fast.stream()
        .map(Column::table)
        .distinct()
        .map(IndexHints::indexesOn)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static List<Constraint> indexesOn(Table table) {
    final Set<Constraint> constraints = table.constraints();
    final List<Constraint> indexes = new ArrayList<>(constraints.size());
    for (Constraint constraint : constraints)
      if (constraint.type() != SQLNode.ConstraintType.NOT_NULL
          && constraint.type() != SQLNode.ConstraintType.CHECK) indexes.add(constraint);
    return indexes;
  }

  private static Set<List<Column>> suggestByMatch(List<Constraint> base, Graph<Column> match) {
    return base.stream()
        .map(fastIndex -> suggestByMatch(fastIndex, match))
        .reduce(Sets::union)
        .orElse(null);
  }

  private static Set<List<Column>> suggestByMatch(Constraint base, Graph<Column> matching) {
    final List<Set<Column>> matches =
        base.columns().stream()
            .map(matching::adjacentNodes)
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

      while (!advanceLength() && advanceCandidate())
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
