package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.superopt.constraint.ConstraintSupport;
import sjtu.ipads.wtune.superopt.constraint.EnumerationMetrics;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.util.List;

import static sjtu.ipads.wtune.superopt.constraint.ConstraintSupport.*;
import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.dataDir;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.loadBank;

public class RunEnumExample implements Runner {
  private Substitution rule;
  private boolean dump;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final String rules = args.getOptional("R", "rules", String.class, "prepared/rules.example.txt");
    dump = args.getOptional("v", "verbose", boolean.class, true);

    final SubstitutionBank bank = loadBank(dataDir().resolve(rules));
    final int index = args.getOptional("I", "index", int.class, 0);
    for (Substitution rule : bank.rules()) {
      if (rule.id() == index) {
        this.rule = rule;
        break;
      }
    }
    if (this.rule == null) throw new IllegalArgumentException("No such rule: " + index);
  }

  @Override
  public void run() throws Exception {
    final int tweak = dump ? ENUM_FLAG_DUMP : 0;

    final List<Substitution> rules =
        enumConstraints(rule._0(), rule._1(), -1, tweak, rule.naming());

    if (Commons.isNullOrEmpty(rules)) {
      System.out.println();
      System.out.println("Not Found Rules.");
      return;
    }

    System.out.println();
    System.out.printf("Found Rules (%d in total) \n", rules.size());
    for (Substitution rule : rules) System.out.println("  " + rule.toString());

    final EnumerationMetrics metric = ConstraintSupport.getEnumerationMetric();
    System.out.println();
    System.out.println("Metrics: ");
    System.out.println("  C* size: " + metric.numTotalConstraintSets.value());
    System.out.println(
        "  # of enumerated constraint sets: " + metric.numEnumeratedConstraintSets.value());
    System.out.println("  # of EQ constraint sets: " + metric.numEq.value());
    System.out.println("  # of NEQ constraint sets: " + metric.numNeq.value());
    System.out.println("  # of UNKNOWN constraint sets: " + metric.numUnknown.value());
    System.out.println(
        "  # of built-in verifier invocations: " + metric.numProverInvocations.value());
    System.out.println("  # of EQ cache hit: " + metric.numCacheHitEq.value());
    System.out.println("  # of NEQ cache hit: " + metric.numCacheHitNeq.value());
    System.out.println("  # of Relaxing: " + metric.numCacheHitNeq.value());
  }
}
