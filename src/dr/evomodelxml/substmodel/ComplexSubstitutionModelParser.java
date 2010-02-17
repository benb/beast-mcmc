package dr.evomodelxml.substmodel;

import dr.evolution.datatype.DataType;
import dr.evomodel.substmodel.ComplexSubstitutionModel;
import dr.evomodel.substmodel.FrequencyModel;
import dr.evomodel.substmodel.SVSComplexSubstitutionModel;
import dr.evomodel.substmodel.SubstitutionModel;
import dr.evoxml.util.DataTypeUtils;
import dr.inference.model.BayesianStochasticSearchVariableSelection;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.logging.Logger;

/**
 */
public class ComplexSubstitutionModelParser extends AbstractXMLObjectParser {

    public static final String COMPLEX_SUBSTITUTION_MODEL = "complexSubstitutionModel";
    public static final String RATES = "rates";
    public static final String ROOT_FREQUENCIES = "rootFrequencies";
    public static final String INDICATOR = "rateIndicator";
    public static final String RANDOMIZE = "randomizeIndicator";
    public static final String NORMALIZATION = "normalize";
    public static final String MAX_CONDITION_NUMBER = "maxConditionNumber";
    public static final String CONNECTED = "mustBeConnected";
    public static final String MAX_ITERATIONS = "maxIterations";
    public static final String CHECK_CONDITIONING = "checkConditioning";

    public String[] getParserNames() {
        return new String[]{
                getParserName(), "beast_" + getParserName()
        };
    }

    public String getParserName() {
        return COMPLEX_SUBSTITUTION_MODEL;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        DataType dataType = DataTypeUtils.getDataType(xo);

        if (dataType == null) dataType = (DataType) xo.getChild(DataType.class);

        XMLObject cxo = xo.getChild(RATES);

        Parameter ratesParameter = (Parameter) cxo.getChild(Parameter.class);

        int rateCount = (dataType.getStateCount() - 1) * dataType.getStateCount();

        if (ratesParameter.getDimension() != rateCount) {
            throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + (rateCount) + " dimensions.  However parameter dimension is " + ratesParameter.getDimension());
        }


        cxo = xo.getChild(ROOT_FREQUENCIES);
        FrequencyModel rootFreq = (FrequencyModel) cxo.getChild(FrequencyModel.class);

        if (dataType != rootFreq.getDataType()) {
            throw new XMLParseException("Data type of " + getParserName() + " element does not match that of its rootFrequencyModel.");
        }

        Parameter indicators = null;

        if (xo.hasChildNamed(INDICATOR)) {
            indicators = (Parameter) ((XMLObject) xo.getChild(INDICATOR)).getChild(Parameter.class);
            if (ratesParameter.getDimension() != indicators.getDimension())
                throw new XMLParseException("Rate parameter dimension must match indicator parameter dimension");
        }

        StringBuffer sb = new StringBuffer().append("Constructing a complex substitution model using\n")
                .append("\tRate parameters: ").append(ratesParameter.getId())
                .append("\n").append("\tRoot frequency model: ").append(rootFreq.getId()).append("\n");

        ComplexSubstitutionModel model;

        if (indicators == null)
            model = new ComplexSubstitutionModel(xo.getId(), dataType, rootFreq, ratesParameter);
        else {

            boolean randomize = xo.getAttribute(RANDOMIZE, false);
            boolean connected = xo.getAttribute(CONNECTED, false);
            model = new SVSComplexSubstitutionModel(xo.getId(), dataType, rootFreq, ratesParameter, indicators, connected);
            if (randomize) {
                BayesianStochasticSearchVariableSelection.Utils.randomize(indicators,
                        dataType.getStateCount(), false);
            }
            sb.append("\tBSSVS indicators: ").append(indicators.getId()).append("\n");
            sb.append("\tGraph must be connected: ").append(connected).append("\n");
        }

        boolean doNormalization = xo.getAttribute(NORMALIZATION, true);
        model.setNormalization(doNormalization);
        sb.append("\tNormalized: ").append(doNormalization).append("\n");

        boolean checkConditioning = xo.getAttribute(CHECK_CONDITIONING, true);
        model.setCheckConditioning(checkConditioning);

        if (checkConditioning) {
            double maxConditionNumber = xo.getAttribute(MAX_CONDITION_NUMBER, 1000);
            model.setMaxConditionNumber(maxConditionNumber);
            sb.append("\tMax. condition number: ").append(maxConditionNumber).append("\n");
        }

        int maxIterations = xo.getAttribute(MAX_ITERATIONS, 1000);
        model.setMaxIterations(maxIterations);
        sb.append("\tMax iterations: ").append(maxIterations).append("\n");

        sb.append("Please cite Lemey, Rambaut, Drummond and Suchard (in submission)\n");

        Logger.getLogger("dr.evomodel.substmodel").info(sb.toString());
        return model;
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "A general reversible model of sequence substitution for any data type with stochastic variable selection.";
    }

    public Class getReturnType() {
        return SubstitutionModel.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            new XORRule(
                    new StringAttributeRule(DataType.DATA_TYPE, "The type of sequence data",
                            DataType.getRegisteredDataTypeNames(), false),
                    new ElementRule(DataType.class)
            ),
            AttributeRule.newBooleanRule(RANDOMIZE, true),
            new ElementRule(ROOT_FREQUENCIES, FrequencyModel.class),
            new ElementRule(RATES,
                    new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)}
            ),
            new ElementRule(INDICATOR,
                    new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)
                    }),
            AttributeRule.newBooleanRule(NORMALIZATION, true),
            AttributeRule.newDoubleRule(MAX_CONDITION_NUMBER, true),
            AttributeRule.newBooleanRule(CONNECTED, true),
            AttributeRule.newIntegerRule(MAX_ITERATIONS, true),
            AttributeRule.newBooleanRule(CHECK_CONDITIONING, true),
    };

}
