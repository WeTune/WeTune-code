package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.LeveledException;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.support.Workflow;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.joining;

public class OptimizeStatements implements Runner {
  private Path inputFile, optOutFile, traceOutFile, errFile;
  private String app, startFrom;
  private boolean single;
  private boolean echo;
  private PrintWriter out, trace, err;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions"));
    optOutFile = Path.of(args.getOptional("-o", String.class, "wtune_data/optimization.out"));
    traceOutFile = Path.of(args.getOptional("-t", String.class, "wtune_data/optimization.trace"));
    errFile = Path.of(args.getOptional("-e", String.class, "wtune_data/optimization.err"));
    app = args.getOptional("app", String.class, "");
    startFrom = args.getOptional("from", String.class, "");
    single = args.getOptional("single", boolean.class, false);
    echo = args.getOptional("echo", boolean.class, true);
  }

  private PlanNode mkPlan(Statement stmt) {
    final Schema schema = stmt.app().schema("base", true);
    try {
      final ASTNode ast = stmt.parsed();
      ast.context().setSchema(schema);
      Workflow.normalize(ast);
      final PlanNode plan = PlanSupport.assemblePlan(ast, schema);
      return plan;
    } catch (LeveledException ex) {
      if (ex.level() != LeveledException.Level.UNSUPPORTED)
        System.out.printf("[Plan] %s: %s\n", stmt, ex.getMessage());

    } catch (Throwable ex) {
      System.out.printf("[Plan] %s: %s\n", stmt, ex.getMessage());
    }

    return null;
  }

  private void optimizeOne(SubstitutionBank bank, Statement stmt) {
    if (echo) System.out.println(stmt);

    try {
      final PlanNode plan = mkPlan(stmt);
      final Optimizer optimizer = Optimizer.mk(bank);
      optimizer.setTimeout(30000);
      optimizer.setTracing(true);

      final Set<PlanNode> optimized = optimizer.optimize(plan);

      int i = 0;

      synchronized (out) {
        for (PlanNode opt : optimized) {
          out.printf("%s\t%d\t%s\n", stmt, i, PlanSupport.translateAsAst(opt));
          final String steps =
              joining(",", optimizer.traceOf(opt), it -> String.valueOf(it.substitutionId()));
          trace.printf("%s\t%d\t%s\n", stmt, i, steps);

          ++i;
        }
      }

    } catch (Throwable ex) {
      if (!isIgnorable(ex)) {
        synchronized (err) {
          err.println(stmt);
          ex.printStackTrace(err);
        }
      }
    }
  }

  @Override
  public void run() throws Exception {
    final SubstitutionBank bank = SubstitutionSupport.loadBank(inputFile);
    boolean running = startFrom.equals("");

    out = new PrintWriter(Files.newOutputStream(optOutFile));
    trace = new PrintWriter(Files.newOutputStream(traceOutFile));
    err = new PrintWriter(Files.newOutputStream(errFile));

    for (Statement stmt : Statement.findAll()) {
      if (startFrom.equals(stmt.toString())) running = true;
      if (!running) continue;
      if (!"".equals(app) && !app.equals(stmt.appName())) continue;

      optimizeOne(bank, stmt);

      if (single) break;
    }

    out.close();
    trace.close();
    err.close();
  }

  private static boolean isIgnorable(Throwable ex) {
    return (ex instanceof LeveledException && ((LeveledException) ex).ignorable());
  }
}
