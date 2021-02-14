package sjtu.ipads.wtune.superopt.optimization.match;

import sjtu.ipads.wtune.sqlparser.relational.Attribute;

import java.util.List;

public interface ProjectionInterpretation {
  List<Attribute> projection();
}
