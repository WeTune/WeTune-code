package sjtu.ipads.wtune.sqlparser.plan1;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.strategy.IdentityHashingStrategy;
import sjtu.ipads.wtune.common.tree.UniformTreeContextBase;
import sjtu.ipads.wtune.common.utils.COW;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import static sjtu.ipads.wtune.common.tree.TreeSupport.checkNodePresent;

class PlanContextImpl extends UniformTreeContextBase<PlanKind> implements PlanContext {
  private final Schema schema;
  private final COW<TObjectIntMap<PlanNode>> nodeReg;
  private final ValuesRegistryImpl valuesReg;

  protected PlanContextImpl(int expectedNumNodes, Schema schema) {
    super(new PlanNd[(expectedNumNodes <= 0 ? 16 : expectedNumNodes) + 1], 2);
    this.schema = schema;
    this.nodeReg = new COW<>(mkIdentityMap(), null);
    this.valuesReg = new ValuesRegistryImpl(this);
  }

  private PlanContextImpl(PlanContextImpl other) {
    super(copyNodesArray((PlanNd[]) other.nodes), 2);
    this.schema = other.schema;
    this.nodeReg = new COW<>(other.nodeReg.forRead(), PlanContextImpl::copyIdentityMap);
    this.valuesReg = other.valuesReg.copy();
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public PlanNode nodeAt(int id) {
    checkNodePresent(this, id);
    return ((PlanNd) nodes[id]).planNode;
  }

  @Override
  public int nodeIdOf(PlanNode node) {
    return nodeReg.forRead().get(node);
  }

  @Override
  public int bindNode(PlanNode node) {
    final int newNodeId = mkNode(node.kind());
    ((PlanNd) nodes[newNodeId]).planNode = node;
    nodeReg.forWrite().put(node, newNodeId);
    return newNodeId;
  }

  @Override
  public void detachNode(int nodeId) {
    nodeReg.forWrite().remove(nodeAt(nodeId));
    super.detachNode(nodeId);
  }

  @Override
  protected void reNumber(int from, int to) {
    nodeReg.forWrite().put(nodeAt(from), to);
    valuesReg.reNumberNode(from, to);
    super.reNumber(from, to);
  }

  @Override
  public ValuesRegistry valuesReg() {
    return valuesReg;
  }

  @Override
  public PlanContext copy() {
    return new PlanContextImpl(this);
  }

  @Override
  protected Nd<PlanKind> mk(PlanKind planKind) {
    return new PlanNd(planKind);
  }

  @Override
  public String toString() {
    return PlanSupport.stringifyTree(this, root());
  }

  private static <K> TObjectIntMap<K> mkIdentityMap() {
    return new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE);
  }

  private static <K> TObjectIntMap<K> copyIdentityMap(TObjectIntMap<K> other) {
    return new TObjectIntCustomHashMap<>(IdentityHashingStrategy.INSTANCE, other);
  }

  private static PlanNd[] copyNodesArray(PlanNd[] nds) {
    final PlanNd[] copiedNodes = new PlanNd[nds.length];
    for (int i = 0; i < nds.length; i++) {
      if (nds[i] != null) copiedNodes[i] = new PlanNd(nds[i]);
    }
    return copiedNodes;
  }

  private static class PlanNd extends Nd<PlanKind> {
    private PlanNode planNode;

    protected PlanNd(PlanKind planKind) {
      super(planKind);
    }

    protected PlanNd(PlanNd other) {
      super(other);
      this.planNode = other.planNode;
    }
  }
}
