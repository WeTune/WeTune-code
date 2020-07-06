package sjtu.ipads.wtune.common.attrs;

import java.util.HashMap;
import java.util.Map;

class AttrsImpl implements Attrs<AttrsImpl> {
  private final Map<String, Object> directAttrs;

  AttrsImpl() {
    this(new HashMap<>());
  }

  AttrsImpl(Map<String, Object> directAttrs) {
    this.directAttrs = directAttrs;
  }

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }
}
