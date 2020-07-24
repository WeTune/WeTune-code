package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class WorkloadGen implements ScriptNode {
  private final List<StmtGen> stmtGens;

  public WorkloadGen(List<Statement> stmts) {
    final List<StmtGen> gens = stmtGens = new ArrayList<>(stmts.size());
    for (int i = 0; i < stmts.size(); i++) gens.add(new StmtGen(stmts.get(i), i));
  }

  @Override
  public List<? extends ScriptNode> children() {
    return stmtGens;
  }

  @Override
  public void outputHead(Output out) {
    out.println("local M = require('testbed.parammod')")
        .println("local statements = {")
        .increaseIndent();
  }

  @Override
  public void outputTail(Output out) {
    out.decreaseIndent().println("}").println().println("return statements");
  }

  @Override
  public void outputAfterChild(Output out) {
    out.println(",");
  }
}
