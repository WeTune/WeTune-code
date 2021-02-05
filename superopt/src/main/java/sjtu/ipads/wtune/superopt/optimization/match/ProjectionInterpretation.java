package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.sqlparser.rel.Attribute;

import java.util.List;

public interface ProjectionInterpretation {
  List<Attribute> projection();
}
