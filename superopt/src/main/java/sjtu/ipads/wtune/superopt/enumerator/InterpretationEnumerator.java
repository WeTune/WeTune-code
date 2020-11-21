package sjtu.ipads.wtune.superopt.enumerator;

public class InterpretationEnumerator {
  public static int factorial(int n) {
    if (n == 0) return 1;
    return n * factorial(n - 1);
  }

  public static int P(int n, int r) {
    return factorial(n) / factorial(n - r);
  }

  public static int C(int n, int r) {
    return factorial(n) / factorial(r) / factorial(n - r);
  }

  public static int permuteInterpret2(int m, int n) {
    if (n > m) {
      int temp = m;
      m = n;
      n = temp;
    }
    if (n == 0) return 0;

    int res = P(m, n);

    res += n * permuteInterpret2(m, n - 1);
    return res;
  }

  public static int permuteInterpret(int m, int n) {
    int ret = 0;
    for (int i = 1; i <= n; i++) {
      for (int j = 1; j <= m; j++) {
        ret += (n - i + 1) * (m - j + 1);
      }
    }
    return ret;
  }

  public static void main(String[] args) {
    //    System.out.println(permuteInterpret2(2, 2));
    for (int i = 1; i <= 4; i++) {
      for (int j = 1; j <= 4; j++) {
        System.out.println(i + "," + j + " " + permuteInterpret2(i, j));
      }
    }
  }
}
