package sjtu.ipads.wtune.sqlparser.plan1;

class RefImpl implements Ref {
  private final String intrinsicQualification, intrinsicName;

  RefImpl(String intrinsicQualification, String intrinsicName) {
    this.intrinsicQualification = intrinsicQualification;
    this.intrinsicName = intrinsicName;
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
