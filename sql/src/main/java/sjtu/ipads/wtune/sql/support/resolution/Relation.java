package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast.SqlNode;

import java.util.List;

import static sjtu.ipads.wtune.sql.ast.SqlKind.Query;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.SimpleSource;

public interface Relation {
  SqlNode rootNode(); // invariant: isRelationBoundary(rootNode())

  String qualification();

  List<Relation> inputs();

  List<Attribute> attributes();

  Attribute resolveAttribute(String qualification, String name);

  static boolean isRelationRoot(SqlNode node) {
    return Query.isInstance(node) || SimpleSource.isInstance(node);
  }
}
