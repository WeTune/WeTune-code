package sjtu.ipads.wtune.rawlog;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  public static void main(String[] args) throws IOException {
    final String baseDir = "/home/cleveland/Projects/wtune-code/data/logs/org.broadleafcommerce/";
    final Path sqlLog = Paths.get(baseDir, "stmts.log");
    final Path traceLog = Paths.get(baseDir, "traces.log");
    LogReader.forTaggedFormat().readFrom(sqlLog, traceLog).stream()
        .limit(10)
        .forEach(System.out::println);
  }
}
