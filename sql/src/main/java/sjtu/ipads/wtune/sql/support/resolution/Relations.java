package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast.AdditionalInfo;
import sjtu.ipads.wtune.sql.ast.SqlNode;

public interface Relations extends AdditionalInfo<Relations> {
  AdditionalInfo.Key<Relations> RELATION = RelationsImpl::new;

  Relation enclosingRelationOf(SqlNode node);
}
