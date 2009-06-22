package dr.evomodel.beagle.parsers;

import dr.evomodel.beagle.substmodel.FrequencyModel;
import dr.evomodel.beagle.substmodel.GTR;
import dr.inference.model.Parameter;
import dr.xml.*;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class GTRParser extends AbstractXMLObjectParser {


    public static final String GTR_MODEL = "gtrModel";

    public static final String A_TO_C = "rateAC";
    public static final String A_TO_G = "rateAG";
    public static final String A_TO_T = "rateAT";
    public static final String C_TO_G = "rateCG";
    public static final String C_TO_T = "rateCT";
    public static final String G_TO_T = "rateGT";

    public static final String FREQUENCIES = "frequencies";

    public String getParserName() {
        return GTR_MODEL;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        XMLObject cxo = xo.getChild(FREQUENCIES);
        FrequencyModel freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);

        Parameter rateACParameter = null;
        if (xo.hasChildNamed(A_TO_C)) {
            rateACParameter = (Parameter) xo.getElementFirstChild(A_TO_C);
        }
        Parameter rateAGParameter = null;
        if (xo.hasChildNamed(A_TO_G)) {
            rateAGParameter = (Parameter) xo.getElementFirstChild(A_TO_G);
        }
        Parameter rateATParameter = null;
        if (xo.hasChildNamed(A_TO_T)) {
            rateATParameter = (Parameter) xo.getElementFirstChild(A_TO_T);
        }
        Parameter rateCGParameter = null;
        if (xo.hasChildNamed(C_TO_G)) {
            rateCGParameter = (Parameter) xo.getElementFirstChild(C_TO_G);
        }
        Parameter rateCTParameter = null;
        if (xo.hasChildNamed(C_TO_T)) {
            rateCTParameter = (Parameter) xo.getElementFirstChild(C_TO_T);
        }
        Parameter rateGTParameter = null;
        if (xo.hasChildNamed(G_TO_T)) {
            rateGTParameter = (Parameter) xo.getElementFirstChild(G_TO_T);
        }
        int countNull = 0;
        if (rateACParameter == null) countNull++;
        if (rateAGParameter == null) countNull++;
        if (rateATParameter == null) countNull++;
        if (rateCGParameter == null) countNull++;
        if (rateCTParameter == null) countNull++;
        if (rateGTParameter == null) countNull++;

        if (countNull != 1)
            throw new XMLParseException("Only five parameters may be specified in GTR, leave exactly one out, the others will be specifed relative to the one left out.");
        return new GTR(rateACParameter, rateAGParameter, rateATParameter, rateCGParameter, rateCTParameter, rateGTParameter, freqModel);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "A general reversible model of nucleotide sequence substitution.";
    }

    public String getExample() {

        return
                "<!-- A general time reversible model for DNA.                                          -->\n" +
                        "<!-- This element must have parameters for exactly five of the six rates               -->\n" +
                        "<!-- The sixth rate has an implied value of 1.0 and all other rates are relative to it -->\n" +
                        "<!-- This example parameterizes the rate matrix relative to the A<->G transition       -->\n" +
                        "<" + getParserName() + " id=\"gtr1\">\n" +
                        "	<" + FREQUENCIES + "> <frequencyModel idref=\"freqs\"/> </" + FREQUENCIES + ">\n" +
                        "	<" + A_TO_C + "> <parameter id=\"rateAC\" value=\"1.0\"/> </" + A_TO_C + ">\n" +
                        "	<" + A_TO_T + "> <parameter id=\"rateAT\" value=\"1.0\"/> </" + A_TO_T + ">\n" +
                        "	<" + C_TO_G + "> <parameter id=\"rateCG\" value=\"1.0\"/> </" + C_TO_G + ">\n" +
                        "	<" + C_TO_T + "> <parameter id=\"rateCT\" value=\"1.0\"/> </" + C_TO_T + ">\n" +
                        "	<" + G_TO_T + "> <parameter id=\"rateGT\" value=\"1.0\"/> </" + G_TO_T + ">\n" +
                        "</" + getParserName() + ">\n";
    }

    public Class getReturnType() {
        return GTR.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            new ElementRule(FREQUENCIES,
                    new XMLSyntaxRule[]{new ElementRule(FrequencyModel.class)}),
            new ElementRule(A_TO_C,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
            new ElementRule(A_TO_G,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
            new ElementRule(A_TO_T,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
            new ElementRule(C_TO_G,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
            new ElementRule(C_TO_T,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
            new ElementRule(G_TO_T,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true)
    };
}
