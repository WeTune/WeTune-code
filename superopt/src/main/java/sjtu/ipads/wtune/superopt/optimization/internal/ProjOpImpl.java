package sjtu.ipads.wtune.superopt.optimization.internal;

import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.superopt.optimization.ProjOp;

import java.util.List;

public class ProjOpImpl extends OperatorBase implements ProjOp {
  private final List<Attribute> projection;

  private ProjOpImpl(List<Attribute> projection) {
    this.projection = projection;
  }

  public static ProjOpImpl build(List<Attribute> projection) {
    return new ProjOpImpl(projection);
  }

  @Override
  public List<Attribute> projection() {
    return projection;
  }
}
