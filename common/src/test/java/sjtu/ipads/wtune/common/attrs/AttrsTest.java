package sjtu.ipads.wtune.common.attrs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.common.memory.AutoReclaimRegion;
import sjtu.ipads.wtune.common.memory.ReclaimWorker;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AttrsTest {
  private static class Foo implements Attrs {
    private final Map<String, Object> directAttrs = new HashMap<>();

    @Override
    public Map<String, Object> directAttrs() {
      return directAttrs;
    }
  }

  private static final Attrs.Key<String> ATTR_FIRST = Attrs.Key.of("wtune.first", String.class);
  private static final Attrs.Key<Integer> ATTR_SECOND = Attrs.Key.of("wtune.second", Integer.class);
  private static final Attrs.Key<List<Integer>> ATTR_THIRD =
      Attrs.Key.of2("wtune.third", List.class);

  @Test
  @DisplayName("[common.attrs] attrs")
  public void test() {
    var foo = new Foo();

    assertNull(foo.get(ATTR_FIRST));

    foo.put(ATTR_FIRST, "123");
    assertEquals("123", foo.putIfAbsent(ATTR_FIRST, "456"));

    assertEquals(1, foo.putIfAbsent(ATTR_SECOND, 1));

    assertEquals("{ \"wtune.first\" = 123, \"wtune.second\" = 1 }", foo.stringify(true));
    assertEquals(2, foo.ofPrefix("wtune").size());

    final Map<String, Object> oldAttrs = foo.directAttrs();

    foo = new Foo();
    assertNotSame(oldAttrs, foo.directAttrs());
    assertEquals(0, foo.directAttrs().size());
  }
}
