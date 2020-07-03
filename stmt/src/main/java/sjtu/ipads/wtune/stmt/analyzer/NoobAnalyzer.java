package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class NoobAnalyzer implements Analyzer<Void>, SQLVisitor {
  @Override
  public boolean enter(SQLNode node) {
    System.out.println(node.get(NODE_ID));
    return true;
  }

  @Override
  public Void analyze(SQLNode node) {
    node.accept(this);
    return null;
  }
}
