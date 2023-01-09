package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.BatchActuator;

import java.util.stream.IntStream;

import static java.util.stream.IntStream.range;
import static sjtu.ipads.wtune.testbed.util.MathHelper.pow10;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.deRandUniqueIntDec;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.randUniqueIntDec;

class UniqueKeyModifier implements Modifier {
  private final int localSeed;
  private final int totalDigits;
  private final int lowDigit, highDigit;
  private final int mod, div, max;

  private final Generator nextStep;

  UniqueKeyModifier(
      int sharedLocalSeed, int totalDigits, int startDigit, int endDigit, Generator nextStep) {
    assert totalDigits >= 0 && totalDigits <= 9;
    assert startDigit >= 0 && startDigit <= endDigit;
    assert endDigit <= totalDigits;

    this.localSeed = sharedLocalSeed;
    this.totalDigits = totalDigits;
    this.lowDigit = startDigit;
    this.highDigit = endDigit;
    this.mod = pow10(endDigit);
    this.div = pow10(startDigit);
    this.max = pow10(endDigit - startDigit);
    this.nextStep = nextStep;
  }

  @Override
  public void modify(int seed, BatchActuator actuator) {
    final int fullSeed = randUniqueIntDec(localSeed, seed, totalDigits);
    final int partSeed = (fullSeed % mod) / div;

    nextStep.generate(partSeed, actuator);
  }

  @Override
  public IntStream locate(Object value) {
    final int max = this.max;
    return nextStep.locate(value).filter(it -> it >= 0 && it < max).flatMap(this::locate0);
  }

  private IntStream locate0(int value) {
    final int localSeed = this.localSeed;
    final int totalDigits = this.totalDigits;
    final int mid = value * div;
    final int lowRoom = pow10(lowDigit);
    final int highRoom = pow10(totalDigits - highDigit);
    final int mod = this.mod;

    return range(0, lowRoom)
        .flatMap(
            i ->
                range(0, highRoom)
                    .map(j -> deRandUniqueIntDec(localSeed, mod * j + mid + i, totalDigits)));
  }

  @Override
  public boolean isPrePopulated() {
    return nextStep.isPrePopulated();
  }
}