package sjtu.ipads.wtune.superopt.uexpr;

import sjtu.ipads.wtune.common.utils.NameSequence;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.*;

import static java.lang.Integer.bitCount;
import static sjtu.ipads.wtune.common.utils.Commons.push;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.common.utils.ListSupport.pop;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.Unique;
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
    private final List<UVar> viableVars; // Viable variable in current scope.
    private UVar auxVar; // Auxiliary variable from outer query.

    private TemplateTranslator(Fragment template, boolean isTargetSide) {
      this.template = template;
      this.isTargetSide = isTargetSide;
      this.freeVars = new ArrayList<>(3);
      this.viableVars = new ArrayList<>(3);
      this.auxVar = null;
    }

    private void translate() {
      final UTerm expr = normalizeExpr(tr(template.root()));
      final UVar outVar = tail(viableVars);
      assert freeVars.size() == 1;
      assert viableVars.size() == 1;
      assert auxVar == null;

      if (!isTargetSide) {
        result.srcExpr = expr;
        result.srcOutVar = outVar;
      } else {
        result.tgtExpr = expr;
        result.tgtOutVar = outVar;
      }
    }

    private UName mkName(Symbol sym, NameSequence nameSeq) {
      /* Create a new or retrieve an existing name for a symbol. */
      final UName name;
      if (!isTargetSide) {
        final Set<Symbol> eqClass = rule.constraints().eqSymbols().eqClassOf(sym);
        final Symbol initiatedSym = linearFind(eqClass, initiatedNames::containsKey);
        if (initiatedSym == null) name = UName.mk(nameSeq.next());
        else name = initiatedNames.get(initiatedSym);
        initiatedNames.put(sym, name);
      } else {
        final Symbol instantiation = rule.constraints().instantiationOf(sym);
        name = initiatedNames.get(instantiation);
      }
      assert name != null;
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
      final UVar var = tail(viableVars);
      assert var != null;
      if (!SUPPORT_DEPENDENT_SUBQUERY) return var;
      if (auxVar == null) return var;

      final UVar visibleVar = mkConcat(auxVar, var);
      result.varSchemas.put(visibleVar, schemaOf(auxVar) | schemaOf(var));
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
        ret = result.symSchemas.get(rule.constraints().instantiationOf(sym));
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
        final Symbol instantiationSource = rule.constraints().instantiationOf(tableSym);
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
      if (existed != null) {
        result.symToAttrs.put(attrSym, existed);
        return existed;
      }

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

    private UVar mkProj(Symbol attrSym, AttrsDesc desc, UVar base) {
      // project `attrSym` (whose desc is `desc`) on the `base` tuple
      if (isTargetSide) attrSym = rule.constraints().instantiationOf(attrSym);
      assert attrSym != null;

      Symbol source = rule.constraints().sourceOf(attrSym);
      rule.constraints().sourceOf(attrSym);
      assert source != null;

      // apply AttrsSub: pick the component from concat (if there is)
      // Suppose we have concat(x,y), where x from T and y from R, and AttrsSub(a,R).
      // then a(concat(x,y)) becomes a(y).
      final int schema = schemaOf(base);
      int restriction = mkSchema(source);

      // indirection source cases.
      //  e.g. Proj<a0>(Proj<a1>(t0)) vs. Proj<a2>(t0)
      //       AttrsSub(a1,t0) /\ AttrsSub(a0,a1) /\ AttrsEq(a2,a0)
      //  In this case, a2 see a tuple of schema t0, while a1 see a tuple of schema a1.
      //  We have to further trace the source of a1.
      while ((schema & restriction) == 0) {
        source = rule.constraints().sourceOf(source);
        assert source != null : "bug in instantiation enum!";
        restriction = mkSchema(source);
      }

      final int partIndex = bitCount(schema & (restriction - 1));
      final UVar restrictedVar = base.args()[partIndex];
      // Note: there can be AttrsSub(a,b), where b is another attrs.
      // Then a(b(x)) becomes a(x).
      if (restrictedVar.kind() != VarKind.PROJ) return UVar.mkProj(desc.name, restrictedVar);
      else return UVar.mkProj(desc.name, restrictedVar.args()[0]);
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
      final UVar var = desc.term().var();
      push(freeVars, var);
      push(viableVars, var);
      result.varSchemas.put(var, desc.schema());
      return UMul.mk(desc.term());
    }

    private UTerm trSimpleFilter(SimpleFilter filter) {
      /* Filter(p,a) --> E * [p(a(x))] */
      final UTerm predecessor = tr(filter.predecessors()[0]);
      final AttrsDesc attrDesc = mkAttrDesc(filter.attrs());
      final PredDesc predDesc = mkPredDesc(filter.predicate());
      final UVar visibleVar = mkVisibleVar();
      final UVar projVar = mkProj(filter.attrs(), attrDesc, visibleVar);
      final UVar booleanVar = mkFunc(predDesc.name(), projVar);
      attrDesc.addProjectedVar(projVar, schemaOf(visibleVar));
      return UMul.mk(predecessor, UPred.mk(booleanVar));
    }

    private UTerm trInSubFilter(InSubFilter filter) {
      final UTerm lhs = tr(filter.predecessors()[0]);
      final UVar lhsViableVar = tail(viableVars);
      assert lhsViableVar != null;

      auxVar = lhsViableVar;
      final UTerm rhs = tr(filter.predecessors()[1]);
      auxVar = null;

      final UVar rhsViableVar = pop(viableVars);
      final UVar rhsFreeVar = pop(freeVars);
      assert rhsViableVar != null && rhsFreeVar != null;

      final AttrsDesc attrsDesc = mkAttrDesc(filter.attrs());
      final UVar lhsProjVar = mkProj(filter.attrs(), attrsDesc, lhsViableVar);
      final UTerm eqVar = UPred.mk(mkEq(lhsProjVar, rhsViableVar));
      final UTerm notNull = mkNotNull(rhsViableVar);
      attrsDesc.addProjectedVar(lhsProjVar, schemaOf(lhsViableVar));

      UTerm decoratedRhs = UMul.mk(eqVar, notNull, rhs);

      final boolean needSum = rhsViableVar.kind() != VarKind.PROJ;
      if (needSum) decoratedRhs = USum.mk(getBaseVars(rhsFreeVar), decoratedRhs);

      final boolean needSquash = !canCoalesceSquash(filter.predecessors()[1]);
      if (needSquash) decoratedRhs = USquash.mk(decoratedRhs);

      if (!needSum && !needSquash) {
        final UVar lhsFreeVar = pop(freeVars);
        assert lhsFreeVar != null;
        push(freeVars, mkConcat(lhsFreeVar, rhsFreeVar));
      }

      return UMul.mk(lhs, decoratedRhs);
    }

    private UTerm trExistsFilter(ExistsFilter filter) {
      final UTerm lhs = tr(filter.predecessors()[0]);
      final UVar lhsFreeVars = pop(freeVars);
      assert lhsFreeVars != null;

      auxVar = tail(viableVars);
      final UTerm rhs = tr(filter.predecessors()[1]);
      auxVar = null;

      final UVar rhsViableVars = pop(viableVars);
      final UVar rhsFreeVars = pop(freeVars);
      assert rhsViableVars != null && rhsFreeVars != null;
      push(freeVars, mkConcat(lhsFreeVars, rhsFreeVars));

      return UMul.mk(lhs, USquash.mk(rhs));
    }

    private UTerm trJoin(Join join) {
      final UTerm lhs = tr(join.predecessors()[0]);
      final UTerm rhs = tr(join.predecessors()[1]);
      final UVar rhsViableVar = pop(viableVars);
      final UVar lhsViableVar = pop(viableVars);
      final UVar rhsFreeVar = pop(freeVars);
      final UVar lhsFreeVar = pop(freeVars);
      assert rhsViableVar != null && rhsFreeVar != null;
      assert lhsViableVar != null && lhsFreeVar != null;

      final int lhsSchema = schemaOf(lhsViableVar);
      final int rhsSchema = schemaOf(rhsViableVar);
      final UVar joinedVar = mkConcat(lhsViableVar, rhsViableVar);
      final int joinedSchema = lhsSchema | rhsSchema;
      push(viableVars, joinedVar);
      push(freeVars, mkConcat(lhsFreeVar, rhsFreeVar));
      result.varSchemas.put(joinedVar, joinedSchema);

      final AttrsDesc lhsAttrsDesc = mkAttrDesc(join.lhsAttrs());
      final AttrsDesc rhsAttrsDesc = mkAttrDesc(join.rhsAttrs());
      final UVar lhsProjVar = mkProj(join.lhsAttrs(), lhsAttrsDesc, lhsViableVar);
      final UVar rhsProjVar = mkProj(join.rhsAttrs(), rhsAttrsDesc, rhsViableVar);
      lhsAttrsDesc.addProjectedVar(lhsProjVar, lhsSchema);
      rhsAttrsDesc.addProjectedVar(rhsProjVar, rhsSchema);

      final UTerm eqCond = UPred.mk(mkEq(lhsProjVar, rhsProjVar));
      final UTerm notNullCond = mkNotNull(rhsProjVar);
      if (join.kind() == INNER_JOIN) return UMul.mk(lhs, eqCond, notNullCond, rhs);

      // left join
      UTerm newExpr = UMul.mk(rhs, eqCond, notNullCond);
      final Set<UVar> freeVars = getBaseVars(rhsFreeVar);
      final Set<UVar> newVars = new HashSet<>(freeVars.size());
      for (UVar oldVar : freeVars) {
        final UVar newVar = mkBase(UName.mk(varSeq.next()));
        newExpr = newExpr.replaceBaseVar(oldVar, newVar);
        newVars.add(newVar);
        result.varSchemas.put(newVar, schemaOf(oldVar));
      }

      final UMul symm = UMul.mk(rhs, eqCond, notNullCond);
      final UMul asymm = UMul.mk(mkIsNull(rhsViableVar), UNeg.mk(USum.mk(newVars, newExpr)));
      return UMul.mk(lhs, UAdd.mk(symm, asymm));
    }

    private UTerm trProj(Proj proj) {
      final UTerm predecessor = tr(proj.predecessors()[0]);
      final UVar viableVar = pop(viableVars);
      final UVar freeVar = pop(freeVars);
      assert viableVar != null && freeVar != null;

      final AttrsDesc attrDesc = mkAttrDesc(proj.attrs());
      final UVar projVar = mkProj(proj.attrs(), attrDesc, viableVar);
      final int schema = mkSchema(proj.attrs());
      final boolean isClosed = needNewFreeVar(proj);

      final UVar newVar = isClosed ? mkFreshVar(schema) : projVar;
      push(freeVars, newVar);
      push(viableVars, newVar);

      attrDesc.addProjectedVar(projVar, schemaOf(viableVar));
      result.varSchemas.put(projVar, schema);
      result.varSchemas.put(newVar, schema);

      if (isClosed) {
        final UPred eq = UPred.mk(mkEq(newVar, projVar));
        final USum s = USum.mk(getBaseVars(freeVar), UMul.mk(eq, predecessor));
        if (proj.isDeduplicated() && !isTreatedAsSet(proj) && !canCoalesceSquash(proj)) {
          return USquash.mk(s);
        } else {
          return s;
        }

      } else {
        return USum.mk(getBaseVars(freeVar), predecessor);
      }
    }

    private boolean needNewFreeVar(Proj proj) {
      return isOutputProj(proj) // 1. directly affects the output
          // 2. The projection need explicit deduplication
          || (proj.isDeduplicated() && !isTreatedAsSet(proj) && !canCoalesceSquash(proj));
    }

    private boolean isOutputProj(Proj proj) {
      Op op = proj;
      Op succ = op.successor();
      while (succ != null) {
        final OperatorType succKind = succ.kind();
        if (succKind == PROJ) return false;
        if (succKind.isSubquery() && succ.predecessors()[1] == op) return false;
        op = succ;
        succ = succ.successor();
      }
      return true;
    }

    private boolean isTreatedAsSet(Proj proj) {
      Op op = proj;
      Op succ = op.successor();
      while (succ != null) {
        final OperatorType succKind = succ.kind();
        if (succKind == PROJ && ((Proj) succ).isDeduplicated()) return true;
        if (succKind.isSubquery() && succ.predecessors()[1] == op) return true;
        op = succ;
        succ = succ.successor();
      }
      return false;
    }

    // Check if |Tr(op)| == Tr(op)
    private boolean canCoalesceSquash(Op op) {
      // Principle: if the output of the op contains unique key, then no need to be squashed.
      if (op.kind().isFilter()) return canCoalesceSquash(op.predecessors()[0]);

      if (op.kind().isJoin())
        return canCoalesceSquash(op.predecessors()[0]) && canCoalesceSquash(op.predecessors()[1]);

      if (op.kind() == PROJ) {
        return isUniqueCoreIn(op.predecessors()[0], ((Proj) op).attrs());
      }

      if (op.kind() == INPUT) {
        // Check if any attrs of this table is unique.
        final Symbol sym = ((Input) op).table();
        final Constraints C = rule.constraints();
        final Symbol table = isTargetSide ? C.instantiationOf(sym) : sym;
        return any(C.ofKind(Unique), it -> C.isEq(table, it.symbols()[0]));
      }

      return false;
    }

    private boolean isUniqueKey(Symbol attrs) {
      return any(rule.constraints().ofKind(Unique), uk -> uk.symbols()[1] == attrs);
    }

    private boolean isUniqueCoreIn(Op op, Symbol attrs) {
      final OperatorType kind = op.kind();
      if (kind.isFilter()) return isUniqueCoreIn(op.predecessors()[0], attrs);

      if (kind == INPUT) {
        final Constraints C = rule.constraints();
        Symbol table = ((Input) op).table();
        if (isTargetSide) {
          attrs = C.instantiationOf(attrs);
          table = C.instantiationOf(table);
        }
        return C.sourceOf(attrs) == table && isUniqueKey(attrs);
      }

      if (kind.isJoin()) {
        final Join join = (Join) op;
        if (isUniqueCoreIn(op.predecessors()[0], attrs)) {
          return isUniqueCoreIn(op.predecessors()[1], join.rhsAttrs());
        } else if (isUniqueCoreIn(op.predecessors()[1], attrs)) {
          return isUniqueCoreIn(op.predecessors()[0], join.lhsAttrs());
        } else {
          return false;
        }
      }

      if (kind == PROJ) {
        final Proj proj = (Proj) op;
        return rule.constraints().isEq(attrs, proj.attrs())
            && isUniqueCoreIn(proj.predecessors()[0], proj.attrs());
      }

      assert false;
      return false;
    }
  }
}
