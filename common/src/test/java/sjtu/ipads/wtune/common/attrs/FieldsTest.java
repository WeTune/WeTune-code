package sjtu.ipads.wtune.common.attrs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FieldsTest {
  private static class Foo implements Fields {
    private final Map<FieldKey, Object> directAttrs = new HashMap<>();

    @Override
    public Map<FieldKey, Object> directAttrs() {
      return directAttrs;
    }
  }

  private static final FieldKey<String> ATTR_FIRST = FieldKey.make("wtune.first");
  private static final FieldKey<Integer> ATTR_SECOND = FieldKey.make("wtune.second");

  @Test
  @DisplayName("[common.attrs] attrs")
  public void test() {
    var foo = new Foo();

    assertNull(foo.get(ATTR_FIRST));

    foo.set(ATTR_FIRST, "123");
    assertEquals("123", foo.setIfAbsent(ATTR_FIRST, "456"));

    assertEquals(1, foo.setIfAbsent(ATTR_SECOND, 1));

    final Map<FieldKey, Object> oldAttrs = foo.directAttrs();

    foo = new Foo();
    assertNotSame(oldAttrs, foo.directAttrs());
    assertEquals(0, foo.directAttrs().size());
  }
}
