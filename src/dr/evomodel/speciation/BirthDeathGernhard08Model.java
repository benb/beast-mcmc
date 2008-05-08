package dr.evomodel.speciation;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evoxml.XMLUnits;
import dr.inference.model.Parameter;
import dr.xml.*;
import static org.apache.commons.math.special.Gamma.logGamma;

/**
 * Birth Death model based on Gerdhart 2008  "The conditioned reconstructed process"
 * doi:10.1016/j.jtbi.2008.04.005
 *
 * This derivation conditions directly on fixed N taxa.
 * 
 * The inference is directly on b-d (strictly positive) and d/b (constrained in (0,1))
 *
 * Unable to nicely verify due to inability to simulate those correctly so far.
 *
 * @author joseph
 *         Date: 24/02/2008
 */
public class BirthDeathGernhard08Model extends SpeciationModel {

    public static final String BIRTH_DEATH_MODEL = "birthDeathModelGernhard08";
    public static String BIRTHDIFF_RATE = "birthMinusDeathRate";
    public static String RELATIVE_DEATH_RATE = "relativeDeathRate";

    private Parameter relativeDeathRateParameter;
    private Parameter birthDiffRateParameter;


    public BirthDeathGernhard08Model(Parameter birthDiffRateParameter, Parameter relativeDeathRateParameter, Type units) {

        super(BIRTH_DEATH_MODEL, units);

        this.birthDiffRateParameter = birthDiffRateParameter;
        addParameter(birthDiffRateParameter);
        birthDiffRateParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.relativeDeathRateParameter = relativeDeathRateParameter;
        addParameter(relativeDeathRateParameter);
        relativeDeathRateParameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));
    }

    public double getR() {
        return birthDiffRateParameter.getParameterValue(0);
    }

    public double getA() {
        return relativeDeathRateParameter.getParameterValue(0);
    }

    public double logTreeProbability(int taxonCount) {
        return logGamma(taxonCount+1)  +
        (taxonCount-1) * Math.log(getR()) + taxonCount * Math.log(1 - getA());
    }

    public double logNodeProbability(Tree tree, NodeRef node) {
        final double height = tree.getNodeHeight(node);
        final double mrh = -getR() * height;
        final double z = Math.log(1 - getA() * Math.exp(mrh));
        double l = -2*z + mrh;

        if( tree.getRoot() == node ) {
            l += mrh - z;
        }
        return l;
    }

    public boolean includeExternalNodesInLikelihoodCalculation() {
        return false;
    }

    /**
     * Parses an element from an DOM document into a SpeciationModel. Recognises
     * birthDeathModel.
     */
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return BIRTH_DEATH_MODEL;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Type units = XMLParser.Utils.getUnitsAttr(xo);

            Parameter birthParameter = (Parameter) xo.getSocketChild(BIRTHDIFF_RATE);
            Parameter deathParameter = (Parameter) xo.getSocketChild(RELATIVE_DEATH_RATE);

            return new BirthDeathGernhard08Model(birthParameter, deathParameter, units);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "Gerdhard (2008) model of speciation (equation at bottom of page 19).";
        }

        public Class getReturnType() {
            return BirthDeathNee94Model.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(BIRTHDIFF_RATE, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(RELATIVE_DEATH_RATE, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                XMLUnits.SYNTAX_RULES[0]
        };
    };
}