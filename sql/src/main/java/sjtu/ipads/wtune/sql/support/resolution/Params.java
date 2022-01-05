package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast1.AdditionalInfo;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

public interface Params extends AdditionalInfo<Params> {
  AdditionalInfo.Key<Params> PARAMS = ParamsImpl::new;

  int numParams();

  ParamDesc paramOf(SqlNode node);
}
