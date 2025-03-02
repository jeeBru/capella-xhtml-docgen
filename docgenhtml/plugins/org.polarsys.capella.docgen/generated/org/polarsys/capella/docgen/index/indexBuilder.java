//Generated with EGF 1.6.3.202110291409
package org.polarsys.capella.docgen.index;

import org.eclipse.egf.common.helper.*;
import java.util.*;
import org.eclipse.emf.ecore.*;
import org.eclipse.egf.model.pattern.*;
import org.eclipse.egf.pattern.execution.*;
import org.eclipse.egf.pattern.query.*;
import org.eclipse.emf.common.util.EList;
import org.polarsys.capella.docgen.util.DocGenHtmlCapellaUtil;
import org.polarsys.capella.core.data.capellamodeller.ModelRoot;

public class indexBuilder extends org.polarsys.kitalpha.doc.gen.business.core.index.IndexBuilder {
    protected static String nl;

    public static synchronized indexBuilder create(String lineSeparator) {
        nl = lineSeparator;
        indexBuilder result = new indexBuilder();
        nl = null;
        return result;
    }

    public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;

    protected final String TEXT_1 = "  <script type=\"text/javascript\">" + NL + "document.write('<frameset rows=\"63,*,40\" frameborder=\"0\" framespacing=\"0\" border=\"0\">');" + NL
            + "document.write('<frame src=\"header.html\" name=\"header\" marginheight=\"0\" marginwidth=\"0\" scrolling=\"no\" noresize=\"0\"/>');" + NL
            + "document.write('<frameset cols=\"22%,*\" border=\"5\" frameborder=\"1\" framespacing=\"1\">');" + NL + "document.write('<frame src=\"sidebar.html\" name=\"sideBar\"/>');" + NL
            + "var locationText = \"\";" + NL + "var anchorText = \"\";" + NL + "if (location.search) {" + NL + "\tlocationText = location.search.substring(1);" + NL
            + "\tif (location.href.lastIndexOf(\"#\")>0) {" + NL + "\t\tanchorText = anchorText + \"#\" + location.href.substring(location.href.lastIndexOf(\"#\")+1);" + NL + "\t}" + NL + "} else {"
            + NL + "\tlocationText = \"";

    protected final String TEXT_2 = "\";" + NL + "}" + NL + "document.write('<frame src=\"'+ locationText +'.html' + anchorText + '\" name=\"content\"\\/>');" + NL + "document.write('<noframes>');"
            + NL + "document.write('Your browser cannot display this page !');" + NL + "  document.write('</noframes>');" + NL + "document.write('</frameset>');" + NL
            + "document.write('<frame src=\"footer.html\" name=\"footer\" scrolling=\"no\" frameborder=\"0\" noresize=\"noresize\"/>');" + NL + "document.write('<noframes>');" + NL
            + "document.write('Your browser cannot display this page !');" + NL + "document.write('</noframes>');" + NL + "document.write('</frameset>');" + NL + "</script>";

    protected final String TEXT_3 = NL;

    public indexBuilder() {
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

        if (preCondition(ctx)) {
            ctx.setNode(new Node.Container(currentNode, getClass()));
            orchestration(ctx);
        }

        ctx.setNode(currentNode);
        if (ctx.useReporter()) {
            ctx.getReporter().executionFinished(OutputManager.computeExecutionOutput(ctx), ctx);
        }

        stringBuffer.append(TEXT_3);
        stringBuffer.append(TEXT_3);
        return stringBuffer.toString();
    }

    public String orchestration(PatternContext ctx) throws Exception {
        InternalPatternContext ictx = (InternalPatternContext) ctx;

        super.orchestration(new SuperOrchestrationContext(ictx));

        return null;
    }

    public Map<String, Object> getParameters() {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        return parameters;
    }

    protected void method_content(final StringBuffer stringBuffer, final PatternContext ctx) throws Exception {

        List<Object> model = (List<Object>) ctx.getValue(PatternContext.DOMAIN_OBJECTS);
        String fileName = "";
        for (Object currentObject : model) {
            if (currentObject instanceof org.polarsys.capella.core.data.capellamodeller.Project) {
                EList<ModelRoot> children = ((org.polarsys.capella.core.data.capellamodeller.Project) currentObject).getOwnedModelRoots();
                if (children.get(0) instanceof org.polarsys.capella.core.data.capellamodeller.SystemEngineering) {
                    fileName = DocGenHtmlCapellaUtil.SERVICE.getFileName(children.get(0));
                    break;
                }
            }
        }

        stringBuffer.append(TEXT_1);
        stringBuffer.append(fileName);
        stringBuffer.append(TEXT_2);
        InternalPatternContext ictx = (InternalPatternContext) ctx;
        new Node.DataLeaf(ictx.getNode(), getClass(), "content", stringBuffer.toString());
    }
}
