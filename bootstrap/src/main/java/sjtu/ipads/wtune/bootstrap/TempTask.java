package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.analyzer.TableAccessAnalyzer;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Issue;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TempTask implements Task {
  @Override
  public void doTask(String appName) {
    final Set<Table> tables =
        Issue.findByApp(appName).stream()
            .map(Issue::stmt)
            .map(it -> it.analyze(TableAccessAnalyzer.class))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    System.out.println(tables);
  }
}
