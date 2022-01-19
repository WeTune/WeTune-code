package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MiscTest {
  @Test
  void testOrdinal() {
    int total = 30;
    int ordinal = 0;
    for (int i = 0; i < total; ++i) {
      for (int j = i; j < total; ++j) {
        assertEquals(ordinal, ordinal(total, i, j), i + "," + j);
        ++ordinal;
      }
    }
  }

  private int ordinal(int total, int i, int j) {
    assert i <= j;
    return ((total * 2) - i + 1) * i / 2 + j - i;
  }
}
