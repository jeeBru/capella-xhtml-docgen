//Generated with EGF 1.6.3.202110291409
package org.polarsys.capella.docgen.sidebar;

import org.eclipse.egf.common.helper.*;
import java.util.*;
import org.eclipse.emf.ecore.*;
import org.eclipse.egf.model.pattern.*;
import org.eclipse.egf.pattern.execution.*;
import org.eclipse.egf.pattern.query.*;
import org.polarsys.capella.core.data.capellacore.CapellaElement;
import org.polarsys.capella.docgen.util.DocGenHtmlCapellaControl;
import org.polarsys.capella.docgen.util.DocGenHtmlCapellaUtil;;

public class AnyNamedElementSideBar extends org.polarsys.kitalpha.doc.gen.business.core.doccontent.ElementSideBarContent {
    protected static String nl;

    public static synchronized AnyNamedElementSideBar create(String lineSeparator) {
        nl = lineSeparator;
        AnyNamedElementSideBar result = new AnyNamedElementSideBar();
        nl = null;
        return result;
    }

    public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;

    protected final String TEXT_1 = "";

    protected final String TEXT_2 = NL;

    public AnyNamedElementSideBar() {
        //Here is the constructor
        StringBuffer stringBuffer = new StringBuffer();

        // add initialisation of the pattern variables (declaration has been already done).

    }

    public String generate(Object argument) throws Exception {
        final StringBuffer stringBuffer = new StringBuffer();

        InternalPatternContext ctx = (InternalPatternContext) argument;
        Map<String, String> queryCtx = null;
        IQuery.ParameterDescription paramDesc = null;
        Node.Container currentNode = ctx.getNode();

        List<Object> parameterList = null;
        //this pattern can only be called by another (i.e. it's not an entry point in execution)

        for (Object parameterParameter : parameterList) {

            this.parameter = (org.polarsys.capella.core.data.capellacore.CapellaElement) parameterParameter;

            if (preCondition(ctx)) {
                ctx.setNode(new Node.Container(currentNode, getClass()));
                orchestration(ctx);
            }

        }
        ctx.setNode(currentNode);
        if (ctx.useReporter()) {
            ctx.getReporter().executionFinished(OutputManager.computeExecutionOutput(ctx), ctx);
        }

        stringBuffer.append(TEXT_1);
        stringBuffer.append(TEXT_2);
        return stringBuffer.toString();
    }

    public String orchestration(PatternContext ctx) throws Exception {
        InternalPatternContext ictx = (InternalPatternContext) ctx;

        super.orchestration(new SuperOrchestrationContext(ictx));

        if (ictx.useReporter()) {
            Map<String, Object> parameterValues = new HashMap<String, Object>();
            parameterValues.put("parameter", this.parameter);
            String outputWithCallBack = OutputManager.computeLoopOutput(ictx);
            String loop = OutputManager.computeLoopOutputWithoutCallback(ictx);
            ictx.getReporter().loopFinished(loop, outputWithCallBack, ictx, parameterValues);
        }
        return null;
    }

    protected org.polarsys.capella.core.data.capellacore.CapellaElement parameter = null;

    public void set_parameter(org.polarsys.capella.core.data.capellacore.CapellaElement object) {
        this.parameter = object;
    }

    public Map<String, Object> getParameters() {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("parameter", this.parameter);
        return parameters;
    }

    protected void method_setCurrentObject(final StringBuffer stringBuffer, final PatternContext ctx) throws Exception {

        currentObject = parameter;
        InternalPatternContext ictx = (InternalPatternContext) ctx;
        new Node.DataLeaf(ictx.getNode(), getClass(), "setCurrentObject", stringBuffer.toString());
    }

    protected void method_setFileNameService(final StringBuffer stringBuffer, final PatternContext ctx) throws Exception {

        fileNameService = DocGenHtmlCapellaUtil.SERVICE;

        InternalPatternContext ictx = (InternalPatternContext) ctx;
        new Node.DataLeaf(ictx.getNode(), getClass(), "setFileNameService", stringBuffer.toString());
    }

    protected void method_startSidebarSubElement(final StringBuffer stringBuffer, final PatternContext ctx) throws Exception {

        if (DocGenHtmlCapellaUtil.hasChildren(parameter)) {
            super.method_startSidebarSubElement(new StringBuffer(), ctx);

        }
        InternalPatternContext ictx = (InternalPatternContext) ctx;
        new Node.DataLeaf(ictx.getNode(), getClass(), "startSidebarSubElement", stringBuffer.toString());
    }

    protected void method_endSidebarSubElement(final StringBuffer stringBuffer, final PatternContext ctx) throws Exception {

        if (DocGenHtmlCapellaUtil.hasChildren(parameter)) {
            super.method_endSidebarSubElement(new StringBuffer(), ctx);

        }
        InternalPatternContext ictx = (InternalPatternContext) ctx;
        new Node.DataLeaf(ictx.getNode(), getClass(), "endSidebarSubElement", stringBuffer.toString());
    }

    public boolean preCondition(PatternContext ctx) throws Exception {
        return DocGenHtmlCapellaControl.isPageCandidate((CapellaElement) parameter);
    }
}
