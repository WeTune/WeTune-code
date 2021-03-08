package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.fragment.ToASTTranslator;
import sjtu.ipads.wtune.superopt.internal.Optimizer;
import sjtu.ipads.wtune.superopt.internal.ProofRunner;
import sjtu.ipads.wtune.superopt.internal.UniquenessInference;
import sjtu.ipads.wtune.superopt.optimization.Substitution;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.LogManager;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

public class Main {

  private static final String LOGGER_CONFIG =
      ".level = FINER\n"
          + "java.util.logging.ConsoleHandler.level = FINER\n"
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

  public static void main(String[] args) throws IOException {
    if (args.length >= 1 && "run".equals(args[0])) {
      ProofRunner.build(args).run();

    } else {
      test0();
    }
  }

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

  private static void test0() throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "substitution_bank"));
    final SubstitutionBank bank =
        SubstitutionBank.make().importFrom(lines.subList(0, lines.size() - 1));
    bank.importFrom(singletonList(lines.get(lines.size() - 1)));

    final String sql =
        "SELECT \"group_users\".\"group_id\" AS \"group_id\" FROM (SELECT * FROM \"group_users\" AS \"gu\" INNER JOIN \"groups\" AS \"groups\" ON \"gu\".\"group_id\" = \"groups\".\"id\" WHERE \"gu\".\"user_id\" = 779 AND \"gu\".\"group_id\" > 0) AS \"sub0\" INNER JOIN \"group_users\" AS \"group_users\" ON \"sub0\".\"id\" = \"group_users\".\"group_id\" WHERE \"group_users\".\"user_id\" = 779";
    final Statement stmt = Statement.findOne("diaspora", 224);

    final ASTNode ast = stmt.parsed();
    //        final ASTNode ast = ASTParser.mysql().parse(sql);
    //    final ASTNode ast = ASTParser.postgresql().parse(sql);
    final Schema schema = stmt.app().schema("base", true);
    //    final Schema schema = App.of("discourse").schema("base", true);
    ast.context().setSchema(schema);
    normalize(ast);

    System.out.println(stmt);
    System.out.println(ast.toString(false));

    final PlanNode plan = ToPlanTranslator.toPlan(ast);
    System.out.println(UniquenessInference.inferUniqueness(plan));
    final List<ASTNode> optimized = Optimizer.make(bank, schema).optimize(ast);
    System.out.println(optimized.size());

    for (ASTNode opt : optimized) System.out.println(opt);
    System.out.println(stmt);
  }
}
