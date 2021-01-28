package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class PatchesGen implements ScriptNode {
  private final List<PatchGen> gens;

  public PatchesGen(List<SchemaPatch> patches, boolean isApplication) {
    this.gens = listMap(p -> new PatchGen(p, isApplication), patches);
  }

  @Override
  public void outputBeforeChild(Output out, int index, int total) {
    if (index != 0) out.println().println();
  }

  @Override
  public List<? extends ScriptNode> children() {
    return gens;
  }
}
