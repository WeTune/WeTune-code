package sjtu.ipads.wtune.superopt.fragment;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.Commons.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.dumb;

public class Semantic extends BaseQueryBuilder implements OperatorVisitor {
  private final Fragment q;
  private final Deque<Relation> stack;

  private final BiMap<Placeholder, TableSym> tables;
  private final BiMap<Placeholder, PickSym> picks;
  private final BiMap<Placeholder, PredicateSym> predicates;

  private Semantic(Fragment q) {
    this.q = q;
    this.stack = new LinkedList<>();

    this.tables = HashBiMap.create(4);
    this.picks = HashBiMap.create(8);
    this.predicates = HashBiMap.create(4);
  }

  public static Semantic build(Fragment q) {
    return new Semantic(q);
  }

  @Override
  public void leaveInput(Input input) {
    final TableSym t = tableSym(input.table());
    final Value tuple = makeTuple();

    stack.push(
        new Relation(asArray(t), (Proposition) t.apply(tuple), asArray(tuple), asArray(tuple)));
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    final Relation inR = stack.pop(), inL = stack.pop();
    final PickSym pickL = pickSym(op.leftFields()), pickR = pickSym(op.rightFields());
    final TableSym[] visibleL = inL.visibleSources, visibleR = inR.visibleSources;

    setSourceForJoinKey(pickL, visibleL, pickR, visibleR);

    final TableSym[] visible = arrayConcat(visibleL, visibleR);
    final Proposition joinCond = pickL.apply(inL.out).equalsTo(pickR.apply(inR.out));
    final Proposition cond = inL.cond.and(inR.cond).and(joinCond);
    final Value[] out = arrayConcat(inL.out, inR.out);
    final Value[] bound = arrayConcat(inL.bound, inR.bound);

    stack.push(new Relation(visible, cond, out, bound));
  }

  @Override
  public void leaveLeftJoin(LeftJoin op) {
    final Relation inR = stack.pop(), inL = stack.pop();
    final PickSym pickL = pickSym(op.leftFields()), pickR = pickSym(op.rightFields());
    final TableSym[] visibleL = inL.visibleSources, visibleR = inR.visibleSources;

    setSourceForJoinKey(pickL, visibleL, pickR, visibleR);

    final TableSym[] visible = arrayConcat(visibleL, visibleR);
    final Proposition joinCond = inR.cond.and(pickL.apply(inL.out).equalsTo(pickR.apply(inR.out)));
    final Value v = makeTuple();
    final Proposition nonNullCond =
        ctx().makeExists(inR.bound, joinCond.and(v.equalsTo(ctx().makeCombine(inR.out))));
    final Proposition nullCond =
        ctx().makeExists(inR.bound, joinCond).not().and(v.equalsTo(ctx().makeNullTuple()));
    final Proposition cond = inL.cond.and(nonNullCond.or(nullCond));
    final Value[] out = arrayConcat(inL.out, v);
    final Value[] bound = arrayConcat(inL.bound, v);

    stack.push(new Relation(visible, cond, out, bound));
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final Relation in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final PredicateSym pred = predicateSym(op.predicate());
    final TableSym[] visible = in.visibleSources;

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Proposition cond = in.cond.and((Proposition) pred.apply(pick.apply(in.out)));
    stack.push(new Relation(visible, cond, in.out, in.bound));
  }

  @Override
  public void leaveProj(Proj op) {
    final Relation in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final TableSym[] visible = in.visibleSources;

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    Operator prev = op.predecessors()[0];
    while (prev.type().numPredecessors() == 1) {
      if (prev instanceof Proj) {
        pick.setUpstream(pickSym(((Proj) prev).fields()));
        break;
      }
      prev = prev.predecessors()[0];
    }

    final Value[] out = asArray(pick.apply(in.out));

    stack.push(new Relation(visible, in.cond, out, in.bound));
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final Relation sub = stack.pop(), in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final TableSym[] visible = in.visibleSources;

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Proposition joinCond = pick.apply(in.out).equalsTo(ctx().makeCombine(sub.out));
    final Proposition cond = in.cond.and(sub.cond).and(joinCond);

    stack.push(new Relation(visible, cond, in.out, arrayConcat(in.bound, sub.bound)));
  }

  @Override
  public void leaveUnion(Union op) {
    final Relation right = stack.pop(), left = stack.pop();
    final TableSym[] visible = left.visibleSources;

    final Proposition cond = left.cond.or(right.cond);
    stack.push(new Relation(visible, cond, left.out, arrayConcat(left.bound, right.bound)));
  }

  public Placeholder lookup(Sym sym) {
    if (sym instanceof TableSym) return tables.inverse().get(sym);
    if (sym instanceof PickSym) return picks.inverse().get(sym);
    if (sym instanceof PredicateSym) return predicates.inverse().get(sym);
    return null;
  }

  @Override
  protected Function<Value, Proposition> semantic() {
    stack.clear();
    q.acceptVisitor(this);

    final Relation rel = stack.peek();
    assert !stack.isEmpty();
    final LogicCtx ctx = ctx(); // don't inline this variable: we don't want this-ref to be captured
    return x -> ctx.makeExists(rel.bound, rel.cond.and(x.equalsTo(ctx.makeCombine(rel.out))));
  }

  private TableSym tableSym(Placeholder placeholder) {
    return tables.computeIfAbsent(placeholder, dumb(this::makeTable));
  }

  private PickSym pickSym(Placeholder placeholder) {
    return picks.computeIfAbsent(placeholder, dumb(this::makePick));
  }

  private PredicateSym predicateSym(Placeholder placeholder) {
    return predicates.computeIfAbsent(placeholder, dumb(this::makePredicate));
  }

  private TableSym[][] makeViableSources(TableSym[] visible, boolean isJoinKey) {
    if (isJoinKey) {
      final TableSym[][] viable = new TableSym[visible.length][1];
      for (int i = 0; i < visible.length; i++) viable[i][0] = visible[i];
      return viable;
    } else {
      final int count = 1 << visible.length;
      final TableSym[][] viable = new TableSym[count - 1][];
      for (int i = count - 1; i > 0; --i) viable[i - 1] = maskArray(visible, i);
      return viable;
    }
  }

  private void setSourceForJoinKey(
      PickSym pickL, TableSym[] visibleL, PickSym pickR, TableSym[] visibleR) {
    pickL.setVisibleSources(visibleL);
    pickR.setVisibleSources(visibleR);
    pickL.setViableSources(makeViableSources(visibleL, true));
    pickR.setViableSources(makeViableSources(visibleR, true));
    pickL.setJoined(pickR);
  }

  private static final class Relation {
    private final TableSym[] visibleSources;
    private final Proposition cond;
    private final Value[] out;
    private final Value[] bound;

    private Relation(TableSym[] visibleSources, Proposition cond, Value[] out, Value[] bound) {
      this.visibleSources = visibleSources;
      this.cond = cond;
      this.out = out;
      this.bound = bound;
    }
  }
}
