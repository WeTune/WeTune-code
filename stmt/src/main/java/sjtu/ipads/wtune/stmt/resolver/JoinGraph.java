package sjtu.ipads.wtune.stmt.resolver;

import java.util.List;
import java.util.Set;
import sjtu.ipads.wtune.sql.relational.Relation;
import sjtu.ipads.wtune.sql.schema.Column;

public interface JoinGraph {
  Set<Relation> tables();

  Set<Relation> getJoined(Relation t);

  JoinKey getJoinKey(Relation t0, Relation t1);

  List<Set<Relation>> getSCC();

  void addTable(Relation tbl);

  void addJoin(Relation leftTbl, Column leftCol, Relation rightTbl, Column rightCol);

  interface JoinKey {
    Column leftCol();

    Column rightCol();

    Relation leftTable();

    Relation rightTable();
  }
}
