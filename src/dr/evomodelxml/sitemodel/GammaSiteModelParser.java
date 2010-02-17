package dr.evomodelxml.sitemodel;

import dr.evomodel.sitemodel.GammaSiteModel;
import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.substmodel.SubstitutionModel;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.logging.Logger;

/**
 */
public class GammaSiteModelParser extends AbstractXMLObjectParser {

    public static final String SUBSTITUTION_MODEL = "substitutionModel";
    public static final String MUTATION_RATE = "mutationRate";
    public static final String RELATIVE_RATE = "relativeRate";
    public static final String GAMMA_SHAPE = "gammaShape";
    public static final String GAMMA_CATEGORIES = "gammaCategories";
    public static final String PROPORTION_INVARIANT = "proportionInvariant";


    public String[] getParserNames() {
        return new String[]{
                getParserName(), "beast_" + getParserName()
        };
    }

    public String getParserName() {
        return SiteModel.SITE_MODEL;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {
        SubstitutionModel substitutionModel = (SubstitutionModel) xo.getElementFirstChild(SUBSTITUTION_MODEL);

        String msg = "";

        Parameter muParam = null;
        if (xo.hasChildNamed(MUTATION_RATE)) {
            muParam = (Parameter) xo.getElementFirstChild(MUTATION_RATE);

            msg += "\n  with initial substitution rate = " + muParam.getParameterValue(0);
        } else if (xo.hasChildNamed(RELATIVE_RATE)) {
            muParam = (Parameter) xo.getElementFirstChild(RELATIVE_RATE);

            msg += "\n  with initial relative rate = " + muParam.getParameterValue(0);
        }

        Parameter shapeParam = null;
        int catCount = 4;
        if (xo.hasChildNamed(GAMMA_SHAPE)) {
            final XMLObject cxo = xo.getChild(GAMMA_SHAPE);
            catCount = cxo.getIntegerAttribute(GAMMA_CATEGORIES);
            shapeParam = (Parameter) cxo.getChild(Parameter.class);

            msg += "\n  " + catCount + " category discrete gamma with initial shape = " + shapeParam.getParameterValue(0);
        }

        Parameter invarParam = null;
        if (xo.hasChildNamed(PROPORTION_INVARIANT)) {
            invarParam = (Parameter) xo.getElementFirstChild(PROPORTION_INVARIANT);
            msg += "\n  initial proportion of invariant sites = " + invarParam.getParameterValue(0);
        }

        Logger.getLogger("dr.evomodel").info("Creating site model." + (msg.length() > 0 ? msg : ""));

        return new GammaSiteModel(substitutionModel, muParam, shapeParam, catCount, invarParam);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "A SiteModel that has a gamma distributed rates across sites";
    }

    public Class getReturnType() {
        return GammaSiteModel.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            new ElementRule(SUBSTITUTION_MODEL, new XMLSyntaxRule[]{
                    new ElementRule(SubstitutionModel.class)
            }),
            new XORRule(
                    new ElementRule(MUTATION_RATE, new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)
                    }),
                    new ElementRule(RELATIVE_RATE, new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)
                    }), true
            ),
            new ElementRule(GAMMA_SHAPE, new XMLSyntaxRule[]{
                    AttributeRule.newIntegerRule(GAMMA_CATEGORIES, true),
                    new ElementRule(Parameter.class)
            }, true),
            new ElementRule(PROPORTION_INVARIANT, new XMLSyntaxRule[]{
                    new ElementRule(Parameter.class)
            }, true)
    };
}
