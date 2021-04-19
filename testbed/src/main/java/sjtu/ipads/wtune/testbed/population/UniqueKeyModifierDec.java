package sjtu.ipads.wtune.testbed.population;

import static java.lang.Integer.parseInt;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniqueRandomIntDec;

class UniqueKeyModifierDec implements Modifier {
  private final int sharedLocalSeed;
  private final int totalDigits;
  private final int startDigit, endDigit;

  private final Generator nextStep;

  UniqueKeyModifierDec(
      int sharedLocalSeed, int totalDigits, int startDigit, int endDigit, Generator nextStep) {
    assert totalDigits >= 0 && totalDigits <= 9;
    assert startDigit >= 0 && startDigit <= endDigit;
    assert endDigit <= totalDigits;

    this.sharedLocalSeed = sharedLocalSeed;
    this.totalDigits = totalDigits;
    this.startDigit = startDigit;
    this.endDigit = endDigit;
    this.nextStep = nextStep;
  }

  @Override
  public void modify(int seed, Actuator actuator) {
    final int fullSeed = uniqueRandomIntDec(sharedLocalSeed, seed, totalDigits);
    final String fullSeedStr = String.format("%0" + totalDigits + "d", fullSeed);
    final int partSeed = parseInt(fullSeedStr.substring(startDigit, endDigit));

    nextStep.generate(partSeed, actuator);
  }
}
