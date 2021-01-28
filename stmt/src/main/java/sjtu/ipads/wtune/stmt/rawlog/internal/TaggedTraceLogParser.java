package sjtu.ipads.wtune.stmt.rawlog.internal;

import sjtu.ipads.wtune.stmt.rawlog.StackTrace;
import sjtu.ipads.wtune.stmt.rawlog.TraceLogParser;

import java.util.ArrayList;
import java.util.List;

public class TaggedTraceLogParser implements TraceLogParser {
  private final List<StackTrace> stackTraces = new ArrayList<>();
  private StackTrace stackTrace;

  private TaggedTraceLogParser() {}

  public static TraceLogParser build() {
    return new TaggedTraceLogParser();
  }

  @Override
  public void accept(String line) {
    if (line.startsWith("  ")) {
      if (line.equals("  ...")) stackTrace.segment();
      else stackTrace.addFrame(line);
    } else {
      final int id = Integer.parseInt(line);
      stackTraces.add(stackTrace = new StackTrace(id));
    }
  }

  @Override
  public List<StackTrace> stackTraces() {
    return stackTraces;
  }
}
