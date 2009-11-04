package dr.app.beagle.evomodel.parsers;

import dr.xml.*;
import dr.inference.model.Parameter;
import dr.inference.model.BayesianStochasticSearchVariableSelection;
import dr.app.beagle.evomodel.substmodel.FrequencyModel;
import dr.app.beagle.evomodel.substmodel.SubstitutionModel;
import dr.app.beagle.evomodel.substmodel.ComplexSubstitutionModel;
import dr.app.beagle.evomodel.substmodel.SVSComplexSubstitutionModel;
import dr.evolution.datatype.DataType;

import java.util.logging.Logger;

/**
 * @author Marc Suchard
 */
public class ComplexSubstitutionModelParser extends AbstractXMLObjectParser {

    public static final String COMPLEX_SUBSTITUTION_MODEL = "complexSubstitutionModel";
    public static final String SVS_COMPLEX_SUBSTITUTION_MODEL = "svsComplexSubstitutionModel";
    public static final String DATA_TYPE = "dataType";
    public static final String RATES = "rates";
    public static final String FREQUENCIES = "frequencies";
    public static final String ROOT_FREQUENCIES = "rootFrequencies";
    public static final String RANDOMIZE = "randomizeIndicator";
    public static final String INDICATOR = "rateIndicator";

    public String getParserName() {
        return COMPLEX_SUBSTITUTION_MODEL;
    }

    public String[] getParserNames() {
        return new String[] {COMPLEX_SUBSTITUTION_MODEL, SVS_COMPLEX_SUBSTITUTION_MODEL};
    }


    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        Parameter ratesParameter;

        XMLObject cxo;
        if (xo.hasChildNamed(FREQUENCIES)) {
            cxo = xo.getChild(FREQUENCIES);
        } else {
            cxo = xo.getChild(ROOT_FREQUENCIES);
        }
        FrequencyModel freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);

        DataType dataType = freqModel.getDataType();

        cxo = xo.getChild(RATES);

        int states = dataType.getStateCount();

        Logger.getLogger("dr.app.beagle.evomodel").info("  Complex Substitution Model (stateCount=" + states + ")");

        ratesParameter = (Parameter) cxo.getChild(Parameter.class);

        int rateCount = (dataType.getStateCount() - 1) * dataType.getStateCount();

        if (ratesParameter == null) {

            if (rateCount == 1) {
                // simplest model for binary traits...
            } else {
                throw new XMLParseException("No rates parameter found in " + getParserName());
            }
        } else if (ratesParameter.getDimension() != rateCount) {
            throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + rateCount + " dimensions.");
        }

        if (!xo.hasChildNamed(INDICATOR)) {
            return new ComplexSubstitutionModel(COMPLEX_SUBSTITUTION_MODEL,dataType, freqModel, ratesParameter);
        }

        cxo = xo.getChild(INDICATOR);

        Parameter indicatorParameter = (Parameter) cxo.getChild(Parameter.class);
        boolean randomize = xo.getAttribute(RANDOMIZE, false);
        if (randomize) {
            BayesianStochasticSearchVariableSelection.Utils.randomize(indicatorParameter,
                    dataType.getStateCount(),false);
        }

        if (indicatorParameter == null || ratesParameter == null || indicatorParameter.getDimension() != ratesParameter.getDimension())
            throw new XMLParseException("Rates and indicator parameters in " + getParserName() + " element must be the same dimension.");

        return new SVSComplexSubstitutionModel(SVS_COMPLEX_SUBSTITUTION_MODEL,dataType, freqModel, ratesParameter, indicatorParameter);

    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "A general irreversible model of sequence substitution for any data type.";
    }

    public Class getReturnType() {
        return SubstitutionModel.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            AttributeRule.newBooleanRule(RANDOMIZE,true),
            new XORRule(
                new ElementRule(FREQUENCIES,FrequencyModel.class),
                new ElementRule(ROOT_FREQUENCIES,FrequencyModel.class)),
            new ElementRule(RATES,
                new XMLSyntaxRule[]{
                    new ElementRule(Parameter.class, true)}
            ),
            new ElementRule(INDICATOR,
                    new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)
                    },true),
    };
}
