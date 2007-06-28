package dr.evomodel.coalescent.operators;

import dr.inference.operators.SimpleMCMCOperator;
import dr.inference.operators.MCMCOperator;
import dr.inference.operators.OperatorFailedException;
import dr.inference.operators.GibbsOperator;
import dr.inference.distribution.ParametricDistributionModel;
import dr.inference.model.Parameter;
import dr.xml.XMLParseException;
import dr.xml.XMLObject;
import dr.xml.XMLSyntaxRule;
import dr.xml.ElementRule;
import dr.math.MathUtils;

/**

 */
public class SampleNonActiveGibbsOperator extends SimpleMCMCOperator implements GibbsOperator {
    private static String SAMPLE_NONACTIVE_GIBBS_OPERATOR = "sampleNoneActiveOperator";

    private static String INDICATOR_PARAMETER = "indicators";
    private static String DATA_PARAMETER = "data";
    private ParametricDistributionModel distribution;
    private Parameter data;
    private Parameter indicators;

    public SampleNonActiveGibbsOperator(ParametricDistributionModel distribution, Parameter data, Parameter indicators, int weight) {
        this.distribution = distribution;
        this.data = data;
        this.indicators = indicators;
        this.weight = weight;
    }


    public String getPerformanceSuggestion() {
        return null;
    }

    public String getOperatorName() {
        return "SampleNonActive";
    }

    public double doOperation() throws OperatorFailedException {
        final int idim = indicators.getDimension();
        assert idim == (data.getDimension() - 1);

        // available locations for direct sampling
        int[] loc = new int[idim];
        int nLoc = 0;

        for (int i = 0; i < idim; i++) {
            final double value = indicators.getStatisticValue(i);
            if( value == 0 ) {
                loc[nLoc] = i + 1;
                ++nLoc;
            }
        }

        if (nLoc > 0) {
            final int index = loc[MathUtils.nextInt(nLoc)];
            data.setParameterValue(index, distribution.quantile(MathUtils.nextDouble()));
        } else {
            throw new OperatorFailedException("no non-active indicators");
        }
        return 0;
    }

    public static dr.xml.XMLObjectParser PARSER = new dr.xml.AbstractXMLObjectParser() {

        public String getParserName() {
            return SAMPLE_NONACTIVE_GIBBS_OPERATOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
            int weight = xo.getIntegerAttribute(WEIGHT);

            XMLObject cxo = (XMLObject) xo.getChild("distribution");
            ParametricDistributionModel distribution =
                    (ParametricDistributionModel) cxo.getChild(ParametricDistributionModel.class);

            cxo = (XMLObject) xo.getChild(DATA_PARAMETER);
            Parameter data = (Parameter) cxo.getChild(Parameter.class);

            cxo = (XMLObject) xo.getChild(INDICATOR_PARAMETER);
            Parameter indicators = (Parameter) cxo.getChild(Parameter.class);

            return new SampleNonActiveGibbsOperator(distribution, data, indicators, weight);

        }

        // ************************************************************************
        // AbstractXMLObjectParser implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element returns a Gibbs operator for the joint distribution of population sizes.";
        }

        public Class getReturnType() {
            return MCMCOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                new ElementRule("distribution",
                        new XMLSyntaxRule[] { new ElementRule(ParametricDistributionModel.class) }),
                new ElementRule(INDICATOR_PARAMETER,
                        new XMLSyntaxRule[] { new ElementRule(Parameter.class) }),
                new ElementRule(DATA_PARAMETER,
                        new XMLSyntaxRule[] { new ElementRule(Parameter.class) }),
        };

    };

    public int getStepCount() {
        return 0;
    }
}
