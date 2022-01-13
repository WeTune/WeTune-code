package sjtu.ipads.wtune.superopt.substitution;

import me.tongfei.progressbar.ProgressBar;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.common.utils.SetSupport;
import sjtu.ipads.wtune.sql.plan.*;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Symbol;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static sjtu.ipads.wtune.common.utils.IterableSupport.zip;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyTree;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.ATTRS;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.PRED;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

class ReduceRuleBank {
  private final SubstitutionBank bank;

  ReduceRuleBank(SubstitutionBank bank) {
    this.bank = bank;
  }

  SubstitutionBank reduce() {
    OptimizerSupport.setOptimizerTweaks(OptimizerSupport.TWEAK_DISABLE_JOIN_FLIP);

    bank.removeIf(ReduceRuleBank::isUselessRule1);
    bank.removeIf(ReduceRuleBank::isUselessRule2);

    try (final ProgressBar pb = new ProgressBar("Reduce", bank.size())) {
      final List<Substitution> rules = new ArrayList<>(bank.rules());
      for (int i = 0, bound = rules.size(); i < bound; i++) {
        pb.step();

        final Substitution rule = rules.get(i);
        try {
          if (isImpliedRule(rule)) bank.remove(rule);
          else bank.add(rule);

        } catch (Throwable ex) {
          System.err.println(i + " " + rule);
          throw ex;
        }
      }
    }

    return bank;
  }

  private static boolean isUselessRule1(Substitution rule) {
    // All LHS attrs symbols are required equal.
    final Constraints constraints = rule.constraints();
    final List<Symbol> attrs = rule._0().symbols().symbolsOf(ATTRS);
    if (attrs.size() <= 5) return false;

    for (int i = 0, bound = attrs.size() - 1; i <= bound; i++) {
      for (int j = i + 1; j < bound; j++)
        if (!constraints.isEq(attrs.get(i), attrs.get(j))) return false;
    }
    return true;
  }

  private static boolean isUselessRule2(Substitution rule) {
    // Two RHS Pred symbols share the same instantiation.
    final Constraints constraints = rule.constraints();
    final List<Symbol> preds = rule._1().symbols().symbolsOf(PRED);
    final Set<Symbol> instantiations = SetSupport.map(preds, constraints::instantiationOf);
    return instantiations.size() < preds.size();
  }

  private boolean isImpliedRule(Substitution rule) {
    final PlanContext plan = translateAsPlan(rule).getLeft();
    completePlan(plan);

    final String str = stringifyTree(plan, plan.root());
    final Set<String> optimized0 = optimizeAsString(plan, bank);
    bank.remove(rule);
    final Set<String> optimized1 = optimizeAsString(plan, bank);
    optimized0.remove(str);
    optimized1.remove(str);

    return !optimized1.isEmpty() && optimized1.containsAll(optimized0);
  }

  private static Set<String> optimizeAsString(PlanContext plan, SubstitutionBank rules) {
    final Optimizer optimizer = Optimizer.mk(rules);
    //    optimizer.setTracing(true);
    final Set<PlanContext> optimized = optimizer.optimize(plan);
    //    for (PlanContext opt : optimized) OptimizerSupport.dumpTrace(optimizer, opt);
    return SetSupport.map(optimized, it -> stringifyTree(it, it.root()));
  }

  private static void completePlan(PlanContext plan) {
    final int oldRoot = plan.root();
    final PlanKind oldRootKind = plan.kindOf(oldRoot);
    if (oldRootKind != PlanKind.Join && !oldRootKind.isFilter()) return;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values inValues = valuesReg.valuesOf(oldRoot);
    final List<String> names = ListSupport.map(inValues, Value::name);
    final List<Expression> exprs = ListSupport.map(inValues, PlanSupport::mkColRefExpr);
    zip(exprs, inValues, (e, v) -> valuesReg.bindValueRefs(e, newArrayList(v)));

    final ProjNode proj = ProjNode.mk(false, names, exprs);
    final int projNode = plan.bindNode(proj);
    plan.setChild(projNode, 0, oldRoot);
    plan.setRoot(projNode);
  }

  public static void main(String[] args) throws IOException {
    //    final SubstitutionBank bank =
    //        SubstitutionSupport.loadBank(Path.of("wtune_data", "rules.txt.bak"));
    //    final SubstitutionBank rules =
    //        SubstitutionSupport.loadBank(Path.of("wtune_data", "rules.test.txt"));
    //    for (Substitution rule : rules.rules()) bank.add(rule);
    //    System.out.println(bank.size());
    final Substitution rule =
        Substitution.parse(
            "Proj<a5 s1>(Filter<p1 a4>(InnerJoin<a2 a3>(Input<t0>,Proj<a1 s0>(Filter<p0 a0>(Input<t1>)))))|Proj<a10 s2>(Filter<p3 a9>(Filter<p2 a8>(InnerJoin<a6 a7>(Input<t2>,Input<t3>))))|AttrsSub(a0,t1);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,s0);AttrsSub(a4,t0);AttrsSub(a5,t0);TableEq(t2,t0);TableEq(t3,t1);AttrsEq(a6,a2);AttrsEq(a7,a3);AttrsEq(a8,a0);AttrsEq(a9,a4);AttrsEq(a10,a5);PredicateEq(p2,p0);PredicateEq(p3,p1);SchemaEq(s2,s1)");
    //    System.out.println(bank.contains(rule));
    //    System.out.println(rule);
    //    System.out.println(new ReduceRuleBank(bank).isImpliedRule(rule));
    System.out.println(isUselessRule1(rule));
    System.out.println(isUselessRule2(rule));
  }
}
