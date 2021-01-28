package sjtu.ipads.wtune.stmt.rawlog.internal;

import sjtu.ipads.wtune.stmt.rawlog.RawStmt;
import sjtu.ipads.wtune.stmt.rawlog.StmtLogParser;

import java.util.ArrayList;
import java.util.List;

public class TaggedStmtLogParser implements StmtLogParser {
  private final List<RawStmt> rawStmts = new ArrayList<>();

  private TaggedStmtLogParser() {}

  public static StmtLogParser build() {
    return new TaggedStmtLogParser();
  }

  @Override
  public void accept(String line) {
    final String[] split = line.split(" ", 2);
    final int id = Integer.parseInt(split[0]);
    final String sql = split[1];
    rawStmts.add(new RawStmt(id, sql));
  }

  @Override
  public List<RawStmt> stmts() {
    return rawStmts;
  }
}
