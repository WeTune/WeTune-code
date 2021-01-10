package sjtu.ipads.wtune.rawlog;

import sjtu.ipads.wtune.rawlog.impl.LogReaderImpl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface LogReader {
  List<RawStmt> readFrom(Path sqlLog, Path traceLog) throws IOException;

  static LogReader forTaggedFormat() {
    return LogReaderImpl.build();
  }
}
