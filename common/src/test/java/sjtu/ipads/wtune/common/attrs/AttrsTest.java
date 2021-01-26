package sjtu.ipads.wtune.common.attrs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AttrsTest {
  private static class Foo implements Attrs {
    private final Map<AttrKey, Object> directAttrs = new HashMap<>();

    @Override
    public Map<AttrKey, Object> directAttrs() {
      return directAttrs;
    }
  }

  private static final AttrKey<String> ATTR_FIRST = AttrKey.make("wtune.first");
  private static final AttrKey<Integer> ATTR_SECOND = AttrKey.make("wtune.second");

  @Test
  @DisplayName("[common.attrs] attrs")
  public void test() {
    var foo = new Foo();

    assertNull(foo.get(ATTR_FIRST));

    foo.set(ATTR_FIRST, "123");
    assertEquals("123", foo.setIfAbsent(ATTR_FIRST, "456"));

    assertEquals(1, foo.setIfAbsent(ATTR_SECOND, 1));

    final Map<AttrKey, Object> oldAttrs = foo.directAttrs();

    foo = new Foo();
    assertNotSame(oldAttrs, foo.directAttrs());
    assertEquals(0, foo.directAttrs().size());
  }
}
