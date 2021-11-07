package sjtu.ipads.wtune.superopt.optimizer;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.TreeScaffold;
import sjtu.ipads.wtune.sqlparser.plan.*;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.*;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.FuncUtils.setMap;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.EXISTS_FILTER;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INPUT;

class FilterReversedMatch implements ReversedMatch<FilterNode, Filter> {
  private List<Filter> ops;
  private FilterChain chain;
  private boolean isLeading, isTrailing;
  private ConstraintAwareModel whatIf;
  private FilterNode[] stack;
  private boolean[] used;
  private List<FilterNode> results;

  @Override
  public List<FilterNode> reverseMatch(FilterNode plan, Filter op, ConstraintAwareModel model) {
    assert isFilterHead(op);

    ops = linearizeOps(op);
    chain = FilterChain.mk(plan, true);
    isLeading = isLeadingChain(ops);
    isTrailing = isTrailingChain(ops);
    whatIf = model;

    if (ops.size() > chain.size()) return emptyList();
    if (isSimpleFullCover(op)) return singletonList(fullCover(chain)); // fast path

    List<FilterMatcher> matchers = mkMatchers();
    if (!validateMatchers(matchers)) return emptyList();

    stack = new FilterNode[ops.size()];
    used = new boolean[chain.size()];
    results = new ArrayList<>();

    head(matchers).match();

    return results;
  }

  @Override
  public List<FilterNode> results() {
    return null;
  }

  private static List<Filter> linearizeOps(Filter head) {
    final List<Filter> filters = new ArrayList<>();

    Filter path = head;
    while (true) {
      filters.add(path);
      if (path.predecessors()[0].kind().isFilter()) path = (Filter) path.predecessors()[0];
      else break;
    }

    return filters;
  }

  private static boolean isFilterHead(Filter op) {
    return op.successor() == null || !op.successor().kind().isFilter();
  }

  private static boolean isLeadingChain(List<Filter> ops) {
    return ops.get(0).successor() == null;
  }

  private static boolean isTrailingChain(List<Filter> ops) {
    return tail(ops).predecessors()[0].kind() == INPUT;
  }

  private static boolean isSimpleFullCover(Filter op) {
    return op.kind() == OperatorType.SIMPLE_FILTER
        && op.successor() != null
        && op.predecessors()[0].kind() != INPUT
        && !op.predecessors()[0].kind().isFilter();
  }

  private static FilterNode fullCover(FilterChain chain) {
    final PlanNode combination = CombinedFilterNode.mk(chain);
    final var scaffold = new TreeScaffold<>(treeRootOf(chain.successor()));
    final var rootTemplate = scaffold.rootTemplate();
    final var subTemplate = rootTemplate.bindJointPoint(chain.get(0), combination);
    subTemplate.bindJointPoint(combination, 0, chain.predecessor());
    scaffold.instantiate();
    return (FilterNode) subTemplate.getInstantiated();
  }

  private List<FilterMatcher> mkMatchers() {
    final boolean[] done = new boolean[ops.size()];
    final List<FilterMatcher> matchers = new ArrayList<>(ops.size() + 1);
    for (int i = 0; i < done.length; i++) {
      if (!done[i]) matchers.add(mkMatcher(i, done));
    }

    matchers.sort(Comparator.comparingInt(FilterMatcher::priority));
    matchers.add(new Terminator());
    for (int i = 1; i < matchers.size(); i++) matchers.get(i - 1).setNext(matchers.get(i));

    return matchers;
  }

  private FilterMatcher mkMatcher(int index, boolean[] done) {
    final Filter op = ops.get(index);
    if (op.kind() == EXISTS_FILTER) {
      done[index] = true;
      return new ExistsFilterMatcher(index);
    }

    final Symbol attrsSym = ((AttrsFilter) op).attrs();
    final Constraints constraints = whatIf.constraints();
    final Symbols symbols = op.fragment().symbols();
    final TIntList buddies = new TIntArrayList(done.length);
    boolean isOrphan = true;

    for (Symbol eqSym : constraints.eqClassOf(attrsSym)) {
      if (eqSym.ctx() != attrsSym.ctx()) continue;

      final Op owner = symbols.ownerOf(eqSym);
      if (eqSym != attrsSym) isOrphan = false;
      if (!owner.kind().isFilter()) continue;

      final int buddyIndex = ops.indexOf(owner);
      if (buddyIndex >= 0) {
        done[buddyIndex] = true;
        buddies.add(buddyIndex);
      }
    }

    if (whatIf.isInterpreted(attrsSym)) return new GroupedFilterMatcher(buddies.toArray(), 0);
    if (isOrphan && !op.kind().isSubquery()) return new FreeFilterMatcher(index, !isTrailing);
    else return new GroupedFilterMatcher(buddies.toArray(), op.kind().isSubquery() ? 2 : 1);
  }

  private boolean validateMatchers(List<FilterMatcher> matchers) {
    boolean isGreedyPresent = false;
    for (FilterMatcher matcher : matchers) {
      if (matcher instanceof FreeFilterMatcher && ((FreeFilterMatcher) matcher).greedy)
        if (isGreedyPresent) return false;
        else isGreedyPresent = true;
    }

    return true;
  }

  private interface FilterMatcher {
    int priority();

    void setNext(FilterMatcher m);

    void match();
  }

  private abstract static class FilterMatcherBase implements FilterMatcher {
    protected FilterMatcher next;

    @Override
    public void setNext(FilterMatcher next) {
      this.next = next;
    }
  }

  private class GroupedFilterMatcher extends FilterMatcherBase {
    private final int[] indices;
    private final int priority;

    private GroupedFilterMatcher(int[] indices, int priority) {
      this.indices = indices;
      this.priority = priority;
    }

    @Override
    public void match() {
      match0(0, 0);
    }

    private void match0(int ordinal, int nodeIndexStart) {
      if (ordinal >= indices.length) {
        next.match();
        return;
      }
      if (nodeIndexStart >= chain.size()) return;

      whatIf = whatIf.derive();

      final int opIndex = indices[ordinal];
      final Filter op = ops.get(opIndex);

      for (int i = nodeIndexStart, bound = chain.size(); i < bound; i++) {
        if (used[i]) continue;

        final FilterNode f = chain.get(i);
        if (op.match(f, whatIf) && whatIf.checkConstraint(false)) {
          used[i] = true;
          stack[opIndex] = f;

          match0(ordinal + 1, i + 1);

          used[i] = false;
          stack[opIndex] = null;
        }

        whatIf.reset();
      }

      whatIf = whatIf.base();
    }

    public int priority() {
      return priority;
    }
  }

  private class FreeFilterMatcher extends FilterMatcherBase {
    private final int opIndex;
    private final Filter op;
    private final boolean greedy;

    private FreeFilterMatcher(int index, boolean greedy) {
      this.opIndex = index;
      this.op = ops.get(index);
      this.greedy = greedy;
    }

    @Override
    public void match() {
      if (greedy) {
        final List<FilterNode> filters = new ArrayList<>(chain.size());
        final boolean[] oldUsed = Arrays.copyOf(used, used.length);

        for (int i = 0; i < used.length; ++i)
          if (!used[i]) {
            filters.add(chain.get(i));
            used[i] = true;
          }

        stack[opIndex] = mkCombined(filters);

        next.match();

        used = oldUsed;
        stack[opIndex] = null;

      } else {
        whatIf = whatIf.derive();

        final PlanContext ctx = chain.predecessor().context();
        final Multimap<Set<PlanNode>, Integer> unused =
            MultimapBuilder.hashKeys().arrayListValues().build();
        for (int i = 0; i < used.length; ++i)
          if (!used[i]) {
            final FilterNode filter = chain.get(i);
            final Set<PlanNode> sources = setMap(filter.refs(), it -> ctx.ownerOf(ctx.deRef(it)));
            unused.put(sources, i);
          }

        for (Collection<Integer> group : unused.asMap().values()) {
          final List<FilterNode> groupedFilters = new ArrayList<>(group.size());
          for (Integer index : group) {
            used[index] = true;
            groupedFilters.add(chain.get(index));
          }

          stack[opIndex] = mkCombined(groupedFilters);

          next.match();

          for (Integer index : group) used[index] = false;
          stack[opIndex] = null;

          whatIf.reset();
        }

        whatIf = whatIf.base();
      }
    }

    public int priority() {
      return Integer.MAX_VALUE;
    }
  }

  private class ExistsFilterMatcher extends FilterMatcherBase {
    private final int opIndex;

    private ExistsFilterMatcher(int index) {
      this.opIndex = index;
    }

    @Override
    public int priority() {
      return Integer.MAX_VALUE;
    }

    @Override
    public void match() {
      for (int i = 0; i < chain.size(); i++) {
        if (used[i]) continue;
        final FilterNode f = chain.get(i);
        if (f.kind() == EXISTS_FILTER) {
          used[i] = true;
          stack[opIndex] = f;
          next.match();
          stack[opIndex] = null;
          used[i] = false;
        }
      }
    }
  }

  private class Terminator implements FilterMatcher {
    @Override
    public int priority() {
      return Integer.MAX_VALUE;
    }

    @Override
    public void setNext(FilterMatcher m) {}

    @Override
    public void match() {
      if (!isLeading && !isTrailing) for (boolean u : used) if (!u) return;

      final List<FilterNode> newFilters = new ArrayList<>(chain.size());

      if (isTrailing) newFilters.addAll(asList(stack));
      for (int i = 0; i < used.length; i++) if (!used[i]) newFilters.add(chain.get(i));
      final int matchPointIndex = isTrailing ? 0 : newFilters.size();
      if (!isTrailing) newFilters.addAll(asList(stack));

      final FilterNode newChain =
          FilterChain.mk(chain.successor(), chain.predecessor(), newFilters).buildChain();
      results.add((FilterNode) filterAt(newChain, matchPointIndex));
    }

    private PlanNode filterAt(PlanNode head, int index) {
      PlanNode node = head;
      while (index > 0) {
        node = node.predecessors()[0];
        --index;
      }
      return node;
    }
  }

  private static FilterNode mkCombined(List<FilterNode> filters) {
    return filters.size() == 1 ? filters.get(0) : CombinedFilterNode.mk(filters);
  }
}
