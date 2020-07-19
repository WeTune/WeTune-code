package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.analyzer.Analyzer;
import sjtu.ipads.wtune.stmt.analyzer.RelationGraphAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.dao.internal.AltStatementDaoInstance;
import sjtu.ipads.wtune.stmt.dao.internal.StatementDaoInstance;
import sjtu.ipads.wtune.stmt.dao.internal.TimingDaoInstance;
import sjtu.ipads.wtune.stmt.mutator.Mutator;
import sjtu.ipads.wtune.stmt.resolver.Resolver;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.utils.StmtHelper.newInstance;

public class Statement {
  public static final String KEY_APP_NAME = "appName";
  public static final String KEY_STMT_ID = "stmtId";
  public static final String KEY_RAW_SQL = "rawSql";

  public static final String TAG_BASE = "base";
  public static final String TAG_OPT = "opt";
  public static final String TAG_INDEX = "index";

  private String appName;
  private int stmtId;
  protected String rawSql;

  protected AppContext appContext;
  protected SQLNode parsed;
  protected RelationGraph relationGraph;

  protected Set<Class<? extends Resolver>> resolvedBy = new HashSet<>();
  protected Set<Class<? extends Resolver>> failToResolveBy = new HashSet<>();

  private List<AltStatement> alt;
  private List<Timing> timing;

  public static Statement findOne(String appName, int id) {
    return StatementDaoInstance.findOne(appName, id);
  }

  public static List<Statement> findByApp(String appName) {
    return StatementDaoInstance.findByApp(appName);
  }

  public static List<Statement> findAll() {
    return StatementDaoInstance.findAll();
  }

  public Statement() {
    stmtId = -1;
  }

  public String appName() {
    return appName;
  }

  public int stmtId() {
    return stmtId;
  }

  public String rawSql() {
    return rawSql;
  }

  public SQLNode parsed() {
    if (parsed == null) {
      final SQLParser parser = SQLParser.ofDb(appContext().dbType());
      if (parser != null) parsed = parser.parse(rawSql);
    }
    return parsed;
  }

  public AppContext appContext() {
    if (appContext == null) appContext = AppContext.of(appName);
    return appContext;
  }

  public Statement alt(String kind) {
    if (alt == null) alt = AltStatementDaoInstance.findByStmt(this);

    return FuncUtils.find(it -> kind.equals(it.kind()), alt);
  }

  public Timing timing(String tag) {
    if (timing == null) timing = TimingDaoInstance.findByStmt(this);
    return FuncUtils.find(it -> tag.equals(it.tag()), timing);
  }

  public boolean resolve(Class<? extends Resolver> cls, boolean force) {
    if (!force && resolvedBy.contains(cls)) return true;

    final Resolver resolver = Resolver.getResolver(cls);

    for (Class<? extends Resolver> dependency : resolver.dependsOn()) resolve(dependency, force);
    final boolean isSuccessful = resolver.resolve(this);
    if (isSuccessful) {
      resolvedBy.add(cls);
      failToResolveBy.remove(cls);
    } else failToResolveBy.add(cls);
    return isSuccessful;
  }

  public boolean resolve(Class<? extends Resolver> cls) {
    return resolve(cls, false);
  }

  public boolean reResolve() {
    final Set<Class<? extends Resolver>> resolvedBy = this.resolvedBy;
    this.resolvedBy = new HashSet<>();
    this.failToResolveBy.clear();

    boolean isAllSuccessful = true;
    for (Class<? extends Resolver> cls : resolvedBy)
      isAllSuccessful = resolve(cls) && isAllSuccessful;
    return isAllSuccessful;
  }

  public boolean resolveStandard() {
    boolean isAllSuccessful = true;
    for (Class<? extends Resolver> cls : Resolver.STANDARD_RESOLVERS)
      isAllSuccessful = resolve(cls) && isAllSuccessful;
    return isAllSuccessful;
  }

  public void mutateStandard() {
    Mutator.STANDARD_MUTATORS.forEach(this::mutate);
  }

  public void retrofitStandard() {
    mutateStandard();
    resolveStandard();
  }

  public Set<Class<? extends Resolver>> failedResolvers() {
    return failToResolveBy;
  }

  public void mutate(Class<? extends Mutator> cls) {
    final Mutator mutator = newInstance(cls);
    for (Class<? extends Resolver> dependency : mutator.dependsOnResolver()) resolve(dependency);
    mutator.mutate(this);
  }

  public <T> T analyze(Class<? extends Analyzer<T>> cls, Object... args) {
    final Analyzer<T> analyzer = newInstance(cls);
    for (Class<? extends Resolver> dependency : analyzer.dependsOn()) resolve(dependency);
    analyzer.setParam(args);
    return analyzer.analyze(this);
  }

  public RelationGraph relationGraph(boolean forceResolve) {
    if (relationGraph == null || forceResolve) relationGraph = analyze(RelationGraphAnalyzer.class);
    return relationGraph;
  }

  public RelationGraph relationGraph() {
    return relationGraph(false);
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  public void setRawSql(String rawSql) {
    this.rawSql = rawSql;
    this.parsed = null;
    this.resolvedBy.clear();
  }

  public void setParsed(SQLNode parsed) {
    this.parsed = parsed;
  }

  public Statement registerToApp() {
    appContext().addStatement(this);
    return this;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Statement statement = (Statement) o;
    return stmtId == statement.stmtId && Objects.equals(appName, statement.appName);
  }

  public Statement copy() {
    final Statement copy = new Statement();
    copy.appName = this.appName;
    copy.stmtId = this.stmtId;
    copy.rawSql = this.rawSql;
    copy.appContext = this.appContext;
    copy.parsed = this.parsed.copy();
    copy.resolvedBy = new HashSet<>(resolvedBy);
    copy.failToResolveBy = new HashSet<>(failToResolveBy);
    copy.reResolve();
    return copy;
  }

  public void delete(String cause) {
    StatementDaoInstance.delete(this, cause);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appName, stmtId);
  }

  @Override
  public String toString() {
    return "<" + appName + ", " + stmtId + ">";
  }
}
