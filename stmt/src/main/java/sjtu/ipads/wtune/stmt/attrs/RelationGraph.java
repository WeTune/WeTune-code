package sjtu.ipads.wtune.stmt.attrs;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import sjtu.ipads.wtune.common.utils.Func3;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.isDerived;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.isSimple;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

public class RelationGraph {
  private final MutableValueGraph<Relation, JoinCondition> graph;

  private RelationGraph(MutableValueGraph<Relation, JoinCondition> graph) {
    this.graph = graph;
  }

  public static RelationGraph build(MutableValueGraph<Relation, JoinCondition> graph) {
    return new RelationGraph(graph);
  }

  public MutableValueGraph<Relation, JoinCondition> graph() {
    return graph;
  }

  public Set<Relation> independentNeighbours(SQLNode root, Relation relation) {
    final Set<Relation> neighbours = graph.adjacentNodes(relation);
    final SQLNode thisNode = relation.locateNodeIn(root);
    final Set<Relation> ret = new HashSet<>(neighbours.size());
    for (Relation neighbour : neighbours)
      if (scopeOf(thisNode) == scopeOf(neighbour.locateNodeIn(root))) ret.add(neighbour);
    return ret;
  }

  private static QueryScope scopeOf(SQLNode node) {
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    return node.type() == SQLNode.Type.QUERY ? scope.parent() : scope;
  }

  public RelationGraph expanded() {
    final var expandedGraph = ValueGraphBuilder.from(graph).build();
    //    graph.nodes().forEach(expandedGraph::addNode);

    for (EndpointPair<Relation> edge : graph.edges()) {
      final JoinCondition expendedEdge = expandEdge(graph.edgeValue(edge).get());
      // deal with weird stmt like "tbl.id in (select tbl.id where tbl.col = xx)"
      if (expendedEdge.left().equals(expendedEdge.right())) continue;

      assert expandedGraph.nodes().contains(expendedEdge.left());
      assert expandedGraph.nodes().contains(expendedEdge.right());

      expandedGraph.putEdgeValue(expendedEdge.left(), expendedEdge.right(), expendedEdge);
    }

    graph.nodes().stream()
        .filter(it -> !expandedGraph.nodes().contains(it))
        .forEach(expandedGraph::addNode);

    return RelationGraph.build(expandedGraph);
  }

  private JoinCondition expandEdge(JoinCondition edge) {
    final Relation left = edge.left();
    final Relation right = edge.right();
    if (isSimple(left.node()) && isSimple(right.node())) return edge;
    return expandEdge(expandEdge(edge, left), right);
  }

  private JoinCondition expandEdge(JoinCondition edge, Relation targetSide) {
    final SQLNode targetNode = targetSide.node();
    if (isSimple(targetNode)) return edge;
    if (isDerived(targetNode)) {
      return expandDerived(edge, targetSide);

    } else if (targetNode.type() == SQLNode.Type.QUERY) {
      return expandSubquery(edge, targetSide);

    } else return assertFalse();
  }

  private JoinCondition expandDerived(JoinCondition edge, Relation targetSide) {
    final SQLNode targetNode = targetSide.node();

    final SelectItem item =
        targetNode.get(RESOLVED_TABLE_SOURCE).resolveAsSelection(edge.thisColumn(targetSide));
    assert item != null;

    final ColumnRef columnRef = item.expr().get(RESOLVED_COLUMN_REF);
    if (columnRef == null) return edge;

    final String name = item.simpleName();
    final TableSource source = columnRef.source();

    if (name == null || source == null) return edge;

    final Relation newRelation = Relation.of(source.node());
    final JoinCondition newCondition =
        JoinCondition.of(
            null, newRelation, edge.thatRelation(targetSide), name, edge.thatColumn(targetSide));

    return expandEdge(newCondition, newRelation);
  }

  private JoinCondition expandSubquery(JoinCondition edge, Relation targetSide) {
    final SQLNode targetNode = targetSide.node();
    final SelectItem item =
        targetNode.get(RESOLVED_QUERY_SCOPE).resolveSelection(edge.thisColumn(targetSide));

    assert item != null;

    final ColumnRef columnRef = item.expr().get(RESOLVED_COLUMN_REF);
    if (columnRef == null) return null;

    final TableSource source = columnRef.source();
    final String columnName = item.simpleName();
    if (source == null || columnName == null) return edge;

    final Relation newRelation = Relation.of(source.node());
    final JoinCondition newCondition =
        JoinCondition.of(
            null,
            newRelation,
            edge.thatRelation(targetSide),
            columnName,
            edge.thatColumn(targetSide));

    return expandEdge(newCondition, newRelation);
  }

  /** Call this on an expanded relation graph!! */
  public void calcRelationPosition() {
    for (Relation rel : graph.nodes()) {
      if (!rel.isTableSource()) continue;
      if (rel.position() == Integer.MIN_VALUE) calc(rel);
    }
    final int min =
        graph.nodes().stream()
            .mapToInt(Relation::position)
            .filter(it -> it != Integer.MIN_VALUE)
            .min()
            .orElse(0);

    for (Relation rel : graph.nodes()) {
      final int position = rel.position() != Integer.MIN_VALUE ? rel.position() - min : 0;
      rel.setPosition(position);
      if (rel.isTableSource()) rel.node().put(RELATION_POSITION, position);
    }
  }

  private int calc(Relation rel) {
    updatePosition(rel, calc0(rel));
    return rel.position();
  }

  private int calc0(Relation rel) {
    return children(rel).stream().mapToInt(this::calc0).max().orElse(0);
  }

  private void updatePosition(Relation rel, int newPosition) {
    if (newPosition > rel.position()) {
      rel.setPosition(newPosition);
      children(rel).forEach(it -> updatePosition(it, newPosition - 1));
      peers(rel).forEach(it -> updatePosition(it, newPosition));
      parents(rel).forEach(it -> updatePosition(it, newPosition + 1));
    }
  }

  private final Map<Relation, Set<Relation>> cachedChildren = new HashMap<>();
  private final Map<Relation, Set<Relation>> cachedParents = new HashMap<>();
  private final Map<Relation, Set<Relation>> cachedPeers = new HashMap<>();

  private Set<Relation> children(Relation rel) {
    return filterNeighbours(rel, RelationGraph::isChild, cachedChildren);
  }

  private Set<Relation> parents(Relation rel) {
    return filterNeighbours(rel, RelationGraph::isParent, cachedParents);
  }

  private Set<Relation> peers(Relation rel) {
    return filterNeighbours(rel, RelationGraph::isPeer, cachedPeers);
  }

  private Set<Relation> filterNeighbours(
      Relation rel,
      Func3<Boolean, JoinCondition, Relation, Relation> filter,
      Map<Relation, Set<Relation>> cache) {
    final Set<Relation> cached = cache.get(rel);
    if (cached != null) return cached;

    final Set<Relation> filtered = new HashSet<>();
    for (Relation neighbour : graph.adjacentNodes(rel)) {
      final JoinCondition edge = graph.edgeValue(rel, neighbour).get();
      if (filter.apply(edge, edge.thisRelation(rel), edge.thatRelation(rel)))
        filtered.add(neighbour);
    }

    cache.put(rel, filtered);
    return filtered;
  }

  private static boolean isPeer(JoinCondition cond, Relation thisRel, Relation thatRel) {
    final ColumnRef thisRef = tryResolveColumnRef(thisRel, cond.thisColumn(thisRel));
    final ColumnRef thatRef = tryResolveColumnRef(thatRel, cond.thisColumn(thatRel));

    if (thisRef == null || thatRef == null) return true;

    final Column thisColumn = thisRef.resolveAsColumn();
    final Column thatColumn = thatRef.resolveAsColumn();

    if (thisColumn == null || thatColumn == null) return true;

    return thisColumn.uniquePart() == thatColumn.uniquePart();
  }

  /** If `thatRel` is a child of `thisRel` */
  private static boolean isChild(JoinCondition cond, Relation thisRel, Relation thatRel) {
    final ColumnRef thisRef = tryResolveColumnRef(thisRel, cond.thisColumn(thisRel));
    final ColumnRef thatRef = tryResolveColumnRef(thatRel, cond.thisColumn(thatRel));

    if (thisRef == null || thatRef == null) return false;

    final Column thisColumn = thisRef.resolveAsColumn();
    final Column thatColumn = thatRef.resolveAsColumn();

    if (thisColumn == null || thatColumn == null) return false;

    return thisColumn.uniquePart() && !thatColumn.uniquePart();
  }

  private static boolean isParent(JoinCondition cond, Relation thisRel, Relation thatRel) {
    return !isPeer(cond, thisRel, thatRel) && !isChild(cond, thisRel, thatRel);
  }

  private static ColumnRef tryResolveColumnRef(Relation rel, String column) {
    if (rel.isTableSource()) {
      final ColumnRef ref = new ColumnRef();
      if (!rel.node().get(RESOLVED_TABLE_SOURCE).resolveRef(column, ref)) return null;
      else return ref;

    } else if (rel.node().type() == SQLNode.Type.QUERY) {
      return rel.node()
          .get(RESOLVED_QUERY_SCOPE)
          .resolveSelection(column)
          .expr()
          .get(RESOLVED_COLUMN_REF);
    } else return assertFalse();
  }

  @Override
  public String toString() {
    return graph.toString();
  }
}
