package dr.evomodelxml;

import dr.evomodel.branchratemodel.DiscretizedBranchRates;
import dr.evomodel.tree.TreeModel;
import dr.inference.distribution.ParametricDistributionModel;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * @author Alexei Drummond
 */
public class DiscretizedBranchRatesParser extends AbstractXMLObjectParser {

    public static final String DISCRETIZED_BRANCH_RATES = "discretizedBranchRates";
    public static final String DISTRIBUTION = "distribution";
    public static final String RATE_CATEGORIES = "rateCategories";
    public static final String SINGLE_ROOT_RATE = "singleRootRate";
    public static final String OVERSAMPLING = "overSampling";
    //public static final String NORMALIZED_MEAN = "normalizedMean";


    public String getParserName() {
        return DISCRETIZED_BRANCH_RATES;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        final int overSampling = xo.getAttribute(OVERSAMPLING, 1);

        TreeModel tree = (TreeModel) xo.getChild(TreeModel.class);
        ParametricDistributionModel distributionModel = (ParametricDistributionModel) xo.getElementFirstChild(DISTRIBUTION);

        Parameter rateCategoryParameter = (Parameter) xo.getElementFirstChild(RATE_CATEGORIES);

        Logger.getLogger("dr.evomodel").info("Using discretized relaxed clock model.");
        Logger.getLogger("dr.evomodel").info("  over sampling = " + overSampling);
        Logger.getLogger("dr.evomodel").info("  parametric model = " + distributionModel.getModelName());
        Logger.getLogger("dr.evomodel").info("   rate categories = " + rateCategoryParameter.getDimension());

        if (xo.hasAttribute(SINGLE_ROOT_RATE)) {
            //singleRootRate = xo.getBooleanAttribute(SINGLE_ROOT_RATE);
            Logger.getLogger("dr.evomodel").warning("   WARNING: single root rate is not implemented!");
        }

        /* if (xo.hasAttribute(NORMALIZED_MEAN)) {
            dbr.setNormalizedMean(xo.getDoubleAttribute(NORMALIZED_MEAN));
        }*/

        return new DiscretizedBranchRates(tree, rateCategoryParameter, distributionModel, overSampling);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return
                "This element returns an discretized relaxed clock model." +
                        "The branch rates are drawn from a discretized parametric distribution.";
    }

    public Class getReturnType() {
        return DiscretizedBranchRates.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
            AttributeRule.newBooleanRule(SINGLE_ROOT_RATE, true, "Whether only a single rate should be used for the two children branches of the root"),
            //AttributeRule.newDoubleRule(NORMALIZED_MEAN, true, "The mean rate to constrain branch rates to once branch lengths are taken into account"),
            AttributeRule.newIntegerRule(OVERSAMPLING, true, "The integer factor for oversampling the distribution model (1 means no oversampling)"),
            new ElementRule(TreeModel.class),
            new ElementRule(DISTRIBUTION, ParametricDistributionModel.class, "The distribution model for rates among branches", false),
            new ElementRule(RATE_CATEGORIES, Parameter.class, "The rate categories parameter", false),
    };
}
