package sjtu.ipads.wtune.superopt;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;
import static sjtu.ipads.wtune.superopt.internal.WeTuneHelper.optimize;
import static sjtu.ipads.wtune.superopt.internal.WeTuneHelper.optimizeWithTrace;
import static sjtu.ipads.wtune.superopt.internal.WeTuneHelper.pickMinCost;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.LogManager;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.fragment.ToASTTranslator;
import sjtu.ipads.wtune.superopt.internal.Enumerator;
import sjtu.ipads.wtune.superopt.internal.ProofRunner;
import sjtu.ipads.wtune.superopt.optimizer.Substitution;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;

public class Main {

  private static final String LOGGER_CONFIG =
      ".level = INFO\n"
          + "java.util.logging.ConsoleHandler.level = INFO\n"
          + "handlers=java.util.logging.ConsoleHandler\n"
          + "java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\n"
          + "java.util.logging.SimpleFormatter.format=[%1$tm/%1$td %1$tT][%3$10s][%4$s] %5$s %n\n";

  static {
    try {
      LogManager.getLogManager()
          .readConfiguration(new ByteArrayInputStream(LOGGER_CONFIG.getBytes()));
    } catch (IOException ignored) {
    }
  }

  private static PrintWriter out, err;

  private static void cleanBank() throws IOException {
    final SubstitutionBank bank =
        SubstitutionBank.make()
            .importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));
    try (final PrintWriter writer =
        new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "filtered_bank")))) {
      for (Substitution substitution : bank) {
        writer.println("====");
        writer.println(substitution);
      }
    }
  }

  private static void printReadableBank() throws IOException {
    final SubstitutionBank bank =
        SubstitutionBank.make()
            .importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));
    try (final PrintWriter writer =
        new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "readable_bank")))) {
      for (Substitution substitution : bank) {
        writer.println("====");
        final ToASTTranslator translator =
            ToASTTranslator.build().setNumbering(substitution.numbering());
        writer.println(translator.translate(substitution.g0()));
        writer.println(translator.translate(substitution.g1()));
        writer.println(substitution);
      }
    }
  }

  private static SubstitutionBank loadBank() throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "filtered_bank"));
    return SubstitutionBank.make().importFrom(lines, false);
  }

  // optimize single
  private static void test0() throws IOException {
    final SubstitutionBank bank = loadBank();
    final String sql =
        "SELECT COUNT(`product0_`.`product_id`) AS `col_0_0_` FROM `product` AS `product0_` INNER JOIN `product_category` AS `categories2_` ON `product0_`.`product_id` = `categories2_`.`product_id` INNER JOIN `product_description` AS `descriptio1_` ON `product0_`.`product_id` = `descriptio1_`.`product_id` INNER JOIN `category` AS `category3_` ON `categories2_`.`category_id` = `category3_`.`category_id` WHERE `category3_`.`category_id` IN (?) AND `descriptio1_`.`language_id` = 1 AND `product0_`.`manufacturer_id` = 1 AND `product0_`.`available` = 1 AND `product0_`.`date_available` <= '2019-10-21 21:17:32.7' AND `product0_`.`merchant_id` = 1";

    //        final Statement stmt = Statement.findOne("diaspora", 460);
    final Statement stmt = Statement.findOne("solidus", 560);

    final ASTNode ast = stmt.parsed();
    //    final ASTNode ast = ASTParser.mysql().parse(sql);
    //    final ASTNode ast = ASTParser.postgresql().parse(sql);
    final Schema schema = stmt.app().schema("base", true);
    //    final Schema schema = App.of("discourse").schema("base", true);
    ast.context().setSchema(schema);
    normalize(ast);

    System.out.println(stmt);
    System.out.println(ast.toString(false));

    //    System.out.println(UniquenessInference2.inferUniqueness(ToPlanTranslator.toPlan(ast)));

    final List<ASTNode> transformed = optimize(stmt, bank);
    System.out.println(transformed.size());

    for (ASTNode opt : transformed) System.out.println(opt);
    System.out.println(stmt);

    //    System.out.println(pickMinCost(stmt.parsed(), transformed, stmt.app().dbProps()));
  }

  // optimize batch
  private static void test1() throws IOException {
    final SubstitutionBank bank = loadBank();
    out = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "optimizations")));
    err = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "err")));

    App.all().forEach(it -> it.schema("base", true)); // trigger, avoid concurrent initialization
    //        doOptimize(Statement.findOne("broadleaf", 200), bank);
    Statement.findByApp("shopizer").parallelStream().forEach(it -> getOptimization(it, bank));
  }

  // optimization trace
  private static void test2() throws IOException {
    final SubstitutionBank bank = loadBank();
    out = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "traces")));
    err = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "err")));

    App.all().forEach(it -> it.schema("base", true)); // trigger, avoid concurrent initialization
    //    getOptimizedTrace(Statement.findOne("discourse", 2596), bank);

    final List<Statement> targets = listMap(Statement::original, Statement.findAllRewritten());
    targets.parallelStream().map(Statement::original).forEach(it -> getOptimizationTrace(it, bank));
  }

  // optimization time
  private static void test3() throws IOException {
    final SubstitutionBank bank = loadBank();
    out = new PrintWriter(Files.newOutputStream(Paths.get("wtune_data", "timing.csv")));

    for (Statement stmt : Statement.findAllRewritten()) {
      getOptimizationTime(stmt, bank);
    }

    out.close();
  }

  private static void getOptimization(Statement stmt, SubstitutionBank bank) {
    try {
      System.out.println(stmt);
      final List<ASTNode> candidates = optimize(stmt, bank);
      final var result = pickMinCost(stmt.parsed(), candidates, stmt.app().dbProps());
      if (result == null) return;

      synchronized (out) {
        out.printf(
            "%s;%d;%s;%f;%f\n",
            stmt.appName(),
            stmt.stmtId(),
            result.getLeft(),
            result.getRight()[0],
            result.getRight()[1]);
        out.flush();
      }

    } catch (Throwable ex) {
      synchronized (err) {
        err.printf("======\n%s-%d\n", stmt.appName(), stmt.stmtId());
        ex.printStackTrace(err);
        err.flush();
      }
    }
  }

  private static void getOptimizationTrace(Statement stmt, SubstitutionBank bank) {
    System.out.println(stmt);

    final var optResult = optimizeWithTrace(stmt, bank);
    final var costResult = pickMinCost(stmt.parsed(), optResult.keySet(), stmt.app().dbProps());

    if (costResult == null) return;

    final List<Substitution> trace = optResult.get(costResult.getLeft());

    synchronized (out) {
      for (Substitution substitution : trace)
        out.printf("%s;%d;%d\n", stmt.appName(), stmt.stmtId(), substitution.index());
      out.flush();
    }
  }

  private static void getOptimizationTime(Statement stmt, SubstitutionBank bank) {
    final long startTime = System.currentTimeMillis();
    optimize(stmt, bank);
    final long end = System.currentTimeMillis();
    synchronized (out) {
      System.out.println(stmt + " " + (end - startTime));
      out.printf("%s;%d;%d", stmt.appName(), stmt.stmtId(), end - startTime);
      out.flush();
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length >= 1 && "run".equals(args[0])) {
      ProofRunner.build(args).run();

    } else {
      //      test0();
      //      test1();
      //      test2();
      //      test3();
      System.out.println(Enumerator.enumPlans().size());
    }
  }
}
