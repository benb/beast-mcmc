package dr.evomodel.speciation;

import dr.evolution.tree.NodeAttributeProvider;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.tree.randomlocalmodel.RandomLocalTreeVariable;
import dr.evoxml.XMLUnits;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * This class contains methods that describe a Yule speciation model whose rate of birth changes
 * at different points in the tree.
 *
 * @author Alexei Drummond
 */
public class RandomLocalYuleModel extends SpeciationModel implements NodeAttributeProvider, RandomLocalTreeVariable {

    public static final String YULE_MODEL = "randomLocalYuleModel";
    public static String MEAN_RATE = "meanRate";
    public static String BIRTH_RATE = "birthRates";
    public static String BIRTH_RATE_INDICATORS = "indicators";
    public static String RATES_AS_MULTIPLIERS = "ratesAsMultipliers";
    private boolean calculateAllBirthRates = false;

    public RandomLocalYuleModel(Parameter birthRates, Parameter indicators, Parameter meanRate,
                                boolean ratesAsMultipliers, Type units, int dp) {

        super(RandomLocalYuleModel.YULE_MODEL, units);

        addParameter(birthRates);
        birthRates.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, birthRates.getDimension()));

        for (int i = 0; i < indicators.getDimension(); i++) {
            indicators.setParameterValueQuietly(i, 0.0);
        }

        addParameter(indicators);

        this.meanRate = meanRate;
        addParameter(meanRate);

        birthRatesAreMultipliers = ratesAsMultipliers;

        format.setMaximumFractionDigits(dp);

        birthRatesName = birthRates.getParameterName();
        System.out.println("  birth rates parameter is named '" + birthRatesName + "'");
        indicatorsName = indicators.getParameterName();
        System.out.println("  indicator parameter is named '" + indicatorsName + "'");

        this.birthRates = new double[birthRates.getDimension() + 1];
    }

    public final double getVariable(TreeModel tree, NodeRef node) {
        return tree.getNodeTrait(node, birthRatesName);
    }

    public final boolean isVariableSelected(TreeModel tree, NodeRef node) {
        return tree.getNodeTrait(node, indicatorsName) > 0.5;
    }

    //
    // functions that define a speciation model
    //
    public final double logTreeProbability(int taxonCount) {

        // calculate all nodes birth rates
        calculateAllBirthRates = true;

        return 0.0;

    }

    //
    // functions that define a speciation model
    //
    public final double logNodeProbability(Tree tree, NodeRef node) {

        if (calculateAllBirthRates) {
            calculateBirthRates((TreeModel) tree, tree.getRoot(), 0.0);
            calculateAllBirthRates = false;
        }

        if (tree.isRoot(node)) {
            return 0.0;
        } else {

            double lambda = birthRates[node.getNumber()];
            double branchLength = tree.getNodeHeight(tree.getParent(node)) - tree.getNodeHeight(node);
            double logP = -lambda * branchLength;

            if (tree.isExternal(node)) logP += Math.log(lambda);

            return logP;
        }
    }

    private void calculateBirthRates(TreeModel tree, NodeRef node, double rate) {

        int nodeNumber = node.getNumber();

        if (tree.isRoot(node)) {
            rate = meanRate.getParameterValue(0);
        } else {
            if (isVariableSelected(tree, node)) {
                if (birthRatesAreMultipliers) {
                    rate *= getVariable(tree, node);
                } else {
                    rate = getVariable(tree, node);
                    ;
                }
            }
        }
        birthRates[nodeNumber] = rate;

        int childCount = tree.getChildCount(node);
        for (int i = 0; i < childCount; i++) {
            calculateBirthRates(tree, tree.getChild(node, i), rate);
        }
    }

//    /**
//     * @param tree the tree
//     * @param node the node to retrieve the birth rate of
//     * @return the birth rate of the given node;
//     */
//    private double getBirthRate(TreeModel tree, NodeRef node) {
//
//        double birthRate;
//        if (!tree.isRoot(node)) {
//
//            double parentRate = getBirthRate(tree, tree.getParent(node));
//            if (isVariableSelected(tree, node)) {
//                birthRate = getVariable(tree, node);
//                if (birthRatesAreMultipliers) {
//                    birthRate *= parentRate;
//                } else {
//                    throw new RuntimeException("Rates must be multipliers in current implementation! " +
//                            "Otherwise root rate might be ignored");
//                }
//            } else {
//                birthRate = parentRate;
//            }
//        } else {
//            birthRate = meanRate.getParameterValue(0);
//        }
//        return birthRate;
//    }

    public String[] getNodeAttributeLabel() {
        return new String[]{"I", "b"};
    }

    public String[] getAttributeForNode(Tree tree, NodeRef node) {

        String rateString = format.format(birthRates[node.getNumber()]);

        if (tree.isRoot(node)) {
            return new String[]{"0", rateString};
        }

        return new String[]{(isVariableSelected((TreeModel) tree, node) ? "1" : "0"), rateString};
    }

    public boolean includeExternalNodesInLikelihoodCalculation() {
        return true;
    }

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public org.w3c.dom.Element createElement(org.w3c.dom.Document d) {
        throw new RuntimeException("createElement not implemented");
    }

    /**
     * Parses an element from an DOM document into a SpeciationModel. Recognises
     * RandomLocalYuleModel.
     */
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return RandomLocalYuleModel.YULE_MODEL;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Type units = XMLParser.Utils.getUnitsAttr(xo);

            XMLObject cxo = (XMLObject) xo.getChild(RandomLocalYuleModel.BIRTH_RATE);
            Parameter brParameter = (Parameter) cxo.getChild(Parameter.class);

            cxo = (XMLObject) xo.getChild(RandomLocalYuleModel.BIRTH_RATE_INDICATORS);
            Parameter indicatorsParameter = (Parameter) cxo.getChild(Parameter.class);

            Parameter meanRate = (Parameter) xo.getElementFirstChild(RandomLocalYuleModel.MEAN_RATE);

            boolean ratesAsMultipliers = xo.getBooleanAttribute(RATES_AS_MULTIPLIERS);

            int dp = xo.getAttribute("dp", 4);

            return new RandomLocalYuleModel(brParameter, indicatorsParameter, meanRate, ratesAsMultipliers, units, dp);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A speciation model of a Yule process whose rate can change at random nodes in the tree.";
        }

        public Class getReturnType() {
            return RandomLocalYuleModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(RandomLocalYuleModel.BIRTH_RATE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(RandomLocalYuleModel.BIRTH_RATE_INDICATORS,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(RandomLocalYuleModel.MEAN_RATE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                AttributeRule.newBooleanRule(RATES_AS_MULTIPLIERS),
                AttributeRule.newIntegerRule("dp", true),
                XMLUnits.SYNTAX_RULES[0]
        };
    };

    private double[] birthRates;

    private String birthRatesName = "birthRates";
    private String indicatorsName = "birthRateIndicator";

    private Parameter meanRate;
    private boolean birthRatesAreMultipliers = false;
    private NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
}
