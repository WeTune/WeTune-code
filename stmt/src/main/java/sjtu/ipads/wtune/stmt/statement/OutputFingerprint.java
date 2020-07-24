package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.FingerprintDao;
import sjtu.ipads.wtune.stmt.similarity.output.OutputSimKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.safeGet;
import static sjtu.ipads.wtune.stmt.Setup.CSV_SEP;

public class OutputFingerprint {
  private static final OutputSimKey[] EMPTY_ARRAY = new OutputSimKey[0];
  private String appName;
  private int stmtId;
  private int point;
  private List<Integer> hashes;

  public static OutputFingerprint readNext(BufferedReader reader) {
    try {
      String line = reader.readLine();
      if (line == null) return null;

      final OutputFingerprint fingerprint = new OutputFingerprint();

      final String[] fields = line.split(CSV_SEP);
      fingerprint.point = Integer.parseInt(fields[0].substring(1));
      fingerprint.stmtId = Integer.parseInt(fields[1]);

      final int rows = Integer.parseInt(fields[2]);
      if (rows == 0) {
        reader.readLine(); // skip an empty line
        fingerprint.hashes = Collections.singletonList(0);
        return fingerprint;
      }

      final List<String[]> result = new ArrayList<>(rows);
      int maxColumns = -1;
      for (int i = 0; i < rows; i++) {
        final String[] split = reader.readLine().split("\\|");
        final int numColumns = Math.max(maxColumns, split.length);
        if (maxColumns != -1 && numColumns != maxColumns) {
          System.out.println("malformed output");
        }
        maxColumns = Math.max(numColumns, maxColumns);
        result.add(split);
      }

      final List<Integer> hashes = new ArrayList<>(5);
      for (int i = 0; i < maxColumns; i++) {
        final String[] columnValue = new String[rows];
        for (int j = 0; j < rows; j++)
          columnValue[j] = Commons.safeGet(result.get(j), i).orElse(null);
        hashes.add(Arrays.hashCode(columnValue));
      }

      fingerprint.hashes = hashes;
      return fingerprint;
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  public static OutputSimKey[] extractKey(Statement stmt) {
    final List<OutputFingerprint> fingerprints = stmt.fingerprints();
    final int numColumns =
        fingerprints.stream().map(OutputFingerprint::hashes).mapToInt(List::size).max().orElse(0);
    if (numColumns == 0) return EMPTY_ARRAY; // shouldn't reach

    final OutputSimKey[] keys = new OutputSimKey[numColumns];

    for (int i = 0; i < numColumns; i++) {
      final int[] columnHashes = new int[fingerprints.size()];
      for (int j = 0; j < fingerprints.size(); j++)
        columnHashes[j] = safeGet(fingerprints.get(j).hashes(), i).orElse(0);
      keys[i] = new OutputSimKey(columnHashes);
    }

    return keys;
  }

  public void save() {
    FingerprintDao.instance().save(this);
  }

  public String appName() {
    return appName;
  }

  public int stmtId() {
    return stmtId;
  }

  public int point() {
    return point;
  }

  public List<Integer> hashes() {
    return hashes;
  }

  public OutputFingerprint setAppName(String appName) {
    this.appName = appName;
    return this;
  }

  public OutputFingerprint setStmtId(int stmtId) {
    this.stmtId = stmtId;
    return this;
  }

  public OutputFingerprint setPoint(int point) {
    this.point = point;
    return this;
  }

  public OutputFingerprint setHashes(List<Integer> hashes) {
    this.hashes = hashes;
    return this;
  }
}
