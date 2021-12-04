package sjtu.ipads.wtune.superopt.uexpr;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.push;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.common.utils.ListSupport.pop;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.INNER_JOIN;
import static sjtu.ipads.wtune.superopt.uexpr.UExprSupport.normalizeExpr;
import static sjtu.ipads.wtune.superopt.uexpr.UTerm.FUNC_IS_NULL_NAME;
import static sjtu.ipads.wtune.superopt.uexpr.UVar.*;

/**
 * Translate a <b>valid</b> candidate rule to U-expr.
 *
 * <p>A rule (S,T,C) is valid only if:
 *
 * <ul>
 *   <li>Any Attrs in S has a single, viable source.
 *   <li>Any Table in T has a single, exclusive instantiation.
 *   <li>Any Attrs/Pred in T has a single instantiation
 *   <li>Any Attrs in T has a viable implied source.
 * </ul>
 */
class UExprTranslator {
  private static final UName NAME_IS_NULL = UName.mk(FUNC_IS_NULL_NAME);
  private static final boolean SUPPORT_DEPENDENT_SUBQUERY = false;

  private final Substitution rule;
  private final NameSequence tableSeq, attrsSeq, predSeq, varSeq;
  private final Map<Symbol, UName> initiatedNames;
  private final UExprTranslationResult result;
  private int nextSchema;

  UExprTranslator(Substitution rule) {
    this.rule = rule;
    this.tableSeq = NameSequence.mkIndexed("r", 0);
    this.attrsSeq = NameSequence.mkIndexed("a", 0);
    this.predSeq = NameSequence.mkIndexed("p", 0);
    this.varSeq = NameSequence.mkIndexed("x", 0);
    this.initiatedNames = new HashMap<>(16);
    this.result = new UExprTranslationResult(rule);
    this.nextSchema = 1;
  }

  UExprTranslationResult translate() {
    new TemplateTranslator(rule._0(), false).translate();
    new TemplateTranslator(rule._1(), true).translate();
    return result;
  }

  private static UTerm mkNotNull(UVar var) {
    return UNeg.mk(UPred.mk(UVar.mkFunc(NAME_IS_NULL, var)));
  }

  private static UTerm mkIsNull(UVar var) {
    return UPred.mk(UVar.mkFunc(NAME_IS_NULL, var));
  }

  class TemplateTranslator {
    private final Fragment template;
    private final boolean isTargetSide;
    private final List<UVar> freeVars; // Free variable in current scope.
    private UVar auxVar; // Auxiliary variable from outer query.

    private TemplateTranslator(Fragment template, boolean isTargetSide) {
      this.template = template;
      this.isTargetSide = isTargetSide;
      this.freeVars = new ArrayList<>(3);
      this.auxVar = null;
    }

    private void translate() {
      final UTerm expr = normalizeExpr(tr(template.root()));
      final UVar freeVar = tail(freeVars);
      assert freeVars.size() == 1;
      assert auxVar == null;

      if (!isTargetSide) {
        result.srcExpr = expr;
        result.srcFreeVar = freeVar;
      } else {
        result.tgtExpr = expr;
        result.tgtFreeVar = freeVar;
      }
    }

    private UName mkName(Symbol sym, NameSequence nameSeq) {
      /* Create a new or retrieve an existing name for a symbol. */
      // Memo: name is nothing to do with `isTargetSide`.
      // Equivalent symbols always has the same name.
      final Set<Symbol> eqClass = rule.constraints().eqSymbols().eqClassOf(sym);
      final Symbol initiatedSym = linearFind(eqClass, initiatedNames::containsKey);
      final UName name;
      if (initiatedSym == null) name = UName.mk(nameSeq.next());
      else name = initiatedNames.get(initiatedSym);
      initiatedNames.put(sym, name);
      return name;
    }

    private UVar mkFreshVar(int schema) {
      /* Create a variable with distinct name and given schema. */
      final UVar var = UVar.mkBase(UName.mk(varSeq.next()));
      result.varSchemas.put(var, schema);
      return var;
    }

    private UVar mkVisibleVar() {
      /*  <!> This feature is for dependent subquery <!>
       * Visible variable is the concat of the free variable in current scope
       * and auxiliary variables from outer scope.*/
      final UVar freeVar = tail(freeVars);
      assert freeVar != null;
      if (!SUPPORT_DEPENDENT_SUBQUERY) return freeVar;
      if (auxVar == null) return freeVar;

      final UVar visibleVar = mkConcat(auxVar, freeVar);
      result.varSchemas.put(visibleVar, schemaOf(auxVar) | schemaOf(freeVar));
      return visibleVar;
    }

    private int mkSchema(Symbol /* Table or Attrs */ sym) {
      /* An integer that distinguishes the schema of a relation/tuple.
       * For tables at the source side, each T_i is assigned with 2^i.
       * Tables at the target side are assigned the same as the instantiation source.
       * Tuple concat(x1,x2) is assigned with schemaOf(x1) | schemaOf(x2) */
      final int existing = result.symSchemas.get(sym);
      if (existing != 0) return existing;

      final int ret;
      if (isTargetSide) {
        ret = result.symSchemas.get(rule.constraints().instantiationSourceOf(sym));
      } else {
        ret = nextSchema;
        nextSchema <<= 1;
        if (nextSchema <= 0) throw new IllegalStateException("too much schema"); // At most 31.
      }

      result.symSchemas.put(sym, ret);
      return ret;
    }

    private TableDesc mkTableDesc(Symbol tableSym) {
      // Each Table at the source side corresponds to a distinct desc.
      // Each Table at the target side shares the desc of its instantiation.
      if (!isTargetSide) {
        final UName name = mkName(tableSym, tableSeq);
        final int schema = mkSchema(tableSym);
        final UVar var = mkFreshVar(schema);
        final UTable tableTerm = UTable.mk(name, var);
        final TableDesc desc = new TableDesc(tableTerm, schema);
        result.symToTable.put(tableSym, desc);
        return desc;
      } else {
        final Symbol instantiationSource = rule.constraints().instantiationSourceOf(tableSym);
        final TableDesc desc = result.symToTable.get(instantiationSource);
        assert desc != null;
        result.symToTable.put(tableSym, desc);
        return desc;
      }
    }

    private AttrsDesc mkAttrDesc(Symbol attrSym) {
      // The congruent Attrs (i.e., identically named) share a desc instance.
      final UName name = mkName(attrSym, attrsSeq);
      final AttrsDesc existed = linearFind(result.symToAttrs.values(), it -> it.name.equals(name));
      if (existed != null) return existed;

      final AttrsDesc desc = new AttrsDesc(name);
      result.symToAttrs.put(attrSym, desc);
      return desc;
    }

    private PredDesc mkPredDesc(Symbol predSym) {
      // The congruent Pred (i.e., identically named) share a desc instance.
      final UName name = mkName(predSym, predSeq);
      final PredDesc existed = linearFind(result.symToPred.values(), it -> it.name().equals(name));
      if (existed != null) return existed;

      final PredDesc desc = new PredDesc(name);
      result.symToPred.put(predSym, desc);
      return desc;
    }

    private int schemaOf(UVar var) {
      return result.varSchemas.get(var);
    }

    private UTerm tr(Op op) {
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
        case LEFT_JOIN:
          return trJoin((Join) op);
        default:
          throw new IllegalArgumentException("unknown op");
      }
    }

    private UTerm trInput(Input input) {
      /* Input(T) --> T(x) */
      final TableDesc desc = mkTableDesc(input.table());
      final UVar freeVar = desc.term().var();
      push(freeVars, freeVar);
      result.varSchemas.put(freeVar, desc.schema());
      return UMul.mk(desc.term());
    }

    private UTerm trSimpleFilter(SimpleFilter filter) {
      /* Filter(p,a) --> E * [p(a(x))] */
      final UTerm predecessor = tr(filter.predecessors()[0]);
      final AttrsDesc attrDesc = mkAttrDesc(filter.attrs());
      final PredDesc predDesc = mkPredDesc(filter.predicate());
      final UVar visibleVar = mkVisibleVar();
      final UVar projVar = mkProj(attrDesc.name(), visibleVar);
      final UVar booleanVar = mkFunc(predDesc.name(), projVar);
      attrDesc.addProjectedVar(projVar, schemaOf(visibleVar));
      return UMul.mk(predecessor, UPred.mk(booleanVar));
    }

    private UTerm trInSubFilter(InSubFilter filter) {
      final UTerm lhs = tr(filter.predecessors()[0]);
      final UVar lhsFreeVar = tail(freeVars);

      auxVar = lhsFreeVar;
      final UTerm rhs = tr(filter.predecessors()[1]);
      final UVar rhsFreeVar = pop(freeVars);
      auxVar = null;

      assert lhsFreeVar != null && rhsFreeVar != null;

      final AttrsDesc attrsDesc = mkAttrDesc(filter.attrs());
      final UVar lhsProjVar = mkProj(attrsDesc.name(), lhsFreeVar);
      final UVar eqVar = mkEq(lhsProjVar, rhsFreeVar);
      attrsDesc.addProjectedVar(lhsProjVar, schemaOf(lhsFreeVar));

      return UMul.mk(lhs, USquash.mk(UMul.mk(rhs, UPred.mk(eqVar))));
    }

    private UTerm trExistsFilter(ExistsFilter filter) {
      final UTerm lhs = tr(filter.predecessors()[0]);

      auxVar = tail(freeVars);
      final UTerm rhs = tr(filter.predecessors()[1]);
      auxVar = null;

      return UMul.mk(lhs, USquash.mk(rhs));
    }

    private UTerm trProj(Proj proj) {
      final UTerm predecessor = tr(proj.predecessors()[0]);
      final UVar oldFreeVar = pop(freeVars); // Future: Take care of input-less subquery.
      assert oldFreeVar != null;

      final AttrsDesc attrDesc = mkAttrDesc(proj.attrs());
      final UVar projVar = mkProj(attrDesc.name, oldFreeVar);
      final int schema = mkSchema(proj.attrs());
      final UVar newFreeVar = mkFreshVar(schema);
      final UVar eqVar = mkEq(newFreeVar, projVar);
      push(freeVars, newFreeVar);
      attrDesc.addProjectedVar(projVar, schemaOf(oldFreeVar));
      result.varSchemas.put(projVar, schema);
      result.varSchemas.put(newFreeVar, schema);
      final USum sum =
          USum.mk(Sets.newHashSet(oldFreeVar.argument()), UMul.mk(UPred.mk(eqVar), predecessor));
      return proj.isDeduplicated() ? USquash.mk(sum) : sum;
    }

    private UTerm trJoin(Join join) {
      final UTerm lhs = tr(join.predecessors()[0]);
      final UTerm rhs = tr(join.predecessors()[1]);
      final UVar rhsFreeVar = pop(freeVars);
      final UVar lhsFreeVar = pop(freeVars);
      assert rhsFreeVar != null;
      assert lhsFreeVar != null;

      final int lhsSchema = schemaOf(lhsFreeVar);
      final int rhsSchema = schemaOf(rhsFreeVar);
      final UVar joinedVar = mkConcat(lhsFreeVar, rhsFreeVar);
      final int joinedSchema = lhsSchema | rhsSchema;
      push(freeVars, joinedVar);
      result.varSchemas.put(joinedVar, joinedSchema);

      final AttrsDesc lhsAttrsDesc = mkAttrDesc(join.lhsAttrs());
      final AttrsDesc rhsAttrsDesc = mkAttrDesc(join.rhsAttrs());
      final UVar lhsProjVar = mkProj(lhsAttrsDesc.name(), lhsFreeVar);
      final UVar rhsProjVar = mkProj(rhsAttrsDesc.name(), rhsFreeVar);
      lhsAttrsDesc.addProjectedVar(lhsProjVar, lhsSchema);
      rhsAttrsDesc.addProjectedVar(rhsProjVar, rhsSchema);

      final UTerm eqCond = UPred.mk(mkEq(lhsProjVar, rhsProjVar));
      final UTerm notNullCond = mkNotNull(rhsFreeVar);
      final UMul symm = UMul.mk(lhs, rhs, eqCond, notNullCond);
      if (join.kind() == INNER_JOIN) return symm;

      UTerm newExpr = UMul.mk(rhs, eqCond, notNullCond);
      final Set<UVar> newVars = new HashSet<>(rhsFreeVar.argument().length);
      for (UVar oldVar : rhsFreeVar.argument()) {
        final UVar newVar = mkBase(UName.mk(varSeq.next()));
        newExpr = newExpr.replaceBaseVar(oldVar, newVar);
        newVars.add(newVar);
        result.varSchemas.put(newVar, schemaOf(oldVar));
      }

      final UMul asymm = UMul.mk(lhs, mkIsNull(rhsFreeVar), UNeg.mk(USum.mk(newVars, newExpr)));
      return UAdd.mk(symm, asymm);
    }
  }
}
