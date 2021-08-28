package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.FragmentSupport;

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
