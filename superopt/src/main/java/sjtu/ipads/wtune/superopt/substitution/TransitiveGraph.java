package sjtu.ipads.wtune.superopt.substitution;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

import java.io.IOException;
import java.nio.file.Path;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.loadBank;

class TransitiveGraph {
  private final SubstitutionBank bank;
  private final MutableValueGraph<FragmentProbe, Substitution> graph;

  TransitiveGraph(SubstitutionBank bank) {
    this.bank = bank;
    this.graph =
        ValueGraphBuilder.directed()
            .expectedNodeCount(bank.size() << 2)
            .allowsSelfLoops(false)
            .build();

    buildGraph();
  }

  private void buildGraph() {
    for (Substitution sub : bank) {
      final FragmentProbe lhs = sub.probe(true), rhs = sub.probe(false);
      graph.addNode(lhs);
      graph.addNode(rhs);
      graph.putEdgeValue(lhs, rhs, sub);
    }
  }

  private int countNodeWithMultipleOut() {
    int count = 0;
    for (FragmentProbe node : graph.nodes()) {
      if (graph.successors(node).size() == 0) {
        ++count;
      }
    }
    return count;
  }

  public static void main(String[] args) throws IOException {
    final SubstitutionBank bank = loadBank(Path.of("wtune_data", "substitutions.filtered"));
    final TransitiveGraph graph = new TransitiveGraph(bank);
    System.out.println(graph.countNodeWithMultipleOut());
  }
}
