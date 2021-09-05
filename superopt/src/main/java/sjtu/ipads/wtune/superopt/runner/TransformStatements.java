package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.LeveledException;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.support.Workflow;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.countOccurrences;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

public class TransformStatements implements Runner {
  private Path inFile, outFile, traceFile, errFile;
  private String app, startFrom;
  private boolean single;
  private boolean echo;
  private PrintWriter out, trace, err;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inFile = Path.of(args.getOptional("-i", String.class, "wtune_data/substitutions"));
    outFile = Path.of(args.getOptional("-o", String.class, "wtune_data/transformation.out"));
    traceFile = Path.of(args.getOptional("-t", String.class, "wtune_data/transformation.trace"));
    errFile = Path.of(args.getOptional("-e", String.class, "wtune_data/transformation.err"));
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
      return PlanSupport.assemblePlan(ast, schema);
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
    if (isTooComplex(stmt.rawSql())) return;

    try {
      final PlanNode plan = mkPlan(stmt);
      if (plan == null) return;
      if (isSimple(plan)) return;

      final Optimizer optimizer = Optimizer.mk(bank);
      optimizer.setTimeout(30000);
      optimizer.setTracing(true);

      final Set<PlanNode> optimized = optimizer.optimize(plan);

      int i = 0;

      synchronized (out) {
        for (PlanNode opt : optimized) {
          final ASTNode ast;
          try {
            ast = PlanSupport.translateAsAst(opt);
            System.out.println(ast);
          } catch (Throwable ex) {
            continue;
          }

          out.printf("%s\t%d\t%s\n", stmt, i, ast);
          final String steps =
              joining(",", optimizer.traceOf(opt), it -> String.valueOf(it.substitutionId()));
          trace.printf("%s\t%d\t%s\n", stmt, i, steps);

          ++i;
        }
      }

    } catch (Throwable ex) {
      if (!isIgnorable(ex)) {
        if (echo) System.err.println(stmt + " error: " + ex.getMessage());
        synchronized (err) {
          err.print("> ");
          err.println(stmt);
          ex.printStackTrace(err);
        }
      }
    }
  }

  private void optimizeAll(SubstitutionBank bank, List<Statement> stmts) {
    boolean running = startFrom.equals("");

    for (Statement stmt : stmts) {
      if (startFrom.equals(stmt.toString())) running = true;
      if (!running) continue;
      if (!"".equals(app) && !app.equals(stmt.appName())) continue;

      optimizeOne(bank, stmt);

      if (single) break;
    }
  }

  @Override
  public void run() throws Exception {
    final SubstitutionBank bank = SubstitutionSupport.loadBank(inFile);

    out = new PrintWriter(Files.newOutputStream(outFile));
    trace = new PrintWriter(Files.newOutputStream(traceFile));
    err = new PrintWriter(Files.newOutputStream(errFile));

    optimizeAll(bank, Statement.findAll());

    out.close();
    trace.close();
    err.close();
  }

  private static boolean isIgnorable(Throwable ex) {
    return (ex instanceof LeveledException && ((LeveledException) ex).ignorable());
  }

  private static boolean isTooComplex(String sql) {
    return countOccurrences(sql.toLowerCase(Locale.ROOT), "join") >= 10;
  }

  private static boolean isSimple(PlanNode plan) {
    if (plan.kind() == LIMIT) plan = plan.predecessors()[0];
    if (plan.kind() == SORT) plan = plan.predecessors()[0];
    if (plan.kind() != PROJ || ((ProjNode) plan).isDeduplicated()) return false;

    PlanNode path = plan.predecessors()[0];
    while (path.kind() == SIMPLE_FILTER) path = path.predecessors()[0];

    return path.kind() == INPUT;
  }
}
