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

  private void push(Proposition condition, Value[] output, int offset, Value[] tuples) {
    stack.push(new Relation(condition, output, offset, tuples));
  }

  private Value[] mergeTuples(Value[] tuples0, Value[] tuples1) {
    if (tuples0 == tuples) return tuples1;
    if (tuples1 == tuples) return tuples0;

    assert tuples0.length == tuples1.length && tuples0.length == tuples.length;

    final Value[] newTuples = Arrays.copyOf(tuples, tuples.length);
    for (int i = 0, bound = newTuples.length; i < bound; i++) {
      if (newTuples[i] != tuples0[i]) newTuples[i] = tuples0[i];
      else if (newTuples[i] != tuples1[i]) newTuples[i] = tuples1[i];
    }

    return newTuples;
  }

  @Override
  protected void prepare() {
    stack.clear();
    produced = false;
    for (PickSym p : picks) setupSources(p);
  }

  @Override
  public void leaveInput(Input input) {
    push(ctx().makeTautology(), asArray(tuples[input.index()]), input.index(), tuples);
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    final Relation right = stack.pop(), left = stack.pop();
    final Value[] tuples = mergeTuples(left.tuples(), right.tuples());
    final PickSym leftPick = pickSym(op.leftFields()), rightPick = pickSym(op.rightFields());
    final Proposition joinCond = leftPick.apply(tuples).equalsTo(rightPick.apply(tuples));

    leftPick.setJoined(rightPick);

    push(
        left.condition().and(right.condition()).and(joinCond),
        arrayConcat(left.output(), right.output()),
        left.offset(),
        tuples);
  }

  @Override
  public void leaveLeftJoin(LeftJoin op) {
    final Relation right = stack.pop(), left = stack.pop();
    final Value[] tuples = mergeTuples(left.tuples(), right.tuples());
    final PickSym leftPick = pickSym(op.leftFields()), rightPick = pickSym(op.rightFields());
    final Proposition joinCond = leftPick.apply(tuples).equalsTo(rightPick.apply(tuples));
    final Proposition nonNullCond = right.condition().and(joinCond);
    final Value nullTuple = ctx().makeNullTuple();
    final Value[] rightTuples =
        arrayMap(it -> ctx().makeIte(nonNullCond, it, nullTuple), Value.class, right.output());

    leftPick.setJoined(rightPick);

    final Value[] modTuples = tuples == this.tuples ? Arrays.copyOf(tuples, tuples.length) : tuples;
    System.arraycopy(rightTuples, 0, modTuples, right.offset(), rightTuples.length);

    push(left.condition(), arrayConcat(left.output(), rightTuples), left.offset(), modTuples);
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final Relation in = stack.pop();
    final Value[] tuples = in.tuples();
    final Proposition cond =
        (Proposition) predSym(op.predicate()).apply(pickSym(op.fields()).apply(tuples));

    push(in.condition().and(cond), in.output(), in.offset(), tuples);
  }

  @Override
  public void leaveProj(Proj op) {
    final Relation in = stack.pop();
    final Value[] tuples = in.tuples();
    push(in.condition(), asArray(pickSym(op.fields()).apply(tuples)), in.offset(), tuples);
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final Relation sub = stack.pop(), in = stack.pop();
    final Proposition cond = pickSym(op.fields()).apply(in.tuples()).equalsTo(sub.output()[0]);
    // actually multiple-output subquery should be rule out earlier. See Heuristic::prune
    push(in.condition().and(cond), in.output(), in.offset(), in.tuples());
  }

  @Override
  public void leaveUnion(Union op) {
    final Relation right = stack.pop(), left = stack.pop();
    push(left.condition().or(right.condition()), left.output(), left.offset(), left.tuples());
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
    public boolean enterLeftJoin(LeftJoin op) {
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
    public void leaveInnerJoin(InnerJoin op) {
      setJoinKeySource(op);
    }

    @Override
    public void leaveLeftJoin(LeftJoin op) {
      setJoinKeySource(op);
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

    private void setJoinKeySource(Join op) {
      final Placeholder leftFields = op.leftFields(), rightFields = op.rightFields();
      final Set<Placeholder> rightTbls = stack.pop(), leftTbls = stack.pop();
      viable.put(leftFields, collectionMap(Collections::singleton, leftTbls, HashSet::new));
      viable.put(rightFields, collectionMap(Collections::singleton, rightTbls, HashSet::new));
      stack.push(union(leftTbls, rightTbls));
    }
  }
}
