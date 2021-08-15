package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.fragment1.FragmentSupport;

import java.util.List;

public class EnumFragment implements Runner {
  @Override
  public void prepare(String[] argStrings) {}

  @Override
  public void run() throws Exception {
    final List<Fragment> fragments = FragmentSupport.enumFragments();
    for (Fragment fragment : fragments) System.out.println(fragment);
    System.out.println(fragments.size());
  }
}
