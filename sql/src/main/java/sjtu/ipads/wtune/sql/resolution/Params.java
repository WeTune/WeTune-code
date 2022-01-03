package sjtu.ipads.wtune.sql.resolution;

import sjtu.ipads.wtune.sql.ast1.AdditionalInfo;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

public interface Params extends AdditionalInfo<Params> {
  AdditionalInfo.Key<Params> PARAMS = ParamsImpl::new;

  ParamDesc paramOf(SqlNode node);
}
