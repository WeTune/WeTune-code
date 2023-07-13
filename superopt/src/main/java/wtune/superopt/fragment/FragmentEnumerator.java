package wtune.superopt.fragment;

import com.google.common.collect.Sets;
import wtune.superopt.fragment.pruning.Rule;
import wtune.superopt.util.Hole;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static wtune.common.utils.IterableSupport.none;

class FragmentEnumerator {
    private final int maxOps;
    private final List<Op> opSet;
    private final Set<Rule> pruningRules;

    FragmentEnumerator(List<Op> opSet, int maxOps) {
        this.maxOps = maxOps;
        this.opSet = opSet;
        this.pruningRules = new HashSet<>(8);
    }

    void setPruningRules(Iterable<Rule> rules) {
        rules.forEach(pruningRules::add);
    }

    List<Fragment> enumerate() {
        Set<FragmentImpl> fragmentSet = enumerateFragmentSet();
        return fragmentSet.stream()
                .peek(FragmentSupport::setupFragment) // fragment initialization 1
                .filter(f -> none(pruningRules, it -> it.match(f))) // reduce useless fragments
                .sorted((x, y) -> FragmentUtils.structuralCompare(x.root(), y.root()))
                .peek(FragmentImpl::symbols) // fragment initialization 2
                .collect(Collectors.toList());
    }

    private Set<FragmentImpl> enumerateFragmentSet() {
        Set<FragmentImpl> result = Collections.singleton(new FragmentImpl(null));

        /* TODO-1a: Enumerate all the possible templates into result set */

        /* END TODO-1a */

        return result;
    }

    /* TODO-1b: You may add function(s) here */

    /* END TODO-1b */
}
