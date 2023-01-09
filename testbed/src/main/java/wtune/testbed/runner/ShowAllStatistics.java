package wtune.testbed.runner;

import wtune.common.utils.Args;
import wtune.common.utils.IOSupport;
import wtune.stmt.Statement;
import wtune.stmt.support.OptimizerType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


import static wtune.testbed.runner.GenerateTableData.*;

public class ShowAllStatistics implements Runner {
  // Input
  private Path rewriteTraceFile;
  private Path profileDir;

  // Output
  private Path outRulesFile;
  private Path outStatistic;
  private Path outOptInfo;
  private Path outUsefulRulesFile;
  private Path outWetuneSurpassesCalcite;
  private Path workloadBase;
  private Path workloadZipf;
  private Path workloadLarge;
  private Path workloadLargeZipf;
  private Path statistics;
  private OptimizerType optimizer;
  private boolean calcite;
  private boolean all;
  private static final double BOUND_VALID = 0.1;
  private static final double BOUND_INVALID = -0.05;
  private Set<String> rulesRecord;
  private Map<String, String> rulesPool;

  public static void main(String[] args) throws Exception {
    args = new String[]{"ShowAllStatistics", "-all"};
    ShowAllStatistics showAllStatistics = new ShowAllStatistics();
    showAllStatistics.prepare(args);
    showAllStatistics.run();
  }

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);

    final Path dataDir = Runner.dataDir();
    final Path rewriteDir =
            dataDir.resolve(args.getOptional("rewriteDir", String.class, "rewrite/result"));
    rewriteTraceFile =
            rewriteDir.resolve(args.getOptional("traceFile", String.class, "2_trace.tsv"));
    final Path usedRulesFile = rewriteDir.resolve("1_rules.tsv");
    profileDir = dataDir.resolve(args.getOptional("profileDir", String.class, "profile/result"));

    IOSupport.checkFileExists(rewriteTraceFile);
    IOSupport.checkFileExists(profileDir);
    IOSupport.checkFileExists(usedRulesFile);

    final String subDirName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    final String outDir = args.getOptional("out", "O", String.class, "viewall");
    final Path dir = dataDir.resolve(outDir).resolve("view" + subDirName);

    if (!Files.exists(dir)) Files.createDirectories(dir);

    outRulesFile = dir.resolve("rules.tsv");
    outStatistic = dir.resolve("statistic.tsv");
    outWetuneSurpassesCalcite = dir.resolve("wetuneSurpassesCalcite.tsv");
    outOptInfo = dir.resolve("optimizationInfo.tsv");
    outUsefulRulesFile = dir.resolve("usefulRules.tsv");


    optimizer = OptimizerType.valueOf(args.getOptional("opt", "optimizer", String.class, "WeTune"));
    calcite = args.getOptional("calcite", boolean.class, false);

    all = args.getOptional("all", boolean.class, false);
    if (all){
      Path fourWorkloadsResultDir = dataDir.resolve("viewall_statistics").resolve("view" + subDirName);
      if (!Files.exists(fourWorkloadsResultDir)) Files.createDirectories(fourWorkloadsResultDir);
      workloadBase = fourWorkloadsResultDir.resolve("base.tsv");
      workloadZipf = fourWorkloadsResultDir.resolve("zipf.tsv");
      workloadLarge = fourWorkloadsResultDir.resolve("large.tsv");
      workloadLargeZipf = fourWorkloadsResultDir.resolve("large_zipf.tsv");
      statistics = fourWorkloadsResultDir.resolve("statistics");
    }

    rulesRecord = new HashSet<>();
    rulesPool = new HashMap<>();
    List<String> rules = Files.readAllLines(usedRulesFile);
    rules.stream()
            .forEach(rule -> {
              String[] split = rule.split("\t", 2);
              rulesPool.put(split[0], split[1]);
            });
  }

  @Override
  public void run() throws Exception {
//        collectRules();

    if (calcite) collectStatisticCalcite();
    else if (all) collectAllStatisticCalcite();
    else {
      collectStatistic();
      collectRules();
    }
  }

  private void collectRules() throws IOException {
    IOSupport.appendTo(outUsefulRulesFile, writer -> writer.printf("%s\t%s\n", "ruleId", "rule"));
    List<String> ruleList = new ArrayList<>(rulesRecord);
    List<Integer> ruleIdList =
            ruleList.stream()
                    .map(Integer::parseInt)
                    .sorted().toList();
    for (Integer ruleId : ruleIdList) {
      IOSupport.appendTo(outUsefulRulesFile, writer -> writer.printf("%d\t%s\n", ruleId, rulesPool.get(String.valueOf(ruleId))));
    }
  }

  private void collectStatistic() throws IOException {
    Map<String, StatementStatistic> statementStatMap = new HashMap<>();
    Map<String, Boolean> workloadExist =
            new HashMap<>(Map.of(BASE, false, ZIPF, false, LARGE, false, LARGE_ZIPF, false));

    for (String tag : List.of(BASE, ZIPF, LARGE, LARGE_ZIPF)) {
      final Path profileFile = profileDir.resolve(tag);
      if (!Files.exists(profileFile)) continue;

      workloadExist.put(tag, true);
      final List<String> lines = Files.readAllLines(profileFile);
      for (int i = 0, bound = lines.size(); i < bound; i += 2) {
        final String[] fieldsBase = lines.get(i).split(";");
        final String[] fieldsOpt = lines.get(i + 1).split(";");

        final String appName = fieldsBase[0];
        final long stmtId = Long.parseLong(fieldsBase[1]);
        final long baseLatency = Long.parseLong(fieldsBase[3]);
        final long optLatency = Long.parseLong(fieldsOpt[3]);
        final double p50Improvement = 1.0 - ((double) optLatency) / ((double) baseLatency);

        final StatementStatistic stat = statementStatMap.computeIfAbsent(
                "%s-%d".formatted(appName, stmtId),
                s -> new StatementStatistic(s.split("-")[0], Integer.parseInt(s.split("-")[1])));
        stat.updateProfile(p50Improvement, tag);
      }
    }

    // Write header
//        final StringBuilder sb = new StringBuilder(
//                String.format("%s\t%s\t%s\t%s\t%s", "appName", "stmtId", "rawSql", "optSql", "usedRules"));
//        writeHeader(workloadExist, sb, outStatistic);

    final StringBuilder sbOptInfo = new StringBuilder(
            String.format("%s\t%s\t%s", "appName", "stmtId", "usedRules"));
    writeHeader(workloadExist, sbOptInfo, outOptInfo);

    // Write data
    List<String> stmtStatKeyList = new ArrayList<>(statementStatMap.keySet());
    Collections.sort(stmtStatKeyList);
    for (String stmtKey : stmtStatKeyList) {
      final StatementStatistic statistic = statementStatMap.get(stmtKey);
//            IOSupport.appendTo(
//                    outStatistic,
//                    writer -> writer.printf("%s\n",
//                            statistic.toString(
//                                    workloadExist.get(BASE),
//                                    workloadExist.get(ZIPF),
//                                    workloadExist.get(LARGE),
//                                    workloadExist.get(LARGE_ZIPF)
//                            )
//                    )
//            );

      String optLine =
              statistic.toString(
                      workloadExist.get(BASE),
                      workloadExist.get(ZIPF),
                      workloadExist.get(LARGE),
                      workloadExist.get(LARGE_ZIPF),
                      BOUND_VALID,
                      BOUND_INVALID
              );
      if (optLine != null) {
        IOSupport.appendTo(
                outOptInfo,
                writer -> writer.printf("%s\n", optLine)
        );
        String ruleTrace = optLine.split("\t")[2];
        for (String ruleId : ruleTrace.split(",")) {
          if (Integer.parseInt(ruleId) > 1) {
            rulesRecord.add(ruleId);
          }
        }
      }
    }
  }


  private void writeHeader(Map<String, Boolean> workloadExist, StringBuilder sbOptInfo, Path outOptInfo) {
    if (workloadExist.get(BASE)) sbOptInfo.append("\t%s".formatted("baseImprove"));
    if (workloadExist.get(ZIPF)) sbOptInfo.append("\t%s".formatted("zipfImprove"));
    if (workloadExist.get(LARGE)) sbOptInfo.append("\t%s".formatted("largeImprove"));
    if (workloadExist.get(LARGE_ZIPF)) sbOptInfo.append("\t%s".formatted("large_zipfImprove"));
    IOSupport.appendTo(outOptInfo, writer -> writer.printf("%s\n", sbOptInfo.toString()));
  }

  private void collectStatisticCalcite() throws IOException {
    Map<String, CalciteStatementStatistic> statementStatMap = new HashMap<>();

    final Path profileFile = profileDir.resolve("base");
    if (!Files.exists(profileFile)) {
      System.out.println("Have not profile base workload of Calcite cases.");
      return;
    }

    final List<String> lines = Files.readAllLines(profileFile);
    for (int i = 0, bound = lines.size(); i < bound; i += 2) {
      final String[] fieldsBase = lines.get(i).split(";");
      final String[] fieldsCalciteOpt = lines.get(i + 1).split(";");
      if (i + 3 >= bound) break;
      final String[] fieldsWeTuneOpt = lines.get(i + 3).split(";");

      final String appName = fieldsBase[0];
      final int stmtId = Integer.parseInt(fieldsBase[1]);
      final String probeAppName = fieldsWeTuneOpt[0];
      final int probeStmtId = Integer.parseInt(fieldsWeTuneOpt[1]);
      if (!(appName.equals(probeAppName) && stmtId == probeStmtId)) {
        continue;
      }

      final int baseLatency = Integer.parseInt(fieldsBase[3]);
      final int calciteOptLatency = Integer.parseInt(fieldsCalciteOpt[3]);
      final int weTuneOptLatency = Integer.parseInt(fieldsWeTuneOpt[3]);
      final double calciteP50Improvement =
              1.0 - ((double) calciteOptLatency) / ((double) baseLatency);
      final double weTuneP50Improvement =
              1.0 - ((double) weTuneOptLatency) / ((double) baseLatency);
      final double weTuneImproveThanCalcite = 1.0 - ((double) weTuneOptLatency) / ((double) calciteOptLatency);

      final CalciteStatementStatistic stat = statementStatMap.computeIfAbsent(
              "%s-%d".formatted(appName, stmtId),
              s -> new CalciteStatementStatistic(s.split("-")[0], Integer.parseInt(s.split("-")[1])));
      stat.updateCalciteImprove(calciteP50Improvement);
      stat.updateWeTuneImprove(weTuneP50Improvement);
      stat.updateWeTuneImproveThanCalcite(weTuneImproveThanCalcite);

      i += 2;
    }

    // Write header
    final String header =
            String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                    "appName", "stmtId",
                    "rawSql", "calciteOptSql", "optSql", "usedRules",
                    "calciteImprove", "weTuneImprove", "weTuneImproveThanCalcite");
    IOSupport.appendTo(outStatistic, writer -> writer.printf("%s\n", header));

    IOSupport.appendTo(outWetuneSurpassesCalcite, writer -> writer.printf("%s\n", header));

    // Write data
    List<String> stmtStatKeyList = new ArrayList<>(statementStatMap.keySet());
    Collections.sort(stmtStatKeyList);
    for (String stmtKey : stmtStatKeyList) {
      final CalciteStatementStatistic statistic = statementStatMap.get(stmtKey);
      IOSupport.appendTo(
              outStatistic, writer -> writer.printf("%s\n", statistic.toString())
      );
      if (statistic.weTuneImproveThanCalcite > 0){
        IOSupport.appendTo(
                outWetuneSurpassesCalcite, writer -> writer.printf("%s\n", statistic.toString())
        );
      }
    }
  }

  private void collectAllStatisticCalcite() throws IOException {
    Map<String, StatementStatistic> statementStatMap = new HashMap<>();
    Map<String, WorkLoadStatistic> workLoadStatisticMap = new HashMap<>();
    Map<String, Boolean> workloadExist =
            new HashMap<>(Map.of(BASE, false, ZIPF, false, LARGE, false, LARGE_ZIPF, false));

    final Path dataDir = Runner.dataDir();
    Path profileDir = dataDir.resolve("viewall/result");
    Path optimizationInfo = profileDir.resolve("optimizationInfo.tsv");

    // profile
    final List<String> lines = Files.readAllLines(optimizationInfo);
    String headerLine = lines.get(0);
    List<String> headerSplit = Arrays.asList(headerLine.split("\t"));
    int idxBase = headerSplit.indexOf("baseImprove"), idxZipf = headerSplit.indexOf("zipfImprove"), idxLarge = headerSplit.indexOf("largeImprove"), idxLarge_zipf = headerSplit.indexOf("large_zipfImprove");
    if (headerLine.contains("baseImprove")) {
      workloadExist.put(BASE, true);
    }
    if (headerLine.contains("zipfImprove")) {
      workloadExist.put(ZIPF, true);
    }
    if (headerLine.contains("largeImprove")) {
      workloadExist.put(LARGE, true);
    }
    if (headerLine.contains("large_zipfImprove")) {
      workloadExist.put(LARGE_ZIPF, true);
    }

    // remove header
    lines.remove(0);
    for (String line: lines) {
      String[] optInfo = line.split("\t");
      final StatementStatistic stat = statementStatMap.computeIfAbsent(
              "%s-%d".formatted(optInfo[0], Integer.parseInt(optInfo[1])),
              s -> new StatementStatistic(s.split("-")[0], Integer.parseInt(s.split("-")[1])));

      if (idxBase != -1 && !optInfo[idxBase].equals("null")) {
        stat.updateProfile(Double.parseDouble(optInfo[idxBase]), BASE);
      }
      if (idxZipf != -1 && !optInfo[idxZipf].equals("null")) {
        stat.updateProfile(Double.parseDouble(optInfo[idxZipf]), ZIPF);
      }
      if (idxLarge != -1 && !optInfo[idxLarge].equals("null")) {
        stat.updateProfile(Double.parseDouble(optInfo[idxLarge]), LARGE);
      }
      if (idxLarge_zipf != -1 && !optInfo[idxLarge_zipf].equals("null")) {
        stat.updateProfile(Double.parseDouble(optInfo[idxLarge_zipf]), LARGE_ZIPF);
      }
    }


    for (String tag: workloadExist.keySet()) {
      WorkLoadStatistic workLoadStatistic = new WorkLoadStatistic();
      if (!workloadExist.get(tag)) continue;
      Path outFile = null;
      switch (tag) {
        case BASE -> outFile = workloadBase;
        case ZIPF -> outFile = workloadZipf;
        case LARGE -> outFile = workloadLarge;
        case LARGE_ZIPF -> outFile = workloadLargeZipf;
      }
      // Write header
      final String header =
              String.format("%s\t%s\t%s",
                      "appName", "stmtId", "weTuneImprove");
      IOSupport.appendTo(outFile, writer -> writer.printf("%s\n", header));
      List<String> stmtStatKeyList = new ArrayList<>(statementStatMap.keySet());
      Collections.sort(stmtStatKeyList);
      for (String key : stmtStatKeyList) {
        StatementStatistic statementStatistic = statementStatMap.get(key);
        if (statementStatistic.getImprovement(tag) == null){
          continue;
        }
        IOSupport.appendTo(outFile, writer -> writer.printf("%s\n", statementStatistic.toString(tag)));
        workLoadStatistic.queriesNumber++;
        if (statementStatistic.getImprovement(tag) >= 0.9){
          workLoadStatistic.queriesNumberWithLatencyReductionOverNinety++;
        }
      }
      List<StatementStatistic> statementStatistics = new ArrayList<>(statementStatMap.values());
      statementStatistics = statementStatistics.stream().filter(statistics -> statistics.getImprovement(tag) != null).collect(Collectors.toList());
      statementStatistics.sort(Comparator.comparing(s -> s.getImprovement(tag)));
      workLoadStatistic.latencyReductionForOverFiftyPercentageQueries = statementStatistics.get(statementStatistics.size() / 2).getImprovement(tag);
      workLoadStatisticMap.put(tag, workLoadStatistic);
    }




    // Write statistic
    for (String workload : List.of(BASE, ZIPF, LARGE, LARGE_ZIPF)){
      if (!workLoadStatisticMap.containsKey(workload)){
        continue;
      }
      WorkLoadStatistic workLoadStatistic = workLoadStatisticMap.get(workload);
      IOSupport.appendTo(
              statistics, writer -> writer.printf("%s\n", workload)
      );
      IOSupport.appendTo(
              statistics, writer -> writer.printf("%s\n", String.format("at least 50%% of the queries achieve >%f%% latency reduction", workLoadStatistic.latencyReductionForOverFiftyPercentageQueries * 100))
      );
      IOSupport.appendTo(
              statistics, writer -> writer.printf("%s\n\n", String.format("optimize %f%%(%d queries) with at least a 90%% latency reduction",
                      1.0 * workLoadStatistic.queriesNumberWithLatencyReductionOverNinety / workLoadStatistic.queriesNumber * 100, workLoadStatistic.queriesNumberWithLatencyReductionOverNinety))
      );
    }
  }

  private class StatementStatistic {
    private final Statement stmt;
    private Double baseImprove;
    private Double zipfImprove;
    private Double largeImprove;
    private Double large_zipfImprove;

    public StatementStatistic(String appName, int stmtId) {
      this.stmt = Statement.findOne(appName, stmtId);
    }

    public void updateProfile(double val, String workloadType) {
      switch (workloadType) {
        case BASE -> baseImprove = val;
        case ZIPF -> zipfImprove = val;
        case LARGE -> largeImprove = val;
        case LARGE_ZIPF -> large_zipfImprove = val;
      }
    }

    public String toString(boolean base, boolean zipf, boolean large, boolean largeZipf, double valid, double invalid) {
      if ((base && baseImprove != null && baseImprove < invalid)
              || (zipf && zipfImprove != null && zipfImprove < invalid)
              || (large && largeImprove != null && largeImprove < invalid)
              || (largeZipf && large_zipfImprove != null && large_zipfImprove < invalid)) {
        return null;
      }
      if (!((base && baseImprove != null && baseImprove > valid)
              || (zipf && zipfImprove != null && zipfImprove > valid)
              || (large && largeImprove != null && largeImprove > valid)
              || (largeZipf && large_zipfImprove != null && large_zipfImprove > valid))) {
        return null;
      }

      final StringBuilder sb = new StringBuilder();
      sb.append(String.format("%s\t%d\t%s", stmt.appName(), stmt.stmtId(), stmt.rewritten(optimizer).stackTrace()));
      return getImprovement(base, zipf, large, largeZipf, sb);
    }

    private String getImprovement(boolean base, boolean zipf, boolean large, boolean largeZipf, StringBuilder sb) {
      if (base) sb.append("\t%s".formatted(baseImprove));
      if (zipf) sb.append("\t%s".formatted(zipfImprove));
      if (large) sb.append("\t%s".formatted(largeImprove));
      if (largeZipf) sb.append("\t%s".formatted(large_zipfImprove));
      return sb.toString();
    }

    public String toString(boolean base, boolean zipf, boolean large, boolean largeZipf) {
      final StringBuilder sb = new StringBuilder(this.toString());
      return getImprovement(base, zipf, large, largeZipf, sb);
    }

    public String toString(String tag) {
      switch (tag){
        case BASE -> {
          return String.format("%s\t%d\t%s",
                  stmt.appName(),
                  stmt.stmtId(),
                  baseImprove);
        }
        case ZIPF ->
        {
          return String.format("%s\t%d\t%s",
                  stmt.appName(),
                  stmt.stmtId(),
                  zipfImprove);
        }
        case LARGE ->
        {
          return String.format("%s\t%d\t%s",
                  stmt.appName(),
                  stmt.stmtId(),
                  largeImprove);
        }
        case LARGE_ZIPF ->
        {
          return String.format("%s\t%d\t%s",
                  stmt.appName(),
                  stmt.stmtId(),
                  large_zipfImprove);
        }
      }
      return null;
    }

    private Double getImprovement(String tag) {
      switch (tag){
        case BASE -> { return baseImprove; }
        case ZIPF -> { return zipfImprove; }
        case LARGE -> { return largeImprove; }
        case LARGE_ZIPF -> { return large_zipfImprove; }
      }
      return 0.0;
    }

    @Override
    public String toString() {
      return String.format("%s\t%d\t%s\t%s\t%s",
              stmt.appName(),
              stmt.stmtId(),
              stmt.original().rawSql(),
              stmt.rewritten(optimizer).rawSql(),
              stmt.rewritten(optimizer).stackTrace());
    }
  }

  private class CalciteStatementStatistic {
    private final Statement stmt;
    // Only record base workload.
    private Double weTuneImprove;
    private Double calciteImprove;
    private Double weTuneImproveThanCalcite;

    public CalciteStatementStatistic(String appName, int stmtId) {
      this.stmt = Statement.findOneCalcite(appName, stmtId);
    }

    public void updateWeTuneImprove(double val) {
      this.weTuneImprove = val;
    }

    public void updateCalciteImprove(double val) {
      this.calciteImprove = val;
    }

    public void updateWeTuneImproveThanCalcite(double val) {
      this.weTuneImproveThanCalcite = val;
    }

    @Override
    public String toString() {
      return String.format("%s\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
              stmt.appName(),
              stmt.stmtId(),
              stmt.original().rawSql(),
              stmt.rewritten(OptimizerType.Calcite).rawSql(),
              stmt.rewritten(optimizer).rawSql(),
              stmt.rewritten(optimizer).stackTrace(),
              calciteImprove,
              weTuneImprove,
              weTuneImproveThanCalcite);
    }
  }

  private class AllStatementStatistic {
    private final Statement stmt;
    // Only record base workload.
    private Double improve;

    public AllStatementStatistic(String appName, int stmtId) {
      if (appName.equals("calcite_test")) this.stmt = Statement.findOneCalcite(appName, stmtId);
      else this.stmt = Statement.findOne(appName, stmtId);
    }

    public void updateImprove(double val) {
      this.improve = val;
    }

    @Override
    public String toString() {
      return String.format("%s\t%d\t%s\t%s\t%s\t%s",
              stmt.appName(),
              stmt.stmtId(),
              stmt.original().rawSql(),
              stmt.rewritten(optimizer).rawSql(),
              stmt.rewritten(optimizer).stackTrace(),
              improve);
    }
  }

  private class WorkLoadStatistic{
    private Double latencyReductionForOverFiftyPercentageQueries = 0.0;
    private Integer queriesNumberWithLatencyReductionOverNinety = 0;
    private Integer queriesNumber = 0;


    public void updateLatencyReductionForOverFiftyPercentageQueries(double val) {
      this.latencyReductionForOverFiftyPercentageQueries = val;
    }

    public void updateQueriesNumber(int val){
      this.queriesNumber = val;
    }

    public void updateQueriesNumberWithLatencyReductionOverNinety(int val) {
      this.queriesNumberWithLatencyReductionOverNinety = val;
    }
  }
}
