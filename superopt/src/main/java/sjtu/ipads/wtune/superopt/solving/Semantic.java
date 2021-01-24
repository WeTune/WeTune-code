package sjtu.ipads.wtune.superopt.solving;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.*;
import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.Commons.*;

public class Semantic extends BaseQueryBuilder implements GraphVisitor {
  private final Graph q;
  private final Deque<Relation> stack;

  private Semantic(Graph q) {
    this.q = q;
    this.stack = new LinkedList<>();
  }

  public static Semantic build(Graph q) {
    return new Semantic(q);
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

  @Override
  public void leaveInput(Input input) {
    final TableSym t = tableSym(input.table());
    final Value tuple = newTuple();

    stack.push(
        new Relation(asArray(t), (Proposition) t.apply(tuple), asArray(tuple), asArray(tuple)));
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    final Relation inR = stack.pop(), inL = stack.pop();
    final PickSym pickL = pickSym(op.leftFields()), pickR = pickSym(op.rightFields());
    final TableSym[] visibleL = inL.visibleSources(), visibleR = inR.visibleSources();

    setJoinKeySource(pickL, visibleL, pickR, visibleR);

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
    final TableSym[] visibleL = inL.visibleSources(), visibleR = inR.visibleSources();

    setJoinKeySource(pickL, visibleL, pickR, visibleR);

    final TableSym[] visible = arrayConcat(visibleL, visibleR);
    final Proposition joinCond = pickL.apply(inL.out).equalsTo(pickR.apply(inR.out));
    final Value v = newTuple();
    final Proposition nonNullCond =
        inR.cond.and(joinCond).and(v.equalsTo(ctx().makeCombine(inL.out)));
    final Proposition nullCond =
        inR.cond.and(joinCond).not().and(v.equalsTo(ctx().makeNullTuple()));
    final Proposition cond = inL.cond.and(nonNullCond.or(nullCond));
    final Value[] out = arrayConcat(inL.out, v);
    final Value[] bound = arrayConcat(inL.bound, v);

    stack.push(new Relation(visible, cond, out, bound));
  }

  private void setJoinKeySource(
      PickSym pickL, TableSym[] visibleL, PickSym pickR, TableSym[] visibleR) {
    pickL.setVisibleSources(visibleL);
    pickR.setVisibleSources(visibleR);
    pickL.setViableSources(makeViableSources(visibleL, true));
    pickR.setViableSources(makeViableSources(visibleR, true));
    pickL.setJoined(pickR);
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final Relation in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final PredicateSym pred = predSym(op.predicate());
    final TableSym[] visible = in.visibleSources();

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Proposition cond = in.cond.and((Proposition) pred.apply(pick.apply(in.out)));
    stack.push(new Relation(visible, cond, in.out, in.bound));
  }

  @Override
  public void leaveProj(Proj op) {
    final Relation in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final TableSym[] visible = in.visibleSources();

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Value[] out = asArray(pick.apply(in.out));

    stack.push(new Relation(visible, in.cond, out, in.bound));
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final Relation sub = stack.pop(), in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final TableSym[] visible = in.visibleSources();

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

  @Override
  protected Function<Value, Proposition> semantic() {
    stack.clear();
    q.acceptVisitor(this);

    final Relation rel = stack.peek();
    return x -> ctx().makeExists(rel.bound, rel.cond.and(x.equalsTo(ctx().makeCombine(rel.out))));
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

    public TableSym[] visibleSources() {
      return visibleSources;
    }
  }
}
