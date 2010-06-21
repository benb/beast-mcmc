package dr.evomodelxml.treelikelihood;

import dr.evomodel.tree.TreeModel;
import dr.inference.model.CompoundParameter;
import dr.inference.model.Parameter;
import dr.inference.model.ParameterParser;
import dr.math.MathUtils;
import dr.xml.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * @author Marc A. Suchard
 */
public class TreeTraitParserUtilities {


    public static final String TRAIT_PARAMETER = "traitParameter";
    public static final String TRAIT_NAME = "traitName";
    public static final String MISSING = "missingIndicator";
    public static final String RANDOM_SAMPLE = "randomSample";
    public static final String DEFAULT_TRAIT_NAME = "trait";

    public static final String RANDOMIZE = "randomize";
    public static final String RANDOMIZE_LOWER = "lower";
    public static final String RANDOMIZE_UPPER = "upper";

    public static final String JITTER = "jitter";
    public static final String WINDOW = "window";
    public static final String DUPLICATES = "duplicatesOnly";

    public void randomize(Parameter trait, double[] lower, double[] upper) {
        // Draws each dimension in each trait from U[lower, upper)
        for (int i = 0; i < trait.getDimension(); i++) {
            final int whichLower = i % lower.length;
            final int whichUpper = i % upper.length;
            final double newValue = MathUtils.uniform(lower[whichLower], upper[whichUpper]);
            trait.setParameterValue(i, newValue);
        }
    }

    public static ElementRule randomizeRules(boolean optional) {
        return new ElementRule(TreeTraitParserUtilities.RANDOMIZE, new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(TreeTraitParserUtilities.RANDOMIZE_LOWER, true),
                AttributeRule.newDoubleRule(TreeTraitParserUtilities.RANDOMIZE_UPPER, true),
                new ElementRule(Parameter.class)
        }, optional);
    }

    public static ElementRule jitterRules(boolean optional) {
        return new ElementRule(JITTER, new XMLSyntaxRule[]{
                AttributeRule.newDoubleArrayRule(WINDOW),
                AttributeRule.newBooleanRule(DUPLICATES, true),
                new ElementRule(Parameter.class),

        }, optional);
    }

    public void jitter(XMLObject xo, int length, List<Integer> missingIndices) throws XMLParseException {
        XMLObject cxo = xo.getChild(TreeTraitParserUtilities.JITTER);
                      Parameter traits = (Parameter) cxo.getChild(Parameter.class);
                      double[] window = cxo.getDoubleArrayAttribute(TreeTraitParserUtilities.WINDOW); // Must be included, no default value
                      boolean duplicates = cxo.getAttribute(TreeTraitParserUtilities.DUPLICATES, true); // default = true
                      jitter(traits, length, missingIndices, window, duplicates, true);                   
    }

    public void randomize(XMLObject xo) throws XMLParseException {
        XMLObject cxo = xo.getChild(RANDOMIZE);
        Parameter traits = (Parameter) cxo.getChild(Parameter.class);
        double[] randomizeLower;
        double[] randomizeUpper;
        if (cxo.hasAttribute(RANDOMIZE_LOWER)) {
            randomizeLower = cxo.getDoubleArrayAttribute(RANDOMIZE_LOWER);
        } else {
            randomizeLower = new double[]{-90.0};
        }
        if (cxo.hasAttribute(RANDOMIZE_UPPER)) {
            randomizeUpper = cxo.getDoubleArrayAttribute(RANDOMIZE_UPPER);
        } else {
            randomizeUpper = new double[]{+90.0};
        }
        randomize(traits, randomizeLower, randomizeUpper);
    }

    private class DoubleArray implements Comparable {

        double[] value;
        int index;

        DoubleArray(double[] value, int index) {
            this.value = value;
            this.index = index;
        }

        public double[] getValues() {
            return value;
        }

        public int getIndex() {
            return index;
        }

        public int compareTo(Object o) {
            double[] x = ((DoubleArray) o).getValues();
            for (int i = 0; i < value.length; i++) {
                if (value[i] > x[i]) {
                    return 1;
                } else if (value[i] < x[i]) {
                    return -1;
                }
            }
            return 0;
        }
    }

    public void jitter(Parameter trait, int dim, List<Integer> missingIndices, double[] window, boolean duplicates, boolean verbose) {
        int numTraits = trait.getDimension() / dim;
        boolean[] update = new boolean[numTraits];
        if (!duplicates) {
            Arrays.fill(update, true);
        } else {
            DoubleArray[] traitArray = new DoubleArray[numTraits];
            for (int i = 0; i < numTraits; i++) {
                double[] x = new double[dim];
                for (int j = 0; j < dim; j++) {
                    x[j] = trait.getParameterValue(i * dim + j);
                }
                traitArray[i] = new DoubleArray(x, i);
            }
            Arrays.sort(traitArray);
            // Mark duplicates
            for (int i = 1; i < numTraits; i++) {
                if (traitArray[i].compareTo(traitArray[i - 1]) == 0) {
                    update[traitArray[i - 1].getIndex()] = true;
                    update[traitArray[i].getIndex()] = true;
                }
            }
        }
        for (int i = 0; i < numTraits; i++) {
            if (update[i]) {
                StringBuffer sb1 = null;
                StringBuffer sb2 = null;
                if (verbose) {
                    sb1 = new StringBuffer();
                    sb2 = new StringBuffer();
                }
                boolean hitAtLeastOneComponent = false;
                for (int j = 0; j < dim; j++) {
                    final double oldValue = trait.getParameterValue(i * dim + j);
                    final double newValue;
                    if (!missingIndices.contains(i * dim + j)) {
                        newValue = window[j % window.length] * (MathUtils.nextDouble() - 0.5) +
                                oldValue;
                        trait.setParameterValue(i * dim + j, newValue);
                        hitAtLeastOneComponent = true;
                    } else {
                        newValue = oldValue;
                    }
                    if (verbose) {
                        sb1.append(" ").append(oldValue);
                        sb2.append(" ").append(newValue);
                    }
                }
                if (verbose && hitAtLeastOneComponent) {
                    Logger.getLogger("dr.evomodel.continuous").info(
                            "  Replacing trait #" + (i + 1) + "  Old:" + sb1.toString() + " New: " + sb2.toString()
                    );
                }
            }
        }
    }

    public class TraitsAndMissingIndices {
        public CompoundParameter traitParameter;
        public List<Integer> missingIndices;

        TraitsAndMissingIndices(CompoundParameter traitParameter, List<Integer> missingIndices) {
            this.traitParameter = traitParameter;
            this.missingIndices = missingIndices;
        }
    }

    public TraitsAndMissingIndices parseTraitsFromTaxonAttributes(
            XMLObject xo,
            String traitName,
            TreeModel treeModel,           
            boolean integrateOutInternalStates) throws XMLParseException {

        XMLObject xoc = xo.getChild(TRAIT_PARAMETER);
        Parameter parameter = (Parameter) xoc.getChild(Parameter.class);
        boolean existingTraitParameter = false;
        int randomSampleSizeFlag = xo.getAttribute(RANDOM_SAMPLE, -1);

        CompoundParameter traitParameter;
        List<Integer> missingIndices = null;

        if (parameter instanceof CompoundParameter) {
            // if we have been passed a CompoundParameter, this will be a leaf trait
            // parameter from a tree model so use this to allow for individual sampling
            // of leaf parameters.
            traitParameter = (CompoundParameter) parameter;
            existingTraitParameter = true;
        } else {
            // create a compound parameter of appropriate dimensions
            traitParameter = new CompoundParameter(parameter.getId());
            ParameterParser.replaceParameter(xoc, traitParameter);
        }

        if (xo.hasAttribute(TRAIT_NAME)) {

            List<Integer> randomSample = null;
            traitName = xo.getStringAttribute(TRAIT_NAME);

            // Fill in attributeValues
            int taxonCount = treeModel.getTaxonCount();
            for (int i = 0; i < taxonCount; i++) {
                String taxonName = treeModel.getTaxonId(i);
                String paramName = taxonName + "." + traitName;

                Parameter traitParam;
                if (existingTraitParameter) {
                    traitParam = getTraitParameterByName(traitParameter, paramName);
                    if (traitParam == null) {
                        throw new RuntimeException("Missing trait parameters for tree tip, " + paramName);
                    }
                } else {
                    traitParam = new Parameter.Default(paramName);
                    traitParameter.addParameter(traitParam);
                }

                String object = (String) treeModel.getTaxonAttribute(i, traitName);
                if (object == null)
                    throw new RuntimeException("Trait \"" + traitName + "\" not found for taxa \"" + taxonName + "\"");
                else {
                    StringTokenizer st = new StringTokenizer(object);
                    int count = st.countTokens();
                    int sampleSize = count;
                    if (randomSampleSizeFlag > 0) {
                        if (randomSample == null) {
                            randomSample = drawRandomSample(randomSampleSizeFlag, count);
                        }
                        sampleSize = randomSampleSizeFlag;
                    }
                    if (sampleSize != traitParam.getDimension()) {
                        if (existingTraitParameter) {
                            throw new RuntimeException("Trait length must match trait parameter dimension");
                        } else {
                            traitParam.setDimension(sampleSize);
                        }
                    }
                    int index = 0;
                    for (int j = 0; j < count; j++) {
                        String oneValue = st.nextToken();
                        if (randomSampleSizeFlag == -1 || randomSample.contains(j)) {
                            double value = Double.NaN;
                            if (oneValue.compareTo("NA") == 0) {
                                Logger.getLogger("dr.evomodel.continuous").info(
                                        "Warning: Missing value in tip for taxon " + taxonName +
                                                " (filling with 0 as starting value when sampling only)"   // See comment below
                                );
                            } else {
                                try {
                                    value = new Double(oneValue);
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException(e.getMessage());
                                }
                            }
                            traitParam.setParameterValue(index, value);
                            index++;
                        }
                    }
                }
            }

            // Find missing values
            double[] allValues = traitParameter.getParameterValues();
            missingIndices = new ArrayList<Integer>();
            for (int i = 0; i < allValues.length; i++) {
                if ((new Double(allValues[i])).isNaN()) {
                    traitParameter.setParameterValue(i, 0); // Here, missings are set to zero
                    missingIndices.add(i);
                }
            }

            if (xo.hasChildNamed(MISSING)) {
                XMLObject cxo = xo.getChild(MISSING);
                Parameter missingParameter = new Parameter.Default(allValues.length, 0.0);
                for (int i : missingIndices) {
                    missingParameter.setParameterValue(i, 1.0);
                }
                missingParameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, allValues.length));
                ParameterParser.replaceParameter(cxo, missingParameter);
            }

            // Give warnings if trait exist for internal and root nodes when integrating them out
            if (integrateOutInternalStates) {
                int numTraits = traitParameter.getNumberOfParameters();
                if (numTraits != treeModel.getExternalNodeCount()) {
                    throw new XMLParseException(
                            "Dimensionality of '" + traitParameter.getId() + "' (" + numTraits + ") is not equal to the number" +
                                    " of tree tips (" + treeModel.getExternalNodeCount() + ")");
                }

                for (int j = 0; j < numTraits; j++) {
                    String parameterName = traitParameter.getParameter(j).getId();
                    if (parameterName.startsWith("node") || parameterName.startsWith("root")) {
                        throw new XMLParseException(
                                "Internal/root node trait parameters are not allowed when " +
                                        "using the integrated observed data multivariateTraitLikelihoood");
                    }
                }
            }
        }
        return new TraitsAndMissingIndices(traitParameter, missingIndices);
    }

    private Parameter getTraitParameterByName(CompoundParameter traits, String name) {

        for (int i = 0; i < traits.getNumberOfParameters(); i++) {
            Parameter found = traits.getParameter(i);
            if (found.getStatisticName().compareTo(name) == 0)
                return found;
        }
        return null;
    }

    private List<Integer> drawRandomSample(int total, int length) {
        List<Integer> thisList = new ArrayList<Integer>(total);
        for (int i = 0; i < total; i++) {
            thisList.add(MathUtils.nextInt(length));
        }
        return thisList;
    }
}
