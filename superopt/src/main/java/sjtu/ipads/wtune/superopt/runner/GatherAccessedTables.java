package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.schema.Table;
import sjtu.ipads.wtune.stmt.Statement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.FuncSupport.deaf;
import static sjtu.ipads.wtune.common.utils.IOSupport.checkFileExists;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.TableName_Table;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Simple_Table;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.SimpleSource;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.nodeLocator;

public class GatherAccessedTables implements Runner {
  private Path inFile, outFile;
  private Set<String> processedStmts;
  private Map<String, Set<String>> usedTables;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final Path dir = Path.of(args.getOptional("D", "dir", String.class, "wtune_data"));
    final String inFilePath =
        args.getOptional("i", "input", String.class, "rewrite/result/1_query.tsv");
    String outFileName = args.getOptional("o", "output", String.class, null);
    inFile = dir.resolve(inFilePath);
    checkFileExists(inFile);

    if (outFileName == null) {
      final String inFileName = inFile.getFileName().toString();
      if ("1_query.tsv".equals(inFileName)) outFileName = "1_tables.txt";
      else if ("2_query.tsv".equals(inFileName)) outFileName = "2_tables.txt";
      else outFileName = inFileName + ".tables.txt";
    }

    outFile = inFile.resolveSibling(outFileName);
  }

  @Override
  public void run() throws Exception {
    processedStmts = new HashSet<>();
    usedTables = new HashMap<>();
    Files.lines(inFile).forEach(this::processLine);
    for (var pair : usedTables.entrySet()) {
      IOSupport.appendTo(
          outFile,
          writer -> {
            writer.print(pair.getKey());
            writer.print(':');
            writer.println(joining(",", pair.getValue()));
          });
    }
  }

  private void processLine(String line) {
    final String[] field = line.split("\t", 3);
    if (field.length < 3) return;

    if (!processedStmts.add(field[0])) return;
    final String[] split = field[0].split("-", 2);
    if (split.length < 2) return;

    final String appName = split[0];
    final String stmtId = split[1];
    final Statement stmt = Statement.findOne(appName, Integer.parseInt(stmtId));
    if (stmt == null) return;

    final Schema schema = stmt.app().schema("base");
    final SqlNodes tableSources = nodeLocator().accept(SimpleSource).gather(stmt.ast());
    for (SqlNode tableSource : tableSources) {
      final String tableName = tableSource.$(Simple_Table).$(TableName_Table);
      final Table table = schema.table(tableName);
      if (table != null) usedTables.computeIfAbsent(appName, deaf(HashSet::new)).add(tableName);
    }
  }
}
