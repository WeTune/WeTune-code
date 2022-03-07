package wtune.testbed;

import wtune.testbed.plantree.PlanTree;
import wtune.testbed.plantree.PlanTreeNode;
import wtune.testbed.util.DataSourceSupport;
import wtune.testbed.util.StmtSyntaxRewriteHelper;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class PlanMain2 {
    private static final String ROOT_PATH = "wtune_data/plan/";

    private static final String ORIGIN_STMTS_PATH = "wtune_data/plan/origin_stmts/";
    private static final String PLAN_BASE_PATH = "wtune_data/plan/plan_base/";
    private static final String PLAN_OPT_PATH = "wtune_data/plan/plan_opt/";

    private static final String SAME_PLAN_FILE_PATH = "wtune_data/plan/samePlanStmt.csv";

    private static BufferedReader stmtReader;
    private static BufferedWriter stmtPlanWriter;  //write query plan struct info

    private static BufferedReader stmtPlanReader;
    private static BufferedWriter resultWriter;    //write rewritten stmts info

    private static Connection conn;

    private static final String SHOW_PLAN_ON_CMD = "SET SHOWPLAN_ALL ON";
    private static final String SHOW_PLAN_OFF_CMD = "SET SHOWPLAN_ALL OFF";

    private static final String BASE = "base";
    private static final String ZIPF = "zipf";
    private static final String LARGE = "large";
    private static final String LARGE_ZIPF = "large_zipf";

    private static void writeLine(BufferedWriter bw, String s) throws IOException{
        bw.write(s);
        bw.newLine();
        bw.flush();
    }

    private static void getConnAndShowPlanOn(String app, String tag) throws SQLException{
        String db = app + "_" + tag;
        DataSource dataSource = DataSourceSupport.makeDataSource(DataSourceSupport.sqlserverProps(db));
        conn = dataSource.getConnection();

        // SET SHOWPLAN_ALL ON
        Statement statement = conn.createStatement();
        statement.execute(SHOW_PLAN_ON_CMD);
    }

    private static void recordStmtPlan(String stmtId, String stmt, String tag, String mode) throws IOException, SQLException{
        File outFile = Paths.get(System.getProperty("user.dir"),
                "base".equals(mode) ? PLAN_BASE_PATH : PLAN_OPT_PATH, tag, stmtId + ".csv").toFile();
        stmtPlanWriter = new BufferedWriter(new FileWriter(outFile));

        Statement statement = conn.createStatement();
        ResultSet res = statement.executeQuery(stmt);

        while(res.next()){
            writeLine(stmtPlanWriter, String.join(";",
                    res.getString("StmtText"),
                    res.getString("NodeId"),
                    res.getString("Parent"),
                    res.getString("PhysicalOp"),
                    res.getString("LogicalOp"),
                    res.getString("Argument"),
                    res.getString("TotalSubtreeCost")));
        }
        stmtPlanWriter.close();
    }

    public static void main(String[] args) throws IOException, SQLException {
//        System.setProperty("user.dir", Paths.get(System.getProperty("user.dir"), "../").normalize().toString());
        for(String oneTag: List.of(LARGE_ZIPF)){
            String currentApp = "";
            File stmtFile = Paths.get(System.getProperty("user.dir"), ORIGIN_STMTS_PATH, oneTag + ".csv").toFile();
            stmtReader = new BufferedReader(new FileReader(stmtFile));
            String line1, line2;
            String stmtId, app, baseStmt, optStmt; // e.g. broadleaf-199
            while((line1 = stmtReader.readLine()) != null && (line2 = stmtReader.readLine()) != null){
                String[] info1 = line1.split(";"); String[] info2 = line2.split(";");
                stmtId = info1[0].split("\\.")[0];
                app = stmtId.split("-")[0];
                baseStmt = StmtSyntaxRewriteHelper.regexRewriteForSQLServer(info1[1]); optStmt = StmtSyntaxRewriteHelper.regexRewriteForSQLServer(info2[1]);

                System.out.println("executing sql: " + stmtId + " \tat workload " + oneTag);
                if(!currentApp.equals(app)){
                    getConnAndShowPlanOn(app, oneTag);
                    currentApp = app;
                }

                recordStmtPlan(stmtId, baseStmt, oneTag, "base");
                recordStmtPlan(stmtId, optStmt, oneTag, "opt");
            }

            stmtReader.close();

            analysePlan(oneTag);
        }
    }

    private static void analysePlan(String tag) throws IOException{
        resultWriter = new BufferedWriter(
                new FileWriter(Paths.get(System.getProperty("user.dir"), ROOT_PATH, "costEstimation_" + tag + ".csv").toFile()));
        Path basePath = Paths.get(System.getProperty("user.dir"), PLAN_BASE_PATH, tag);
        Path optPath = Paths.get(System.getProperty("user.dir"), PLAN_OPT_PATH, tag);

        List<File> files = Files.list(basePath).map(Path::toFile).collect(Collectors.toList());
        for (File f: files) {
            String fileName = f.getName(); //e.g. broadleaf-119.csv
            System.out.println("Analyzing " + fileName);

            String[] tags = fileName.substring(0, fileName.indexOf(".")).split("-");
            PlanTree basePlanTree = constructPlanTree(basePath.resolve(fileName).toFile(), tags[0], Integer.parseInt(tags[1]));
            PlanTree optPlanTree = constructPlanTree(optPath.resolve(fileName).toFile(), tags[0], Integer.parseInt(tags[1]));

//            String result = PlanTree.samePlan(basePlanTree, optPlanTree) ? "same" : "diff";
            String result = basePlanTree.moreCostThan(optPlanTree) ? "base" : "opt";
            writeLine(resultWriter, String.join(",", fileName.substring(0, fileName.indexOf(".")), result));
        }
        resultWriter.close();
    }

    private static PlanTree constructPlanTree(File file, String appName, int stmtId) throws IOException{
        stmtPlanReader = new BufferedReader(new FileReader(file));
        String oneLine;
        PlanTree planTree = new PlanTree(appName, stmtId);
        while((oneLine = stmtPlanReader.readLine()) != null){
            String[] fields = oneLine.split(";");
            planTree.insertNode(
                    new PlanTreeNode(fields[0], Integer.parseInt(fields[1]), fields[4], Double.parseDouble(fields[6]))
                    , Integer.parseInt(fields[2]));
        }
        stmtPlanReader.close();
        return planTree;
    }
}
