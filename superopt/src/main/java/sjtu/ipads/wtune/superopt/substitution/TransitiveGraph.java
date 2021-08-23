package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;

import java.util.*;
import java.util.function.Consumer;

public class TransitiveGraph<V, E> {
  private final MutableValueGraph<V, E> graph;

  public TransitiveGraph(MutableValueGraph<V, E> graph) {
    this.graph = graph;
  }

  public MutableValueGraph<V, E> graph() {
    return graph;
  }

  public void breakTransitivity(Consumer<E> listener) {
    for (V node : graph.nodes()) {
      if (graph.successors(node).size() == 0) {
        final Set<V> predecessors = graph.predecessors(node);
        if (predecessors.isEmpty()) continue;

        final boolean isMandatory = predecessors.size() == 1;
        final Map<V, NodeDistance<V>> distances = new IdentityHashMap<>();
        final List<EndpointPair<V>> toRemoveEdges = new ArrayList<>();
        for (V predecessor : predecessors) {
          breakTransitivity0(predecessor, node, isMandatory, 1, distances, toRemoveEdges);
        }
        for (EndpointPair<V> toRemoveEdge : toRemoveEdges) {
          listener.accept(graph.removeEdge(toRemoveEdge));
        }
      }
    }
  }

  private void breakTransitivity0(
      V start,
      V end,
      boolean mandatory,
      int distance,
      Map<V, NodeDistance<V>> distances,
      List<EndpointPair<V>> toRemoveEdges) {
    final NodeDistance<V> knownDist = distances.get(start);

    if (knownDist != null) {
      if (distance < knownDist.distance) {
        toRemoveEdges.add(EndpointPair.ordered(start, end));
      } else if (distance > knownDist.distance) {
        if (knownDist.adjacent != end && !knownDist.mandatory)
          toRemoveEdges.add(EndpointPair.ordered(start, knownDist.adjacent));
        knownDist.adjacent = end;
        knownDist.distance = distance;
        knownDist.mandatory = mandatory;
      }
      return;
    }

    final NodeDistance<V> d = new NodeDistance<>();
    d.adjacent = end;
    d.distance = distance;
    d.mandatory = mandatory;
    distances.put(start, d);

    final Set<V> predecessors = graph.predecessors(start);
    boolean isMandatory = predecessors.size() == 1;
    for (V predecessor : predecessors)
      breakTransitivity0(predecessor, start, isMandatory, distance + 1, distances, toRemoveEdges);
  }

  private static class NodeDistance<E> {
    private E adjacent;
    private int distance;
    private boolean mandatory;
  }
}
