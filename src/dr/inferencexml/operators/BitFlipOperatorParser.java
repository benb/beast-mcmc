package dr.inferencexml.operators;

import dr.inference.model.Parameter;
import dr.inference.operators.BitFlipOperator;
import dr.inference.operators.MCMCOperator;
import dr.xml.*;

/**
 *
 */
public class BitFlipOperatorParser extends AbstractXMLObjectParser {

    public static final String BIT_FLIP_OPERATOR = "bitFlipOperator";
    public static final String BITS = "bits";
    public static final String USES_SUM_PRIOR = "usesPriorOnSum";

    public String getParserName() {
        return BIT_FLIP_OPERATOR;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

        Parameter parameter = (Parameter) xo.getChild(Parameter.class);

        boolean usesPriorOnSum = xo.getAttribute(USES_SUM_PRIOR,true);

        return new BitFlipOperator(parameter, weight, usesPriorOnSum);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "This element returns a bit-flip operator on a given parameter.";
    }

    public Class getReturnType() {
        return BitFlipOperator.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
//                AttributeRule.newIntegerRule(BITS,true),
            AttributeRule.newBooleanRule(USES_SUM_PRIOR,true),
            new ElementRule(Parameter.class)
    };

}
