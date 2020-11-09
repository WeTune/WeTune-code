package sjtu.ipads.wtune.learning.support;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.resolver.BoolExprResolver;
import sjtu.ipads.wtune.stmt.statement.Issue;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtractFeature {
  public static void main(String[] args) throws IOException {
    Setup._default().registerAsGlobal().setup();

    final String path = args[args.length - 1];
    fromFile(path);
    fromIssues(path);
  }

  private static void fromFile(String pathStr) throws IOException {
    final List<QueryFeature> features = new ArrayList<>();
    final Path path = Path.of(pathStr);

    final List<String> queries = new ArrayList<>();
    int i = 0;
    for (String line : Files.readAllLines(path)) {
      ++i;
      try {
        final SQLNode query = SQLParser.ofDb("postgresql").parse(line);
        queries.add("\"" + query.toString(false).replaceAll("\"", "\"\"") + "\"");

        new BoolExprResolver().resolve(null, query);
        features.add(new FeatureExtractor().analyze(query));
      } catch (Exception ex) {
        System.err.println("" + i + ": " + line);
        throw ex;
      }
    }

    final List<String> lines =
        features.stream().map(ExtractFeature::stringifyFeature).collect(Collectors.toList());

    final Path out = path.getParent().resolve("features.csv");
    final Path queryOut = path.getParent().resolve("queries.csv");
    Files.write(out, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    Files.write(queryOut, queries, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
  }

  private static void fromIssues(String pathStr) throws IOException {
    final List<QueryFeature> features = new ArrayList<>();
    final Path path = Path.of(pathStr);

    for (Issue issue : Issue.findAll()) {
      final Statement stmt = issue.stmt();
      if ((stmt.appName().equals("discourse") && stmt.stmtId() >= 5178)
          || (stmt.appName().equals("spree") && stmt.stmtId() >= 1199)) continue;
      final SQLNode query = stmt.parsed();
      new BoolExprResolver().resolve(null, query);
      features.add(new FeatureExtractor().analyze(query));
    }

    final List<String> lines =
        features.stream().map(ExtractFeature::stringifyFeature).collect(Collectors.toList());
    final Path out = path.getParent().resolve("features.csv");
    Files.write(out, lines, StandardOpenOption.APPEND);
  }

  private static String stringifyFeature(QueryFeature feature) {
    return String.format(
        "%d,%d,%d,%d,%d,%d",
        feature.queries,
        feature.tables,
        feature.predicates,
        feature.groupBys,
        feature.orderBys,
        feature.distincts);
  }
}
