package sjtu.ipads.wtune.prover.normalform;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.FuncUtils.any;
import static sjtu.ipads.wtune.prover.decision.SimpleDecisionProcedure.decideSame;
import static sjtu.ipads.wtune.prover.utils.Util.comparePred;
import static sjtu.ipads.wtune.prover.utils.Util.compareTable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import sjtu.ipads.wtune.prover.expr.PredTerm;
import sjtu.ipads.wtune.prover.expr.TableTerm;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.PropositionMemo.Proposition;

class TautologyInference {
  private final List<Tuple> fixedVars;
  private final List<Object> terms;
  private final List<Proposition> props;
  private PropositionMemo memo;

  TautologyInference(List<Tuple> fixedVars) {
    this(new ArrayList<>(), new ArrayList<>(), new PropositionMemo(), fixedVars);
  }

  private TautologyInference(
      List<Object> terms, List<Proposition> props, PropositionMemo memo, List<Tuple> fixedVars) {
    this.terms = terms;
    this.props = props;
    this.memo = memo;
    this.fixedVars = fixedVars;
  }

  boolean checkTautology() {
    return memo.checkTautology();
  }

  void add(Conjunction conjunction) {
    for (List<Proposition> props : toProps(conjunction)) {
      memo = memo.add(props); // don't forget memo is immutable
    }
  }

  TautologyInference copy() {
    return new TautologyInference(new ArrayList<>(terms), new ArrayList<>(props), memo, fixedVars);
  }

  private List<List<Proposition>> toProps(Conjunction conjunction) {
    if (conjunction.isEmpty()) return singletonList(singletonList(memo.makeTrue()));

    final List<Proposition> props = new ArrayList<>();

    final List<Tuple> vars = new ArrayList<>(conjunction.vars());
    vars.removeAll(fixedVars);

    final List<UExpr> boundedTables = new ArrayList<>(conjunction.tables().size());
    for (UExpr table : conjunction.tables())
      if (any(vars, table::uses)) boundedTables.add(table);
      else props.add(toProp(table));

    final List<UExpr> boundedPreds = new ArrayList<>(conjunction.preds().size());
    for (UExpr pred : conjunction.preds())
      if (any(vars, pred::uses)) boundedPreds.add(pred);
      else props.add(toProp(pred));

    // not(T1 + T2 + ...) becomes not(T1) * not(T2) ...
    // We assume that, if T1 is not a Sum, then T1 contains no multiplicity.
    // Since that normalization will always break down such case
    final Disjunction boundedNeg;
    if (conjunction.neg() != null) {
      final List<Conjunction> boundedC = new ArrayList<>(1);
      for (Conjunction c : conjunction.neg()) {
        if (any(vars, c::uses)) boundedC.add(c);
        else props.add(toProp(c).not());
      }
      boundedNeg = boundedC.isEmpty() ? null : Disjunction.make(boundedC);
    } else {
      boundedNeg = null;
    }

    final Function<Disjunction, Conjunction> conjunctionMaker =
        squash -> Conjunction.make(vars, boundedTables, boundedPreds, boundedNeg, squash);

    // |T1 + T2 + ...| becomes T1 + T2 + ..., then distributed with previous ones
    if (conjunction.squash() != null) {
      final Disjunction squash = conjunction.squash();
      final List<List<Proposition>> ret = new ArrayList<>(squash.conjunctions().size());

      for (Conjunction c : squash) {
        final List<Proposition> copy = new ArrayList<>(props);
        if (any(vars, c::uses)) {
          copy.add(toProp(conjunctionMaker.apply(Disjunction.make(singletonList(c)))));
        } else {
          copy.add(toProp(c));
          copy.add(toProp(conjunctionMaker.apply(null)));
        }
        ret.add(copy);
      }
      return ret;

    } else {
      if (!boundedTables.isEmpty() || !boundedPreds.isEmpty() || boundedNeg != null)
        props.add(toProp(conjunctionMaker.apply(null)));

      return singletonList(props);
    }
  }

  private Proposition toProp(Object term) {
    if (term instanceof TableTerm) {
      for (int i = 0, bound = terms.size(); i < bound; i++) {
        final Object o = terms.get(i);
        if (o instanceof TableTerm && compareTable((UExpr) o, (UExpr) term)) {
          return props.get(i);
        }
      }
      return makeProp(term);
    }

    if (term instanceof PredTerm) {
      for (int i = 0, bound = terms.size(); i < bound; i++) {
        final Object o = terms.get(i);
        if (o instanceof PredTerm && comparePred((UExpr) o, (UExpr) term)) {
          return props.get(i);
        }
      }
      return makeProp(term);
    }

    if (term instanceof Conjunction) {
      final Conjunction c = (Conjunction) term;

      final UExpr unwrapped = tryUnwrap(c);
      if (unwrapped != null) return toProp(unwrapped);

      assert !c.vars().isEmpty();
      for (int i = 0, bound = terms.size(); i < bound; i++) {
        final Object o = terms.get(i);
        if (o instanceof Conjunction && decideSame((Conjunction) o, c)) {
          return props.get(i);
        }
      }
      return makeProp(term);
    }

    return assertFalse();
  }

  private static UExpr tryUnwrap(Conjunction c) {
    if (!c.vars().isEmpty()) return null;

    int flag = 0;
    flag |= Math.min(c.tables().size(), 2);
    flag |= Math.min(c.preds().size(), 2) << 2;
    flag |= (c.neg() != null ? 1 : 0) << 4;
    flag |= (c.squash() != null ? 1 : 0) << 6;

    switch (flag) {
      case 1:
        return c.tables().get(0);
      case (1 << 2):
        return c.preds().get(0);
      case (1 << 4):
        return UExpr.not(c.neg().toExpr());
      case (1 << 6):
        return c.squash().toExpr();
      default:
        return null;
    }
  }

  private Proposition makeProp(Object term) {
    final Proposition prop = memo.makeTerm();
    props.add(prop);
    terms.add(term);
    return prop;
  }
}
