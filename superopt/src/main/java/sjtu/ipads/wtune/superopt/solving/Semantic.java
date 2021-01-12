package sjtu.ipads.wtune.superopt.solving;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.*;
import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Scoped;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.*;

import static com.google.common.collect.Sets.powerSet;
import static com.google.common.collect.Sets.union;
import static java.util.Collections.singleton;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

public class Semantic extends BaseQueryBuilder implements GraphVisitor {
  private final Graph q;
  private final Placeholders placeholders;
  private final Sources sources;
  private final Deque<Relation> stack;
  private boolean produced;

  private Semantic(Graph q) {
    this.q = q;
    this.placeholders = new Placeholders();
    this.sources = new Sources();
    this.stack = new LinkedList<>();

    q.acceptVisitor(placeholders);
    q.acceptVisitor(sources);
  }

  public static Semantic build(Graph q) {
    return new Semantic(q);
  }

  private void push(Proposition condition, Value... output) {
    stack.push(new Relation(condition, output));
  }

  @Override
  protected void prepare() {
    stack.clear();
    produced = false;
    for (PickSym p : picks) setupSources(p);
  }

  @Override
  public void leaveInput(Input input) {
    push(ctx().makeTautology(), tuples[input.index()]);
  }

  @Override
  public void leaveInnerJoin(Join op) {
    final Relation right = stack.pop(), left = stack.pop();
    final PickSym leftPick = pickSym(op.leftFields()), rightPick = pickSym(op.rightFields());
    final Proposition joinCond = leftPick.apply(tuples).equalsTo(rightPick.apply(tuples));

    leftPick.setJoined(rightPick);

    push(
        left.condition().and(right.condition()).and(joinCond),
        arrayConcat(left.output(), right.output()));
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final Relation in = stack.pop();
    final Proposition cond =
        (Proposition) predSym(op.predicate()).apply(pickSym(op.fields()).apply(tuples));

    push(in.condition().and(cond), in.output());
  }

  @Override
  public void leaveProj(Proj op) {
    final Relation in = stack.pop();
    push(in.condition(), pickSym(op.fields()).apply(tuples));
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final Relation sub = stack.pop(), in = stack.pop();
    final Proposition cond = pickSym(op.fields()).apply(tuples).equalsTo(sub.output()[0]);
    // actually multiple-output subquery should be rule out earlier. See Heuristic::prune
    push(in.condition().and(cond), in.output());
  }

  @Override
  public void leaveUnion(Union op) {
    final Relation right = stack.pop(), left = stack.pop();
    push(left.condition().or(right.condition()), left.output());
  }

  @Override
  protected List<? extends Scoped> tablePlaceholders() {
    return placeholders.tbls;
  }

  @Override
  protected List<? extends Scoped> pickPlaceholders() {
    return placeholders.picks;
  }

  @Override
  protected List<? extends Scoped> predicatePlaceholders() {
    return placeholders.preds;
  }

  @Override
  public Value[] output() {
    if (!produced) {
      q.acceptVisitor(this);
      produced = true;
    }
    return stack.peek().output();
  }

  @Override
  public Proposition condition() {
    if (!produced) {
      q.acceptVisitor(this);
      produced = true;
    }
    return stack.peek().condition();
  }

  private void setupSources(PickSym p) {
    final Set<Set<Placeholder>> viable = sources.viable.get(p.unwrap(Placeholder.class));

    final TableSym[][] viableSources = new TableSym[viable.size()][];
    int i = 0;
    for (Set<Placeholder> placeholders : viable)
      viableSources[i++] = arrayMap(this::tableSym, TableSym.class, placeholders);

    p.setViableSources(viableSources);
    p.setVisibleSources(tables);
  }

  private static class Placeholders implements GraphVisitor {
    private List<Placeholder> tbls = new ArrayList<>(5);
    private List<Placeholder> picks = new ArrayList<>(8);
    private List<Placeholder> preds = new ArrayList<>(4);

    @Override
    public boolean enterInput(Input input) {
      tbls.add(input.table());
      return true;
    }

    @Override
    public boolean enterInnerJoin(InnerJoin op) {
      picks.add(op.leftFields());
      picks.add(op.rightFields());
      return true;
    }

    @Override
    public boolean enterPlainFilter(PlainFilter op) {
      picks.add(op.fields());
      preds.add(op.predicate());
      return true;
    }

    @Override
    public boolean enterProj(Proj op) {
      picks.add(op.fields());
      return true;
    }

    @Override
    public boolean enterSubqueryFilter(SubqueryFilter op) {
      picks.add(op.fields());
      return true;
    }
  }

  private static class Sources implements GraphVisitor {
    private final Map<Placeholder, Set<Set<Placeholder>>> viable = new HashMap<>(8);
    private final Deque<Set<Placeholder>> stack = new LinkedList<>();

    @Override
    public void leaveInput(Input input) {
      stack.push(singleton(input.table()));
    }

    @Override
    public void leaveInnerJoin(Join op) {
      final Set<Placeholder> rightTbls = stack.pop(), leftTbls = stack.pop();
      final Placeholder leftFields = op.leftFields(), rightFields = op.rightFields();
      viable.put(leftFields, collectionMap(Collections::singleton, leftTbls, HashSet::new));
      viable.put(rightFields, collectionMap(Collections::singleton, rightTbls, HashSet::new));
      stack.push(union(leftTbls, rightTbls));
    }

    @Override
    public void leavePlainFilter(PlainFilter op) {
      viable.put(op.fields(), powerSet(stack.peek()));
    }

    @Override
    public void leaveProj(Proj op) {
      viable.put(op.fields(), powerSet(stack.peek()));
    }

    @Override
    public void leaveSubqueryFilter(SubqueryFilter op) {
      viable.put(op.fields(), powerSet(stack.peek()));
    }

    @Override
    public void leaveUnion(Union op) {
      stack.pop(); // drop tables from right
    }
  }
}
