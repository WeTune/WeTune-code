package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniformRandomInt;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.uniqueRandomIntBin;

class UniqueKeyModifierBin implements Modifier {
  private final int sharedLocalSeed;
  private final int totalBits;
  private final int startBit, endBit;

  private final Generator nextStep;

  UniqueKeyModifierBin(
      int sharedLocalSeed, int totalBits, int startBit, int endBit, Generator nextStep) {
    assert totalBits >= 0 && totalBits <= 30;
    assert startBit >= 0 && startBit <= endBit;
    assert endBit <= totalBits;

    this.sharedLocalSeed = sharedLocalSeed;
    this.totalBits = totalBits;
    this.startBit = startBit + (32 - totalBits);
    this.endBit = endBit + (32 - totalBits);
    this.nextStep = nextStep;
  }

  @Override
  public void modify(int seed, Actuator actuator) {
    final int fullSeed;
    if (seed < (1 << totalBits)) {
      fullSeed = uniqueRandomIntBin(sharedLocalSeed, seed, totalBits);
    } else {
      fullSeed = uniformRandomInt(seed);
    }
    final int partSeed = (fullSeed << startBit) >>> (32 - endBit + startBit);
    nextStep.generate(partSeed, actuator);
  }
}
