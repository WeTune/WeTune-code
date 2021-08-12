package sjtu.ipads.wtune.testbed;

import sjtu.ipads.wtune.testbed.plantree.PlanTree;
import sjtu.ipads.wtune.testbed.plantree.PlanTreeNode;

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
import java.util.Set;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.testbed.util.DataSourceHelper.makeDataSource;
import static sjtu.ipads.wtune.testbed.util.DataSourceHelper.sqlserverProps;

public class PlanMain2 {

    private static final String PLAN_BASE_PATH = "wtune_data/plan/plan_base/";
    private static final String PLAN_OPT_PATH = "wtune_data/plan/plan_opt/";
    private static final String STMTS_BASE_PATH = "wtune_data/plan/stmts_base/";
    private static final String STMTS_OPT_PATH = "wtune_data/plan/stmts_opt";
    private static final String DIFF_PLAN_FILE_PATH = "wtune_data/plan/diffPlanStmt.csv";
    private static final String SAME_PLAN_FILE_PATH = "wtune_data/plan/samePlanStmt.csv";

    private static BufferedReader stmtReader;
    private static BufferedWriter stmtPlanWriter;  //write query plan struct info

    private static BufferedReader stmtPlanReader;
    private static BufferedWriter resultWriter;    //write rewritten stmts info

    private static Connection conn;

    private static void writeLine(BufferedWriter bw, String s) throws IOException{
        bw.write(s);
        bw.newLine();
        bw.flush();
    }

    public static void main(String[] args) throws IOException, SQLException {
        System.setProperty("user.dir", "D:\\study\\WeTune\\wtune-code\\");

        List<File> files = Files.list(Paths.get(System.getProperty("user.dir"), STMTS_BASE_PATH))
                .map(Path::toFile).collect(Collectors.toList());

        for(File file: files){
            String appName = file.getName().substring(0, file.getName().indexOf("."));
            runStmtsOfApp(appName);
        }
        analysePlan();
    }

    private static final Set<String> BLACK_LIST = Set.of("lobsters-118");

    private static void runStmtsOfApp(String appName) throws IOException, SQLException {
        System.out.println("run base and opt stmts of " + appName);
        // Get connection
        String db = appName + "_base";
        DataSource dataSource = makeDataSource(sqlserverProps(db));
        conn = dataSource.getConnection();

        // SET SHOWPLAN_ALL ON
        Statement statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        Boolean cmdRes = statement.execute("SET SHOWPLAN_ALL ON");
        // Run base and opt stmts
        File baseFile = Paths.get(System.getProperty("user.dir"), STMTS_BASE_PATH).resolve(appName + ".csv").toFile();
        File optFile = Paths.get(System.getProperty("user.dir"), STMTS_OPT_PATH).resolve(appName + ".csv").toFile();

        run(baseFile, "base");
        run(optFile, "opt");

        conn.close();
    }

    private static void run(File stmtFile, String mode) throws IOException, SQLException{
        stmtReader = new BufferedReader(new FileReader(stmtFile));
        String oneLine, tag, stmt;
        Statement statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        //read one stmt and run it, record its query plan to a single file
        while((oneLine = stmtReader.readLine()) != null){
            String[] info = oneLine.split(";");
            tag = info[0]; stmt = info[1]; //tag: broadleaf-119
            if(BLACK_LIST.contains(tag)) continue;

            String outFilePath = System.getProperty("user.dir")
                    + (mode.equals("base") ? PLAN_BASE_PATH : PLAN_OPT_PATH) + tag + ".csv";
            stmtPlanWriter = new BufferedWriter(new FileWriter(outFilePath));
            ResultSet res = statement.executeQuery(stmt);
            while(res.next()){
                writeLine(stmtPlanWriter, String.join(";",
                        res.getString("StmtText"),
                        res.getString("NodeId"),
                        res.getString("Parent"),
                        res.getString("PhysicalOp"),
                        res.getString("LogicalOp"),
                        res.getString("Argument")));
            }
            stmtPlanWriter.close();
        }
        stmtReader.close();
    }

    private static void analysePlan() throws IOException{
        resultWriter = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + SAME_PLAN_FILE_PATH));
        String basePath = System.getProperty("user.dir") + PLAN_BASE_PATH;
        String optPath = System.getProperty("user.dir") + PLAN_OPT_PATH;

        List<File> files = Files.list(Paths.get(basePath)).map(Path::toFile).collect(Collectors.toList());
        for (File f: files) {
            String fileName = f.getName(); //e.g. broadleaf-119.csv
            System.out.println("Analyzing " + fileName);

            String[] tags = fileName.substring(0, fileName.indexOf(".")).split("-");
            PlanTree offPlanTree = constructPlanTree(basePath + fileName, tags[0], Integer.parseInt(tags[1]));
            PlanTree onPlanTree = constructPlanTree(optPath + fileName, tags[0], Integer.parseInt(tags[1]));
            if(PlanTree.samePlan(offPlanTree, onPlanTree)){
                writeLine(resultWriter, fileName.substring(0, fileName.indexOf(".")));
            }
        }
        resultWriter.close();
    }

    private static PlanTree constructPlanTree(String filePath, String appName, int stmtId) throws IOException{
        stmtPlanReader = new BufferedReader(new FileReader(filePath));
        String oneLine;
        PlanTree planTree = new PlanTree(appName, stmtId);
        while((oneLine = stmtPlanReader.readLine()) != null){
            String[] fields = oneLine.split(";");
            planTree.insertNode(new PlanTreeNode
                    (fields[0], Integer.parseInt(fields[1]), fields[4]), Integer.parseInt(fields[2]));
        }
        stmtPlanReader.close();
        return planTree;
    }
}
