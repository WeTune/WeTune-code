package sjtu.ipads.wtune.rawlog;

import sjtu.ipads.wtune.rawlog.impl.TaggedTraceLogParser;

import java.util.List;

public interface TraceLogParser extends LogParser {
  List<StackTrace> stackTraces();

  static TraceLogParser forTaggedFormat() {
    return TaggedTraceLogParser.build();
  }
}
