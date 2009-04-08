package dr.evomodelxml;

import dr.app.beauti.options.NucModelType;
import dr.evomodel.substmodel.FrequencyModel;
import dr.evomodel.substmodel.HKY;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class HKYParser extends AbstractXMLObjectParser {

    public static final String KAPPA = "kappa";
    public static final String FREQUENCIES = "frequencies";

    public String getParserName() {
        return NucModelType.HKY.getXMLName();
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        Parameter kappaParam = (Parameter) xo.getElementFirstChild(KAPPA);
        FrequencyModel freqModel = (FrequencyModel) xo.getElementFirstChild(FrequencyModel.FREQUENCIES);

        Logger.getLogger("dr.evomodel").info("Creating HKY substitution model. Initial kappa = " +
                kappaParam.getParameterValue(0));

        return new HKY(kappaParam, freqModel);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "This element represents an instance of the HKY85 " +
                "(Hasegawa, Kishino & Yano, 1985) model of nucleotide evolution.";
    }

    public Class getReturnType() {
        return HKY.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
            new ElementRule(FrequencyModel.FREQUENCIES,
                    new XMLSyntaxRule[]{new ElementRule(FrequencyModel.class)}),
            new ElementRule(KAPPA,
                    new XMLSyntaxRule[]{new ElementRule(Parameter.class)})
    };

}