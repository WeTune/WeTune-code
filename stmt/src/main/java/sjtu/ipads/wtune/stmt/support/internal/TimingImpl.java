package sjtu.ipads.wtune.stmt.support.internal;

import sjtu.ipads.wtune.stmt.support.Timing;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.utils.FileUtils.CSV_SEP;

public record TimingImpl(String app, int stmtId, String tag, long p50, long p90, long p99)
    implements Timing {
  public static List<Timing> fromLines(String appName, String tag, Iterable<String> lines) {
    return listMap(it -> fromLine(appName, tag, it), lines);
  }

  private static Timing fromLine(String appName, String tag, String line) {
    final String[] fields = line.split(CSV_SEP);
    final int stmtId = Integer.parseInt(fields[0]);
    final long p50 = Long.parseLong(fields[1]);
    final long p90 = Long.parseLong(fields[2]);
    final long p99 = Long.parseLong(fields[3]);
    return new TimingImpl(appName, stmtId, tag, p50, p90, p99);
  }
}
