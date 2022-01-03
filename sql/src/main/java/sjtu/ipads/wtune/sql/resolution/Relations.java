package sjtu.ipads.wtune.sql.resolution;

import sjtu.ipads.wtune.sql.ast1.AdditionalInfo;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

public interface Relations extends AdditionalInfo<Relations> {
  AdditionalInfo.Key<Relations> RELATION = RelationsImpl::new;

  Relation enclosingRelationOf(SqlNode node);
}
