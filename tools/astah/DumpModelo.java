import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.model.*;
import java.util.*;

public class DumpModelo {
    static StringBuilder out = new StringBuilder();
    static void p(String s) { out.append(s).append('\n'); }
    static String n(Object o) {
        if (o == null) return "?";
        if (o instanceof INamedElement ne) {
            String s = ne.getName();
            return (s == null || s.isBlank()) ? "<" + o.getClass().getSimpleName().replace("Imp", "") + ">" : s;
        }
        return String.valueOf(o);
    }
    static String st(IElement e) {
        String[] s = e.getStereotypes();
        return s.length == 0 ? "" : "<<" + String.join(",", s) + ">> ";
    }

    public static void main(String[] args) throws Exception {
        ProjectAccessor pa = AstahAPI.getAstahAPI().getProjectAccessor();
        pa.open(args[0], true, false, true);
        IModel root = pa.getProject();
        walk(root, "");
        for (IDiagram d : allDiagrams(root)) diagram(d);
        pa.close();
        System.out.print(out);
    }

    static List<IDiagram> allDiagrams(INamedElement e) {
        List<IDiagram> l = new ArrayList<>(Arrays.asList(e.getDiagrams()));
        if (e instanceof IPackage pk) for (INamedElement c : pk.getOwnedElements()) l.addAll(allDiagrams(c));
        if (e instanceof IClass c) for (IClass nc : c.getNestedClasses()) l.addAll(allDiagrams(nc));
        return l;
    }

    static void walk(INamedElement e, String ind) {
        if (e instanceof IAssociation || e instanceof IDiagram) return;
        String kind = e.getClass().getSimpleName().replace("Imp", "");
        p(ind + kind + " " + st(e) + n(e) + (e instanceof IClass c && c.isAbstract() ? " {abstract}" : ""));
        if (e instanceof IClass c) {
            for (IAttribute a : c.getAttributes()) {
                if (a.getAssociation() != null) {
                    IAssociation as = a.getAssociation();
                    IAttribute other = null;
                    for (IAttribute end : as.getMemberEnds()) if (end != a) other = end;
                    if (other != null)
                        p(ind + "  assoc " + st(as) + "'" + n(as) + "' -> " + n(other.getType()) + " role=" + n(other)
                            + " mult=" + mult(other) + " agg=" + (a.isComposite() ? "composite" : a.isAggregate() ? "aggregate" : "none")
                            + " navigable=" + other.getNavigability());
                } else {
                    p(ind + "  attr " + vis(a) + " " + n(a) + " : " + a.getTypeExpression()
                        + (a.getInitialValue() != null && !a.getInitialValue().isEmpty() ? " = " + a.getInitialValue() : ""));
                }
            }
            for (IOperation o : c.getOperations()) {
                StringBuilder ps = new StringBuilder();
                for (IParameter pr : o.getParameters()) ps.append(ps.length() > 0 ? ", " : "").append(pr.getName()).append(" : ").append(pr.getTypeExpression());
                p(ind + "  op " + vis(o) + " " + st(o) + o.getName() + "(" + ps + ") : " + o.getReturnTypeExpression() + (o.isAbstract() ? " {abstract}" : ""));
            }
            for (IGeneralization g : c.getGeneralizations()) p(ind + "  extends " + n(g.getSuperType()));
            for (IRealization r : c.getClientRealizations()) p(ind + "  realizes " + n(r.getSupplier()));
            for (IUsage u : c.getClientUsages()) p(ind + "  uses " + n(u.getSupplier()));
            for (IDependency d : c.getClientDependencies()) p(ind + "  depends " + st(d) + "'" + n(d) + "' -> " + n(d.getSupplier()));
            if (c instanceof IComponent) { }
            for (IPort port : c.getPorts()) {
                List<String> pv = new ArrayList<>(), rq = new ArrayList<>();
                for (IClass i : port.getProvidedInterfaces()) pv.add(n(i));
                for (IClass i : port.getRequiredInterfaces()) rq.add(n(i));
                p(ind + "  port " + n(port) + " provided=" + pv + " required=" + rq);
            }
            for (IClass nc : c.getNestedClasses()) walk(nc, ind + "  ");
        }
        if (e instanceof IUseCase uc) {
            for (IInclude i : uc.getIncludes()) p(ind + "  include " + n(i.getAddition()));
            for (IExtend x : uc.getExtends()) p(ind + "  extend " + n(x.getExtendedCase()));
        }
        if (e instanceof IPackage pk) for (INamedElement c : pk.getOwnedElements()) walk(c, ind + "  ");
        if (!(e instanceof IPackage) && !(e instanceof IClass) ) { }
    }

    static String vis(INamedElement e) { return e.isPublicVisibility()?"+":e.isPrivateVisibility()?"-":e.isProtectedVisibility()?"#":"~"; }
    static String mult(IAttribute a) {
        IMultiplicityRange[] r = a.getMultiplicity();
        if (r.length == 0) return "";
        IMultiplicityRange m = r[0];
        String lo = m.getLower() == IMultiplicityRange.UNDEFINED ? "" : (m.getLower() == IMultiplicityRange.UNLIMITED ? "*" : "" + m.getLower());
        String hi = m.getUpper() == IMultiplicityRange.UNDEFINED ? "" : (m.getUpper() == IMultiplicityRange.UNLIMITED ? "*" : "" + m.getUpper());
        return lo.equals(hi) ? lo : lo + ".." + hi;
    }

    static void diagram(IDiagram d) {
        p("\n=== DIAGRAM " + d.getClass().getSimpleName().replace("Imp", "") + " : " + n(d));
        try {
            if (d instanceof IStateMachineDiagram sd) {
                IStateMachine sm = sd.getStateMachine();
                for (IVertex v : sm.getVertexes()) vertex(v, "  ");
                for (ITransition t : sm.getTransitions())
                    p("  T " + n(t.getSource()) + " --> " + n(t.getTarget()) + " : " + t.getEvent() + " [" + t.getGuard() + "] / " + t.getAction());
            } else if (d instanceof IActivityDiagram ad) {
                IActivity a = ad.getActivity();
                for (IPartition pt : a.getPartitions()) partition(pt, "  ");
                for (IActivityNode an : a.getActivityNodes())
                    p("  NODE " + an.getClass().getSimpleName().replace("Imp", "") + " " + st(an) + "'" + n(an).replace('\n', ' ') + "'"
                        + (an instanceof IObjectNode on && on.getBase() != null ? " base=" + n(on.getBase()) : "")
                        + (an instanceof IObjectNode on2 && on2.getInState() != null && !on2.getInState().isEmpty() ? " inState=" + on2.getInState() : ""));
                for (IFlow f : a.getFlows())
                    p("  FLOW " + "'" + n(f.getSource()).replace('\n', ' ') + "' --> '" + n(f.getTarget()).replace('\n', ' ') + "' [" + f.getGuard() + "] " + n(f));
            } else if (d instanceof ISequenceDiagram sq) {
                IInteraction it = sq.getInteraction();
                for (ILifeline l : it.getLifelines()) p("  LIFELINE " + n(l) + " : " + n(l.getBase()) + " " + st(l));
                for (IMessage m : it.getMessages())
                    p("  MSG " + m.getIndex() + " " + n(m.getSource()) + " -> " + n(m.getTarget()) + " : " + (m.isReturnMessage() ? "return " : m.isCreateMessage() ? "create " : m.isAsynchronous() ? "async " : "")
                        + n(m) + "(" + m.getArgument() + ")" + (m.getReturnValue() != null && !m.getReturnValue().isEmpty() ? " => " + m.getReturnValue() : "")
                        + (m.getGuard() != null && !m.getGuard().isEmpty() ? " [" + m.getGuard() + "]" : "")
                        + (m.getOperation() != null ? " {op=" + n(m.getOperation().getOwner()) + "." + n(m.getOperation()) + "}" : ""));
                Set<INamedElement> seen = new LinkedHashSet<>();
                for (ILifeline l : it.getLifelines()) for (INamedElement fr : l.getFragments()) seen.add(fr);
                for (INamedElement fr : seen) if (fr instanceof IInteractionFragment f2) fragment(f2, "  ");
            }
        } catch (Throwable ex) { p("  !! " + ex); }
    }

    static void fragment(IInteractionFragment fr, String ind) {
        if (fr instanceof ICombinedFragment cf) {
            String opk = cf.isAlt()?"alt":cf.isLoop()?"loop":cf.isOpt()?"opt":cf.isPar()?"par":cf.isBreak()?"break":cf.isCritical()?"critical":cf.isNeg()?"neg":"other";
            p(ind + "FRAGMENT " + opk + " " + n(cf));
            for (IInteractionOperand op : cf.getInteractionOperands()) {
                StringBuilder ms = new StringBuilder();
                for (IMessage m : op.getMessages()) ms.append(m.getIndex()).append(' ');
                p(ind + "  operand [" + op.getGuard() + "] msgs=" + ms);
            }
        } else if (fr instanceof IInteractionUse iu) {
            p(ind + "REF " + n(iu));
        }
    }

    static void partition(IPartition pt, String ind) {
        p(ind + "LANE " + n(pt));
        for (IActivityNode an : pt.getActivityNodes()) p(ind + "  . " + n(an).replace('\n', ' '));
        for (IPartition sp : pt.getSubPartitions()) partition(sp, ind + "  ");
    }

    static void vertex(IVertex v, String ind) {
        String kind = v.getClass().getSimpleName().replace("Imp", "");
        String extra = "";
        if (v instanceof IState s) {
            extra = " entry=" + s.getEntry() + " do=" + s.getDoActivity() + " exit=" + s.getExit();
            for (ITransition t : s.getInternalTransitions()) extra += " internal(" + t.getEvent() + "[" + t.getGuard() + "]/" + t.getAction() + ")";
        }
        p(ind + "V " + kind + " '" + n(v) + "'" + extra);
        if (v instanceof IState s) for (IVertex sub : s.getSubvertexes()) vertex(sub, ind + "  ");
    }
}
