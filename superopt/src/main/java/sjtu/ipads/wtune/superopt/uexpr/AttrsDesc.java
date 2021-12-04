package sjtu.ipads.wtune.superopt.uexpr;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;

public class AttrsDesc {
  public final UName name;
  public final List<UVar> projections;
  public final TIntList projectedSchemas;

  public AttrsDesc(UName name) {
    this.name = name;
    this.projections = new ArrayList<>(4);
    this.projectedSchemas = new TIntArrayList(4);
  }

  public UName name() {
    return name;
  }

  void addProjectedVar(UVar projection, int schema) {
    projections.add(projection);
    projectedSchemas.add(schema);
  }
}
