package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.dao.TimingDao;

import static sjtu.ipads.wtune.stmt.Setup.CSV_SEP;

public class Timing {
  public static final String KEY_APP_NAME = "appName";
  public static final String KEY_STMT_ID = "stmtId";
  public static final String KEY_TAG = "tag";
  public static final String KEY_P50 = "p50";
  public static final String KEY_P90 = "p90";
  public static final String KEY_P99 = "p99";

  private long p50;
  private long p90;
  private long p99;
  private String tag;

  private String appName;
  private int stmtId;

  private Statement stmtImpl;

  public static Timing fromLine(String line) {
    final String[] fields = line.split(CSV_SEP);
    final Timing timing = new Timing();
    timing.setStmtId(Integer.parseInt(fields[0]));
    timing.setP50(Long.parseLong(fields[1]));
    timing.setP90(Long.parseLong(fields[2]));
    timing.setP99(Long.parseLong(fields[3]));
    return timing;
  }

  public long p50() {
    return p50;
  }

  public long p90() {
    return p90;
  }

  public long p99() {
    return p99;
  }

  public String tag() {
    return tag;
  }

  public String appName() {
    return appName;
  }

  public int stmtId() {
    return stmtId;
  }

  public Statement statement() {
    return stmtImpl;
  }

  public Timing setP50(long p50) {
    this.p50 = p50;
    return this;
  }

  public Timing setP90(long p90) {
    this.p90 = p90;
    return this;
  }

  public Timing setP99(long p99) {
    this.p99 = p99;
    return this;
  }

  public Timing setTag(String tag) {
    this.tag = tag;
    return this;
  }

  public Timing setAppName(String appName) {
    this.appName = appName;
    return this;
  }

  public Timing setStmtId(int stmtId) {
    this.stmtId = stmtId;
    return this;
  }

  public void save() {
    TimingDao.instance().save(this);
  }

  public void setStatement(Statement stmtImpl) {
    this.stmtImpl = stmtImpl;
  }
}
