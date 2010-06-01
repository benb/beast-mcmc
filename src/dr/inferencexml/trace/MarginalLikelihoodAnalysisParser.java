package dr.inferencexml.trace;

import dr.inference.trace.LogFileTraces;
import dr.inference.trace.MarginalLikelihoodAnalysis;
import dr.inference.trace.Trace;
import dr.inference.trace.TraceException;
import dr.util.Attribute;
import dr.xml.*;

import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 */
public class MarginalLikelihoodAnalysisParser extends AbstractXMLObjectParser {

    public static final String ML_ANALYSIS = "marginalLikelihoodAnalysis";
    public static final String FILE_NAME = "fileName";
    public static final String BURN_IN = "burnIn";
    public static final String COLUMN_NAME = "likelihoodColumn";
    //public static final String DO_BOOTSTRAP = "bootstrap";
    public static final String ONLY_HARMONIC = "harmonicOnly";
    public static final String BOOTSTRAP_LENGTH = "bootstrapLength";

    public String getParserName() {
        return ML_ANALYSIS;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        String fileName = xo.getStringAttribute(FILE_NAME);
        try {

            File file = new File(fileName);
            String name = file.getName();
            String parent = file.getParent();

            if (!file.isAbsolute()) {
                parent = System.getProperty("user.dir");
            }

            file = new File(parent, name);

            fileName = file.getAbsolutePath();

            XMLObject cxo = xo.getChild(COLUMN_NAME);
            String likelihoodName = cxo.getStringAttribute(Attribute.NAME);

            LogFileTraces traces = new LogFileTraces(fileName, file);
            traces.loadTraces();
            int maxState = traces.getMaxState();

            // leaving the burnin attribute off will result in 10% being used
            int burnin = xo.getAttribute(BURN_IN, maxState / 10);

            if (burnin < 0 || burnin >= maxState) {
                burnin = maxState / 10;
                System.out.println("WARNING: Burn-in larger than total number of states - using to 10%");
            }

            traces.setBurnIn(burnin);

            int traceIndex = -1;
            for (int i = 0; i < traces.getTraceCount(); i++) {
                String traceName = traces.getTraceName(i);
                if (traceName.equals(likelihoodName)) {
                    traceIndex = i;
                    break;
                }
            }

            if (traceIndex == -1) {
                throw new XMLParseException("Column '" + likelihoodName + "' can not be found for " + getParserName() + " element.");
            }

            boolean harmonicOnly = false;
            if (cxo.hasAttribute(ONLY_HARMONIC)) {
                harmonicOnly = cxo.getBooleanAttribute(ONLY_HARMONIC);
            }

            int bootstrapLength = cxo.getAttribute(BOOTSTRAP_LENGTH, 1000);

            Double sample[] = new Double[traces.getStateCount()];
            traces.getValues(traceIndex, sample);

            MarginalLikelihoodAnalysis analysis = new MarginalLikelihoodAnalysis(
                    Trace.arrayConvert(sample),
                    traces.getTraceName(traceIndex), burnin,
                    harmonicOnly, bootstrapLength);

            System.out.println(analysis.toString());

            return analysis;

        } catch (FileNotFoundException fnfe) {
            throw new XMLParseException("File '" + fileName + "' can not be opened for " + getParserName() + " element.");
        } catch (java.io.IOException ioe) {
            throw new XMLParseException(ioe.getMessage());
        } catch (TraceException e) {
            throw new XMLParseException(e.getMessage());
        }
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "Performs a trace analysis. Estimates the mean of the various statistics in the given log file.";
    }

    public Class getReturnType() {
        return MarginalLikelihoodAnalysis.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            new StringAttributeRule(FILE_NAME, "The traceName of a BEAST log file (can not include trees, which should be logged separately"),
            AttributeRule.newIntegerRule("burnIn", true),
            //, "The number of states (not sampled states, but actual states) that are discarded from the beginning of the trace before doing the analysis" ),
            new ElementRule(COLUMN_NAME, new XMLSyntaxRule[] {
                    new StringAttributeRule(Attribute.NAME,"The column name")}),
    };

}
