package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanKind;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.countOccurrences;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.normalizeAst;

public class OptimizeQuery implements Runner {
  private Path out, trace, err;
  private String app, startFrom;
  private boolean single;
  private boolean echo;
  private SubstitutionBank rules;

  @Override
  public void prepare(String[] argStrings) throws IOException {
    final Args args = Args.parse(argStrings, 1);
    echo = args.getOptional("echo", boolean.class, true);

    final Path parentDir = Path.of(args.getOptional("dir", String.class, "wtune_data"));
    final String subDirName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final Path dir = parentDir.resolve("opt" + subDirName);

    if (!Files.exists(dir)) Files.createDirectories(dir);

    out = dir.resolve("opts");
    trace = dir.resolve("log");
    err = dir.resolve("error");

    app = args.getOptional("app", String.class, "");
    startFrom = args.getOptional("from", String.class, "");
    single = args.getOptional("single", boolean.class, false);

    rules = SubstitutionSupport.loadBank(parentDir.resolve("rules").resolve("rules"));
  }

  @Override
  public void run() throws Exception {
    optimizeAll(Statement.findAll());
  }

  private PlanContext parsePlan(Statement stmt) {
    final Schema schema = stmt.app().schema("base", true);
    try {
      final SqlNode ast = parseSql(stmt.app().dbType(), stmt.rawSql());
      normalizeAst(ast);
      return PlanSupport.assemblePlan(ast, schema);
    } catch (Throwable ex) {
      System.out.printf("[Plan] %s: %s\n", stmt, ex.getMessage());
    }

    return null;
  }

  private void optimizeOne(Statement stmt) {
    if (echo) System.out.println(stmt);
    if (isTooComplex(stmt.rawSql())) return;

    try {
      final PlanContext plan = parsePlan(stmt);
      if (plan == null) return;
      if (isSimple(plan)) return;

      final Optimizer optimizer = Optimizer.mk(rules);
      optimizer.setTimeout(5000);
      optimizer.setTracing(true);

      final Set<PlanContext> optimized = optimizer.optimize(plan);
      if (optimized.isEmpty()) return;

      final List<String> optimizedSql = new ArrayList<>(optimized.size());
      final List<String> traces = new ArrayList<>(optimized.size());
      for (PlanContext opt : optimized) {
        try {
          final SqlNode sqlNode = translateAsAst(opt, opt.root(), false);
          final String sql = sqlNode.toString();
          optimizedSql.add(sql);
        } catch (Throwable ex) {
          continue;
        }

        final String trace =
            joining(",", optimizer.traceOf(opt), it -> String.valueOf(it.ruleId()));
        traces.add(trace);
      }

      IOSupport.appendTo(
          out,
          writer -> {
            for (int i = 0, bound = optimizedSql.size(); i < bound; i++)
              writer.printf("%s\t%d\t%s\n", stmt, i, optimizedSql.get(i));
          });

      IOSupport.appendTo(
          trace,
          writer -> {
            for (int i = 0, bound = traces.size(); i < bound; i++)
              writer.printf("%s\t%d\t%s\n", stmt, i, traces.get(i));
          });

    } catch (Throwable ex) {
      if (echo) System.err.println(stmt + " error: " + ex.getMessage());
      IOSupport.appendTo(
          err,
          writer -> {
            writer.print(" >");
            writer.println(stmt);
            ex.printStackTrace(writer);
          });
    }
  }

  private void optimizeAll(List<Statement> stmts) {
    boolean running = startFrom.equals("");

    for (Statement stmt : stmts) {
      if (startFrom.equals(stmt.toString())) running = true;
      if (!running) continue;
      if (!"".equals(app) && !app.equals(stmt.appName())) continue;

      optimizeOne(stmt);

      if (single) break;
    }
  }

  private static boolean isTooComplex(String sql) {
    return countOccurrences(sql.toLowerCase(Locale.ROOT), "join") >= 10;
  }

  private static boolean isSimple(PlanContext plan) {
    int node = plan.root();
    if (plan.kindOf(node) == PlanKind.Limit) node = plan.childOf(node, 0);
    if (plan.kindOf(node) == PlanKind.Sort) node = plan.childOf(node, 0);
    if (plan.kindOf(node) != PlanKind.Proj || PlanSupport.isDedup(plan, node)) return false;
    node = plan.childOf(node, 0);

    while (plan.kindOf(node).isFilter()) node = plan.childOf(node, 0);
    return plan.kindOf(node) == PlanKind.Input;
  }
}
