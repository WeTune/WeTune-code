package sjtu.ipads.wtune.superopt.substitution;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.common.utils.NaturalCongruence;
import sjtu.ipads.wtune.prover.uexpr2.*;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.*;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.find;
import static sjtu.ipads.wtune.prover.uexpr2.UVar.*;

/** Translate a candidate rule to U-expr. */
class UExprTranslator {
  private static final UName NAME_IS_NULL = UName.mk("IsNull");

  private final Substitution rule;
  private final NameSequence relSeq, attrsSeq, predSeq, varSeq;
  private final NaturalCongruence<Symbol> isomorphicSyms;
  private final Map<Symbol, UName> initiatedNames;

  UExprTranslator(Substitution rule) {
    this.rule = rule;
    this.relSeq = NameSequence.mkIndexed("r", 0);
    this.attrsSeq = NameSequence.mkIndexed("a", 0);
    this.predSeq = NameSequence.mkIndexed("p", 0);
    this.varSeq = NameSequence.mkIndexed("t", 0);
    this.isomorphicSyms = rule.constraints().congruence();
    this.initiatedNames = new HashMap<>(16);
  }

  Pair<TranslationResult, TranslationResult> translate() {
    return Pair.of(
        new TemplateTranslator(rule._0()).translate(),
        new TemplateTranslator(rule._1()).translate());
  }

  private static UExpr mkNotNull(UVar var) {
    return UNeg.mk(UPred.mk(UVar.mkFunc(NAME_IS_NULL, var)));
  }

  private static UExpr mkIsNull(UVar var) {
    return UPred.mk(UVar.mkFunc(NAME_IS_NULL, var));
  }

  class TemplateTranslator {
    private final Fragment template;
    private final List<UVar> freeVars;
    private final List<UVar> auxVars;
    private final TranslationResult result;

    private TemplateTranslator(Fragment template) {
      this.template = template;
      this.freeVars = new ArrayList<>(3);
      this.auxVars = new ArrayList<>(1);
      this.result = new TranslationResult();
    }

    private TranslationResult translate() {
      result.expr = tr(template.root());
      result.freeVar = tail(freeVars);
      assert freeVars.size() == 1;
      assert auxVars.isEmpty();
      return result;
    }

    private UName mkName(Symbol sym, NameSequence nameSeq) {
      final Set<Symbol> isoSyms = isomorphicSyms.eqClassOf(sym);
      final Symbol initiatedSym = find(isoSyms, initiatedNames::containsKey);

      final UName name;
      if (initiatedSym == null) name = UName.mk(nameSeq.next());
      else name = initiatedNames.get(initiatedSym);

      initiatedNames.put(sym, name);
      result.symToName.put(sym, name);

      return name;
    }

    private UVar mkFreshVar() {
      final UVar var = UVar.mkBase(UName.mk(varSeq.next()));
      freeVars.add(var);
      return var;
    }

    private UVar mkVisibleVar() {
      final Optional<UVar> foldedVar = auxVars.stream().reduce(UVar::mkConcat);
      final UVar freeVar = tail(freeVars);
      return foldedVar.isEmpty() ? freeVar : mkConcat(foldedVar.get(), freeVar);
    }

    private UExpr tr(Op op) {
      switch (op.kind()) {
        case INPUT:
          return trInput((Input) op);
        case SIMPLE_FILTER:
          return trSimpleFilter((SimpleFilter) op);
        case IN_SUB_FILTER:
          return trInSubFilter((InSubFilter) op);
        case EXISTS_FILTER:
          return trExistsFilter((ExistsFilter) op);
        case PROJ:
          return trProj((Proj) op);
        case INNER_JOIN:
          return trJoin((Join) op);
        default:
          assert false;
      }
      return null;
    }

    private UExpr trInput(Input input) {
      final UName tableName = mkName(input.table(), relSeq);
      final UVar var = mkFreshVar();
      result.tableToVar.put(tableName, var);
      return UTable.mk(tableName, var);
    }

    private UExpr trSimpleFilter(SimpleFilter filter) {
      final UExpr predecessor = tr(filter.predecessors()[0]);

      final UVar visibleVar = mkVisibleVar();
      final UName attrsName = mkName(filter.attrs(), attrsSeq);
      final UName predName = mkName(filter.predicate(), predSeq);
      final UVar projVar = mkProj(attrsName, visibleVar);
      final UVar binaryVar = UVar.mkFunc(predName, projVar);

      return UMul.mk(predecessor, UPred.mk(binaryVar));
    }

    private UExpr trInSubFilter(InSubFilter filter) {
      final UExpr lhs = tr(filter.predecessors()[0]);
      final UVar lhsFreeVar = tail(freeVars);

      push(auxVars, tail(freeVars));
      final UExpr rhs = tr(filter.predecessors()[1]);
      final UVar rhsFreeVar = pop(freeVars);
      pop(auxVars);

      final UName attrsName = mkName(filter.attrs(), attrsSeq);
      final UVar lhsProjVar = mkProj(attrsName, lhsFreeVar);
      final UVar eqVar = mkEq(lhsProjVar, rhsFreeVar);
      return UMul.mk(lhs, USquash.mk(UMul.mk(rhs, UPred.mk(eqVar))));
    }

    private UExpr trExistsFilter(ExistsFilter filter) {
      final UExpr lhs = tr(filter.predecessors()[0]);
      push(auxVars, tail(freeVars));
      final UExpr rhs = tr(filter.predecessors()[1]);
      pop(auxVars);

      return UMul.mk(lhs, USquash.mk(rhs));
    }

    private UExpr trProj(Proj proj) {
      final UExpr predecessor = tr(proj.predecessors()[0]);
      final UVar oldFreeVar = pop(freeVars);
      final UName attrName = mkName(proj.attrs(), attrsSeq);
      final UVar projVar = mkProj(attrName, oldFreeVar);
      final UVar newFreeVar = mkFreshVar();
      final UVar eqVar = mkEq(newFreeVar, projVar);
      return USum.mk(oldFreeVar.usedVars(), UMul.mk(predecessor, UPred.mk(eqVar)));
    }

    private UExpr trJoin(Join join) {
      final UExpr lhs = tr(join.predecessors()[0]);
      final UExpr rhs = tr(join.predecessors()[1]);
      final UVar rhsFreeVar = pop(freeVars);
      final UVar lhsFreeVar = pop(freeVars);
      push(freeVars, mkConcat(lhsFreeVar, rhsFreeVar));
      final UName lhsAttrsName = mkName(join.lhsAttrs(), attrsSeq);
      final UName rhsAttrsName = mkName(join.rhsAttrs(), attrsSeq);
      final UVar lhsProjVar = mkProj(lhsAttrsName, lhsFreeVar);
      final UVar rhsProjVar = mkProj(rhsAttrsName, rhsFreeVar);
      final UExpr eqCond = UPred.mk(mkEq(lhsProjVar, rhsProjVar));
      final UExpr notNullCond = mkNotNull(rhsFreeVar);
      final UMul symm = UMul.mk(UMul.mk(UMul.mk(lhs, rhs), eqCond), notNullCond);
      if (join.kind() == OperatorType.INNER_JOIN) return symm;

      UExpr newExpr = UMul.mk(UMul.mk(rhs, eqCond), notNullCond);
      ArrayList<UVar> newVars = new ArrayList<>(rhsFreeVar.usedVars().length);
      for (UVar oldVar : rhsFreeVar.usedVars()) {
        final UVar newVar = mkBase(UName.mk(varSeq.next()));
        newExpr = newExpr.replaceBaseVar(oldVar, newVar);
        newVars.add(newVar);
      }

      final UMul asymm =
          UMul.mk(
              UMul.mk(lhs, mkIsNull(rhsFreeVar)),
              UNeg.mk(USum.mk(newVars.toArray(UVar[]::new), newExpr)));
      return UAdd.mk(symm, asymm);
    }
  }
}
