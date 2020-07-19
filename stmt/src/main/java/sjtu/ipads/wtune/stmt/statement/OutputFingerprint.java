package sjtu.ipads.wtune.stmt.statement;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.internal.FingerprintDaoInstance;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.stmt.Setup.CSV_SEP;

public class OutputFingerprint {
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
      fingerprint.point = Integer.parseInt(fields[0]);
      fingerprint.stmtId = Integer.parseInt(fields[1]);

      final int rows = Integer.parseInt(fields[2]);
      if (rows == 0) {
        fingerprint.hashes = Collections.emptyList();
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

      final List<Integer> hashes = new ArrayList<>();
      for (int i = 0; i < maxColumns; i++) {
        final String[] columnValue = new String[rows];
        for (int j = 0; j < rows; j++) columnValue[j] = safeGet(result.get(j), i);
        hashes.add(Arrays.hashCode(columnValue));
      }

      fingerprint.hashes = hashes;
      return fingerprint;
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  private static String safeGet(String[] arr, int idx) {
    if (idx >= arr.length) return null;
    return arr[idx];
  }

  public void save() {
    FingerprintDaoInstance.save(this);
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

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public void setStmtId(int stmtId) {
    this.stmtId = stmtId;
  }

  public void setPoint(int point) {
    this.point = point;
  }

  public void setHashes(List<Integer> hashes) {
    this.hashes = hashes;
  }
}
