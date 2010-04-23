package dr.evomodel.treelikelihood;

import dr.evolution.alignment.Alignment;
import dr.evolution.alignment.HypermutantAlignment;
import dr.evolution.datatype.Nucleotides;
import dr.inference.model.Parameter;
import dr.inference.model.Statistic;
import dr.inference.model.Variable;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class HypermutantErrorModel extends TipPartialsModel {

    public static final String HYPERMUTANT_ERROR_MODEL = "hypermutantErrorModel";
    public static final String HYPERMUTATION_RATE = "hypermutationRate";
    public static final String HYPERMUTATION_INDICATORS = "hypermutationIndicators";
    public static final String UNLINKED_RATES = "unlinkedRates";

    public HypermutantErrorModel(HypermutantAlignment hypermutantAlignment, Parameter hypermutationRateParameter, Parameter hypermuationIndicatorParameter, boolean unlinkedRates) {
        super(HYPERMUTANT_ERROR_MODEL, null, null);
        this.hypermutantAlignment = hypermutantAlignment;
        this.unlinkedRates = unlinkedRates;

        this.hypermutationRateParameter = hypermutationRateParameter;
        addVariable(this.hypermutationRateParameter);

        this.hypermuationIndicatorParameter = hypermuationIndicatorParameter;

        addVariable(this.hypermuationIndicatorParameter);

        addStatistic(new TaxonHypermutatedStatistic());
        addStatistic(new TaxonHypermutationRateStatistic());
        addStatistic(new HypermutatedProportionStatistic());
    }

    protected void taxaChanged() {
        if (hypermuationIndicatorParameter.getDimension() <= 1) {
            this.hypermuationIndicatorParameter.setDimension(tree.getExternalNodeCount());
        }
        if (unlinkedRates && hypermutationRateParameter.getDimension() <= 1) {
            this.hypermutationRateParameter.setDimension(tree.getExternalNodeCount());
        }
    }

    public void getTipPartials(int nodeIndex, double[] partials) {
        int[] states = this.states[nodeIndex];
        boolean isHypermutated = hypermuationIndicatorParameter.getParameterValue(nodeIndex) > 0.0;

        double rate = (unlinkedRates ? hypermutationRateParameter.getParameterValue(nodeIndex) : hypermutationRateParameter.getParameterValue(0));

        int k = 0;
        for (int j = 0; j < patternCount; j++) {

            switch (states[j]) {
                case Nucleotides.A_STATE: // is an A
                    partials[k] = 1.0;
                    partials[k + 1] = 0.0;
                    partials[k + 2] = 0.0;
                    partials[k + 3] = 0.0;
                    break;
                case Nucleotides.C_STATE: // is an C
                    partials[k] = 0.0;
                    partials[k + 1] = 1.0;
                    partials[k + 2] = 0.0;
                    partials[k + 3] = 0.0;
                    break;
                case Nucleotides.G_STATE: // is an G
                    partials[k] = 0.0;
                    partials[k + 1] = 0.0;
                    partials[k + 2] = 1.0;
                    partials[k + 3] = 0.0;
                    break;
                case Nucleotides.UT_STATE: // is an T
                    partials[k] = 0.0;
                    partials[k + 1] = 0.0;
                    partials[k + 2] = 0.0;
                    partials[k + 3] = 1.0;
                    break;
                case Nucleotides.R_STATE: // is an A in a APOBEC context
                    if (isHypermutated) {
                        partials[k] = 1.0 - rate;
                        partials[k + 1] = 0.0;
                        partials[k + 2] = rate;
                        partials[k + 3] = 0.0;
                    } else {
                        partials[k] = 1.0;
                        partials[k + 1] = 0.0;
                        partials[k + 2] = 0.0;
                        partials[k + 3] = 0.0;
                    }

                    break;
                default: // is an ambiguity
                    partials[k] = 1.0;
                    partials[k + 1] = 1.0;
                    partials[k + 2] = 1.0;
                    partials[k + 3] = 1.0;
            }

            k += stateCount;
        }

    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        if (variable == hypermuationIndicatorParameter) {
            fireModelChanged();
        } else if (variable == hypermutationRateParameter) {
            if (!unlinkedRates || hypermuationIndicatorParameter.getValue(index)  > 0.5) {
                // only fire an update if the indicator is on....
                fireModelChanged();
            }
        } else {
            throw new RuntimeException("Unknown parameter has changed in HypermutantErrorModel.handleVariableChangedEvent");
        }

    }


    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() { return HYPERMUTANT_ERROR_MODEL; }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            boolean unlinkedRates = false;
            if (xo.hasAttribute(UNLINKED_RATES)) {
                unlinkedRates = xo.getBooleanAttribute(UNLINKED_RATES);
            }

            HypermutantAlignment hypermutantAlignment = (HypermutantAlignment)xo.getChild(HypermutantAlignment.class);

            Parameter hypermutationRateParameter = null;
            if (xo.hasChildNamed(HYPERMUTATION_RATE)) {
                hypermutationRateParameter = (Parameter)xo.getElementFirstChild(HYPERMUTATION_RATE);
            }

            Parameter hypermuationIndicatorParameter = null;
            if (xo.hasChildNamed(HYPERMUTATION_INDICATORS)) {
                hypermuationIndicatorParameter = (Parameter)xo.getElementFirstChild(HYPERMUTATION_INDICATORS);
            }

            HypermutantErrorModel errorModel =  new HypermutantErrorModel(hypermutantAlignment, hypermutationRateParameter, hypermuationIndicatorParameter, unlinkedRates);

            Logger.getLogger("dr.evomodel").info("Using APOBEC error model");

            return errorModel;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return
                    "This element returns a model that allows for APOBEC-type RNA editing.";
        }

        public Class getReturnType() { return HypermutantErrorModel.class; }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                AttributeRule.newBooleanRule(UNLINKED_RATES, true),
                new ElementRule(HypermutantAlignment.class),
                new ElementRule(HYPERMUTATION_RATE, Parameter.class, "The hypermutation rate per target site per sequence"),
                new ElementRule(HYPERMUTATION_INDICATORS, Parameter.class, "A binary indicator of whether the sequence is hypermutated"),
        };
    };

    public class TaxonHypermutatedStatistic extends Statistic.Abstract {

        public TaxonHypermutatedStatistic() {
            super("isHypermutated");
        }

        public int getDimension() {
            return hypermuationIndicatorParameter.getDimension();
        }

        public String getDimensionName(int dim) {
            return taxonMap.get(dim);
        }

        public double getStatisticValue(int dim) {
            return hypermuationIndicatorParameter.getParameterValue(dim);
        }

    }

    public class TaxonHypermutationRateStatistic extends Statistic.Abstract {

        public TaxonHypermutationRateStatistic() {
            super("hypermutationRate");
        }

        public int getDimension() {
            return hypermutationRateParameter.getDimension();
        }

        public String getDimensionName(int dim) {
            return taxonMap.get(dim) + ".rate";
        }

        public double getStatisticValue(int dim) {
            return hypermutationRateParameter.getParameterValue(dim) * hypermuationIndicatorParameter.getParameterValue(dim);
        }

    }

    public class HypermutatedProportionStatistic extends Statistic.Abstract {

        public HypermutatedProportionStatistic() {
            super("proportionHypermutated");
        }

        public int getDimension() {
            return 1;
        }

        public String getDimensionName(int dim) {
            return "P(hypermutated)";
        }

        public double getStatisticValue(int dim) {
            int[] mutatedCounts = hypermutantAlignment.getMutatedContextCounts();
            int[] unmutatedCounts = hypermutantAlignment.getUnmutatedContextCounts();

            double mutatedCount = 0;
            double totalCount = 0;
            for (int i = 0; i < hypermuationIndicatorParameter.getDimension(); i++) {
                if (hypermuationIndicatorParameter.getParameterValue(i) > 0.5) {
                    mutatedCount += mutatedCounts[i];
                    totalCount += mutatedCount + unmutatedCounts[i];

                }
            }

            double r = hypermutationRateParameter.getParameterValue(0);
            return (r * mutatedCount) / totalCount;
        }

    }

    private final HypermutantAlignment hypermutantAlignment;
    private final Parameter hypermutationRateParameter;
    private final Parameter hypermuationIndicatorParameter;
    private final boolean unlinkedRates;
}