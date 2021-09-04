package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

import java.nio.file.Path;

public class OptimizeStatements implements Runner {
  private Path inputFile;
  private Path outputFile;
  private boolean echo;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions"));
    outputFile = Path.of(args.getOptional("-o", String.class, "wtune_data/optimized"));
    echo = args.getOptional("-echo", boolean.class, true);
  }

  @Override
  public void run() throws Exception {
    for (Statement stmt : Statement.findAll()) {
      final Schema schema = stmt.app().schema("base", true);
      try {
        PlanSupport.assemblePlan(stmt.parsed(), schema);
      } catch (Throwable ex) {
        System.out.println(stmt);
      }
    }
    //    final SubstitutionBank bank = SubstitutionSupport.loadBank(inputFile, false);
    //    final int oldSize = bank.size();
    //    final SubstitutionBank minimized = SubstitutionSupport.minimize(bank);
    //    final int minSize = minimized.size();
    //
    //    if (echo) System.out.printf("%d -> %d\n", oldSize, minSize);
    //
    //    try (final PrintWriter out = new PrintWriter(Files.newOutputStream(outputFile))) {
    //      minimized.forEach(it -> out.println(it.canonicalStringify()));
    //    }
  }
}
