package wtune.common.datasource.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLServerPlanTree {
  private final String appName;
  private final int stmtId;
  private final List<TreeNode> allNodes;
  private final List<TreeNode> roots;// may be multiple root due to subquery

  public static SQLServerPlanTree constructPlanTree(String planInfo) {
    final SQLServerPlanTree planTree = new SQLServerPlanTree();
    final String[] lines = planInfo.split("\n");
    for (String line : lines) {
      final String[] fields = line.split(";");
      if (fields.length != 7) return null;
      planTree.insertNode(
          new TreeNode(
              fields[0],
              Integer.parseInt(fields[1]),
              fields[4],
              Double.parseDouble(fields[6])),
          Integer.parseInt(fields[2]));
    }
    return planTree;
  }

  public SQLServerPlanTree() {
    this.appName = "unknown";
    this.stmtId = -1;
    this.allNodes = new ArrayList<>();
    this.roots = new ArrayList<>();
  }

  public SQLServerPlanTree(String appName, int stmtId) {
    this.appName = appName;
    this.stmtId = stmtId;
    this.allNodes = new ArrayList<>();
    this.roots = new ArrayList<>();
  }

  public String getAppName() {
    return appName;
  }

  public int getStmtId() {
    return stmtId;
  }

  private List<TreeNode> getRoots() {
    return roots;
  }

  private TreeNode getRoot(int i) {
    return getRoots().get(i);
  }

  public int getRootNum() {
    return this.roots.size();
  }

  public void insertNode(TreeNode node, int parentId) {
    allNodes.add(node);
    if (parentId == 0) {
      this.roots.add(node);
    } else {
      for (TreeNode existNode : allNodes) {
        if (existNode.getNodeId() == parentId) {
          existNode.addChildren(node);
        }
      }
    }
  }

  public boolean moreCostThan(SQLServerPlanTree other) {
    return this.getRoot(0).getTotalSubtreeCost() > other.getRoot(0).getTotalSubtreeCost();
  }

  public static boolean samePlan(SQLServerPlanTree plan1, SQLServerPlanTree plan2) {
    if (plan1 == null || plan2 == null) return false;

    if (plan1.getRootNum() != plan2.getRootNum()) return false;

    for (int i = 0; i < plan1.getRootNum(); i++) {
      plan1.getRoot(i).adjustChildrenPosition();
      plan2.getRoot(i).adjustChildrenPosition();
      if (!sameTreeStruct(plan1.getRoot(i), plan2.getRoot(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean sameTreeStruct(TreeNode root1, TreeNode root2) {
    if (!root1.isSameOp(root2) || root1.childrenNum() != root2.childrenNum()) {
      return false;
    }
    for (int i = 0; i < root1.childrenNum(); i++) {
      if (!sameTreeStruct(root1.getNthChildren(i), root2.getNthChildren(i))) {
        return false;
      }
    }
    return true;
  }

  private static class TreeNode implements Comparable<TreeNode> {
    private final String stmtText;
    private final int nodeId;
    private final String logicalOp;
    private final double totalSubtreeCost;

    private final List<TreeNode> children;

    public TreeNode(String stmtText, int nodeId, String logicalOp, double totalSubtreeCost) {
      this.stmtText = stmtText;
      this.nodeId = nodeId;
      this.logicalOp = logicalOp;
      this.totalSubtreeCost = totalSubtreeCost;
      this.children = new ArrayList<>();
    }

    public String getStmtText() {
      return stmtText;
    }

    public int getNodeId() {
      return nodeId;
    }

    public String getLogicalOp() {
      return logicalOp;
    }

    public double getTotalSubtreeCost() {
      return totalSubtreeCost;
    }

    public List<TreeNode> getChildren() {
      return children;
    }

    public TreeNode getNthChildren(int n) {
      return this.children.get(n);
    }

    public void addChildren(TreeNode node) {
      this.children.add(node);
    }

    public int childrenNum() {
      return this.children.size();
    }

    public boolean isLeafNode() {
      return this.children.isEmpty();
    }

    public boolean isSameOp(TreeNode other) {
      return this.logicalOp.equals(other.getLogicalOp());
    }

    public void adjustChildrenPosition() {
      Collections.sort(this.children);
      if (!isLeafNode()) {
        this.children.forEach(TreeNode::adjustChildrenPosition);
      }
    }

    @Override
    public int compareTo(TreeNode other) {
      return this.logicalOp.compareTo(other.getLogicalOp());
    }
  }

}
