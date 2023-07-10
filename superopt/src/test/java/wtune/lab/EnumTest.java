package wtune.lab;

import org.junit.jupiter.api.Test;
import wtune.superopt.fragment.Fragment;
import wtune.superopt.fragment.FragmentSupport;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.List;

public class EnumTest {

    @Test
    void testTemplateNum() throws IOException {
        final List<Fragment> fragments_1 = FragmentSupport.enumFragments(1, 2);
        System.out.println("TemplateNum: " + fragments_1.size());
        if (fragments_1.size() != 7) {
            throw new UnexpectedException("unexpected num of templates");
        }
        final List<Fragment> fragments_2 = FragmentSupport.enumFragments(2, 2);
        System.out.println("TemplateNum: " + fragments_2.size());
        if (fragments_2.size() != 40) {
            throw new UnexpectedException("unexpected num of templates");
        }

        final List<Fragment> fragments_3 = FragmentSupport.enumFragments(3, 2);
        System.out.println("TemplateNum: " + fragments_3.size());
        if (fragments_3.size() != 165) {
            throw new UnexpectedException("unexpected num of templates");
        }

        final List<Fragment> fragments_4 = FragmentSupport.enumFragments(4, 2);
        System.out.println("TemplateNum: " + fragments_4.size());
        if (fragments_4.size() != 825) {
            throw new UnexpectedException("unexpected num of templates");
        }
    }

    @Test
    void testTemplateContent() throws IOException {
        final List<Fragment> fragments = FragmentSupport.enumFragments(2, 2);
        System.out.println("Your Templates: " + fragments);
        final List<String> fragments_exp = List.of(
                "Input",
                "Filter(Input)",
                "Proj(Input)",
                "Proj*(Input)",
                "InnerJoin(Input,Input)",
                "LeftJoin(Input,Input)",
                "Filter(Filter(Input))",
                "Filter(Proj(Input))",
                "Filter(Proj*(Input))",
                "InSubFilter(Input,Input)",
                "Proj(Filter(Input))",
                "Proj(Proj(Input))",
                "Proj(Proj*(Input))",
                "Proj*(Filter(Input))",
                "Proj*(Proj(Input))",
                "Proj*(Proj*(Input))",
                "InnerJoin(Input,Proj(Input))",
                "InnerJoin(Input,Proj*(Input))",
                "InnerJoin(Proj(Input),Input)",
                "InnerJoin(Proj*(Input),Input)",
                "LeftJoin(Input,Proj(Input))",
                "LeftJoin(Input,Proj*(Input))",
                "LeftJoin(Proj(Input),Input)",
                "LeftJoin(Proj*(Input),Input)",
                "Filter(InnerJoin(Input,Input))",
                "Filter(LeftJoin(Input,Input))",
                "Filter(InSubFilter(Input,Input))",
                "InSubFilter(Input,Proj(Input))",
                "InSubFilter(Input,Proj*(Input))",
                "InSubFilter(Proj(Input),Input)",
                "InSubFilter(Proj*(Input),Input)",
                "Proj(InnerJoin(Input,Input))",
                "Proj(LeftJoin(Input,Input))",
                "Proj(InSubFilter(Input,Input))",
                "Proj*(InnerJoin(Input,Input))",
                "Proj*(LeftJoin(Input,Input))",
                "Proj*(InSubFilter(Input,Input))",
                "InSubFilter(InnerJoin(Input,Input),Input)",
                "InSubFilter(LeftJoin(Input,Input),Input)",
                "InSubFilter(InSubFilter(Input,Input),Input)");
        fragments.forEach(it -> {
            if (!fragments_exp.contains(it.toString()))
                throw new RuntimeException("unexpected templates");
        });

        fragments_exp.forEach(it -> {
            if (!fragments.toString().contains(it))
                throw new RuntimeException("unexpected templates");
        });
    }


}
