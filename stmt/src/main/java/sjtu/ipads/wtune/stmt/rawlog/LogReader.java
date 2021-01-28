package sjtu.ipads.wtune.stmt.rawlog;

import sjtu.ipads.wtune.stmt.rawlog.internal.LogReaderImpl;

import java.util.List;

public interface LogReader {
  List<RawStmt> readFrom(Iterable<String> sqlLog, Iterable<String> traceLog);

  static LogReader forTaggedFormat() {
    return LogReaderImpl.build();
  }
}
