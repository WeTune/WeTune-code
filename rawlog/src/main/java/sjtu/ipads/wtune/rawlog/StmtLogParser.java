package sjtu.ipads.wtune.rawlog;

import sjtu.ipads.wtune.rawlog.impl.TaggedStmtLogParser;

import java.util.List;

public interface StmtLogParser extends LogParser {
  List<RawStmt> stmts();

  static StmtLogParser forTaggedFormat() {
    return TaggedStmtLogParser.build();
  }
}
