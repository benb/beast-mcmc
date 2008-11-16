package dr.evomodel.treelikelihood;

import dr.evolution.alignment.PatternList;
import dr.evolution.tree.NodeAttributeProvider;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.substmodel.SubstitutionModel;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.Likelihood;
import dr.math.matrixAlgebra.Vector;
import dr.xml.*;

/**
 * Package: dr.evomodel.treelikelihood
 * Description:
 * <p/>
 * <p/>
 * Created by
 * avaleks (alexander.alekseyenko@gmail.com)
 * Date: 01-Aug-2008
 * Time: 10:36:39
 */
public class NodePosteriorTreeLikelihood extends TreeLikelihood implements NodeAttributeProvider {

    protected double[][] nodePosteriors;
    protected double[][] forwardProbs;
    protected double[] likes;
    boolean posteriorsKnown;
    private double[] childPartials;
    private double[] partialLikelihood;

    public NodePosteriorTreeLikelihood(PatternList patternList, TreeModel treeModel, SiteModel siteModel, BranchRateModel branchRateModel, TipPartialsModel tipPartialsModel, boolean useAmbiguities, boolean allowMissingTaxa, boolean storePartials, boolean forceJavaCore) {
        super(patternList, treeModel, siteModel, branchRateModel, tipPartialsModel, useAmbiguities, allowMissingTaxa, storePartials, forceJavaCore);
        // TreeLikelihood does not initialize the partials for tips, we'll do it ourselves
        int extNodeCount = treeModel.getExternalNodeCount();
        for (int i = 0; i < extNodeCount; i++) {
            String id = treeModel.getTaxonId(i);
            int index = patternList.getTaxonIndex(id);
            setPartials(likelihoodCore, patternList, categoryCount, index, i);
        }
        childPartials = new double[stateCount * patternCount];
        partialLikelihood = new double[stateCount * patternCount];
        posteriorsKnown = false;
    }

    public String[] getNodeAttributeLabel() {
        return new String[]{"posteriors"};
    }

    public String[] getAttributeForNode(Tree tree, NodeRef node) {
        if (tree != treeModel) {
            throw new RuntimeException("Can only calculate node posteriors on treeModel given to constructor");
        }
        if (!posteriorsKnown) {
            calculatePosteriors();
        }
        return new String[]{new Vector(nodePosteriors[node.getNumber()]).toString()};
    }

    public double[] getPosteriors(int nodeNum) {
        if (!posteriorsKnown) {
            calculatePosteriors();
        }
        return nodePosteriors[nodeNum];
    }
  
    public void getNodeMatrix(int nodeNum, double[] probabilities) {
        ((AbstractLikelihoodCore) likelihoodCore).getNodeMatrix(nodeNum, 0, probabilities);
    }

    public void calculatePosteriors() {
        int nodeCount = treeModel.getNodeCount();
        traverseForward(treeModel, treeModel.getRoot());
        for (int k = 0; k < nodeCount; ++k) {
            for (int i = 0; i < patternCount; ++i) {
                for (int j = 0; j < stateCount; ++j) {
                    nodePosteriors[k][stateCount * i + j] /= likes[i];
                }
            }
        }
        posteriorsKnown = true;
    }

    protected double calculateLogLikelihood() {
        posteriorsKnown = false;

        return super.calculateLogLikelihood();
    }


    public void traverseForward(TreeModel tree, NodeRef node) {
        if (nodePosteriors == null) {
            nodePosteriors = new double[tree.getNodeCount()][patternCount * stateCount];
        }
        if (forwardProbs == null) {
            forwardProbs = new double[tree.getNodeCount()][patternCount * stateCount];
        }
        int nodeNum = node.getNumber();

        NodeRef parent = tree.getParent(node);


        if (parent == null) {/* this is the root */
            double[] rootFreqs = frequencyModel.getFrequencies();
            double[] rootPartials = getRootPartials();
            if (likes == null) {
                likes = new double[patternCount];
            }

            for (int j = 0; j < patternCount; j++) {
                System.arraycopy(rootFreqs, 0, forwardProbs[nodeNum], j * stateCount, stateCount);
                likes[j] = 0.0;
                for (int k = 0; k < stateCount; ++k) {
                    nodePosteriors[nodeNum][stateCount * j + k] = forwardProbs[nodeNum][stateCount * j + k] * rootPartials[j * stateCount + k];
                    likes[j] = likes[j] + rootPartials[stateCount * j + k] * rootFreqs[k];
                }
            }

        } else { /* regular internal node */
            int parentNum = parent.getNumber();
            int numChildren = tree.getChildCount(parent);

            System.arraycopy(forwardProbs[parentNum], 0, forwardProbs[nodeNum], 0, stateCount * patternCount);

            for (int child = 0; child < numChildren; ++child) {
                NodeRef childNode = tree.getChild(parent, child);
                int childNum = childNode.getNumber();
                if (childNum != nodeNum) {
                    getNodeMatrix(childNum, probabilities);
                    likelihoodCore.getPartials(childNum, childPartials);
                    accumulateMatrixMultiply(probabilities, childPartials, forwardProbs[nodeNum]);
                }
            }

            getNodeMatrix(nodeNum, probabilities);
            likelihoodCore.getPartials(nodeNum, partialLikelihood);

            matrixMultiplyBackward(probabilities, forwardProbs[nodeNum], nodePosteriors[nodeNum]);
            for (int i = 0; i < patternCount * stateCount; ++i) {
                forwardProbs[nodeNum][i] = nodePosteriors[nodeNum][i];
                nodePosteriors[nodeNum][i] = nodePosteriors[nodeNum][i] * partialLikelihood[i];
            }
        }
        if (!tree.isExternal(node)) { /* looking at internal nodes here */
            for (int child = 0; child < tree.getChildCount(node); ++child) {
                NodeRef child1 = tree.getChild(node, child);
                traverseForward(tree, child1);
            }
        }
    }

    public void accumulateMatrixMultiply(double[] probabilities, double[] partials, double[] out) {
        int u = 0;
        int v = 0;

        for (int k = 0; k < patternCount; ++k) {
            int w = 0;
            for (int i = 0; i < stateCount; ++i) {
                double sum1 = 0.0;
                for (int j = 0; j < stateCount; ++j) {
                    sum1 += probabilities[w] * partials[v + j];
                    w++;
                }
                out[u] = sum1 * out[u];
                u++;
            }
            v += stateCount;
        }
    }

    public void matrixMultiplyBackward(double[] probabilities, double[] partials, double[] out) {
        int u = 0;
        int v = 0;
        for (int k = 0; k < patternCount; ++k) {
            for (int i = 0; i < stateCount; ++i) {
                int w = i;
                double sum1 = 0.0;
                for (int j = 0; j < stateCount; ++j) {
                    sum1 += probabilities[w] * partials[v + j];
                    w += stateCount;
                }
                out[u] = sum1;
                u++;
            }
            v += stateCount;
        }
    }

    /**
     * The XML parser
     */

    public static final String NODE_POSTERIOR_LIKELIHOOD = "nodePosteriorLikelihood";
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return NODE_POSTERIOR_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            boolean useAmbiguities = xo.getAttribute(USE_AMBIGUITIES, false);
            boolean storePartials = xo.getAttribute(STORE_PARTIALS, true);

            PatternList patternList = (PatternList) xo.getChild(PatternList.class);
            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            SiteModel siteModel = (SiteModel) xo.getChild(SiteModel.class);

            BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

            return new NodePosteriorTreeLikelihood(patternList, treeModel, siteModel,
                    branchRateModel, null, useAmbiguities, false, storePartials, false);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the likelihood of a patternlist on a tree given the site model.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newBooleanRule(USE_AMBIGUITIES, true),
                AttributeRule.newBooleanRule(ALLOW_MISSING_TAXA, true),
                new ElementRule(PatternList.class),
                new ElementRule(TreeModel.class),
                new ElementRule(SiteModel.class),
                new ElementRule(BranchRateModel.class, true),
                new ElementRule(SubstitutionModel.class)
        };
    };

}

