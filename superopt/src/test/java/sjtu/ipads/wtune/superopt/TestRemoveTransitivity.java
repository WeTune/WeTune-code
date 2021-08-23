package sjtu.ipads.wtune.superopt;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.substitution.TransitiveGraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("optimizer")
@Tag("fast")
public class TestRemoveTransitivity {
  @Test
  void test0() {
    final MutableValueGraph<Integer, Integer> g =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    final Set<Integer> edgeSet = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5));
    g.putEdgeValue(0, 1, 0);
    g.putEdgeValue(1, 2, 1);
    g.putEdgeValue(2, 3, 2);
    g.putEdgeValue(3, 0, 3);
    g.putEdgeValue(0, 4, 4);
    g.putEdgeValue(1, 3, 5);
    final TransitiveGraph<Integer, Integer> graph = new TransitiveGraph<>(g);
    graph.breakTransitivity(edgeSet::remove);
    assertEquals(5, edgeSet.size());
    assertFalse(edgeSet.contains(5));
  }

  @Test
  void test1() {
    final MutableValueGraph<Integer, Integer> g =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    final Set<Integer> edgeSet = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4));
    g.putEdgeValue(0, 1, 0);
    g.putEdgeValue(1, 2, 1);
    g.putEdgeValue(2, 3, 2);
    g.putEdgeValue(1, 3, 3);
    g.putEdgeValue(3, 4, 4);
    final TransitiveGraph<Integer, Integer> graph = new TransitiveGraph<>(g);
    graph.breakTransitivity(edgeSet::remove);
    assertEquals(4, edgeSet.size());
    assertFalse(edgeSet.contains(3));
  }

  @Test
  void test2() {
    final MutableValueGraph<Integer, Integer> g =
        ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    final Set<Integer> edgeSet = new HashSet<>(Arrays.asList(0, 1, 2, 3));
    g.putEdgeValue(0, 1, 0);
    g.putEdgeValue(1, 2, 1);
    g.putEdgeValue(2, 1, 2);
    g.putEdgeValue(2, 3, 3);
    final TransitiveGraph<Integer, Integer> graph = new TransitiveGraph<>(g);
    graph.breakTransitivity(edgeSet::remove);
    assertEquals(4, edgeSet.size());
  }
}
