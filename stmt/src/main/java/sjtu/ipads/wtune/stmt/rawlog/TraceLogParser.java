package sjtu.ipads.wtune.stmt.rawlog;

import sjtu.ipads.wtune.stmt.rawlog.internal.TaggedTraceLogParser;

import java.util.List;

public interface TraceLogParser extends LogParser {
  List<StackTrace> stackTraces();

  static TraceLogParser forTaggedFormat() {
    return TaggedTraceLogParser.build();
  }
}
