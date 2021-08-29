package sjtu.ipads.wtune.sqlparser.plan;

import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

class RefImpl implements Ref {
  private final String intrinsicQualification, intrinsicName;

  RefImpl(String intrinsicQualification, String intrinsicName) {
    this.intrinsicQualification = simpleName(intrinsicQualification);
    this.intrinsicName = simpleName(intrinsicName);
  }

  RefImpl(Ref other) {
    this.intrinsicQualification = other.intrinsicQualification();
    this.intrinsicName = other.intrinsicName();
  }

  @Override
  public String intrinsicQualification() {
    return intrinsicQualification;
  }

  @Override
  public String intrinsicName() {
    return intrinsicName;
  }

  @Override
  public String toString() {
    if (intrinsicQualification == null) return intrinsicName;
    else return intrinsicQualification + "." + intrinsicName;
  }
}
