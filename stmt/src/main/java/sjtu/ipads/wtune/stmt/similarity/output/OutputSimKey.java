package sjtu.ipads.wtune.stmt.similarity.output;

import java.util.Arrays;

public class OutputSimKey {
  private final int[] columnHashes;

  public OutputSimKey(int[] columnHashes) {
    this.columnHashes = columnHashes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OutputSimKey that = (OutputSimKey) o;
    return Arrays.equals(columnHashes, that.columnHashes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(columnHashes);
  }
}
