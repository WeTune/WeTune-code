package sjtu.ipads.wtune.stmt.rawlog;

import sjtu.ipads.wtune.stmt.rawlog.internal.TaggedStmtLogParser;

import java.util.List;

public interface StmtLogParser extends LogParser {
  List<RawStmt> stmts();

  static StmtLogParser forTaggedFormat() {
    return TaggedStmtLogParser.build();
  }
}
