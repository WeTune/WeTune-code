package sjtu.ipads.wtune.stmt.support;

import sjtu.ipads.wtune.stmt.support.internal.TimingImpl;

import java.util.List;

public interface Timing {
  String app();

  int stmtId();

  String tag();

  long p50();

  long p90();

  long p99();

  static List<Timing> fromLines(String appName, String tag, Iterable<String> lines) {
    return TimingImpl.fromLines(appName, tag, lines);
  }
}