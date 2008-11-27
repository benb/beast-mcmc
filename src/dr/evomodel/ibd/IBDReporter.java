package dr.evomodel.ibd;

import dr.evolution.tree.NodeAttributeProvider;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.DefaultBranchRateModel;
import dr.evomodel.substmodel.AbstractSubstitutionModel;
import dr.evomodel.substmodel.HKY;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.*;
import dr.xml.*;

/**
 * Package: dr.evomodel.ibd
 * Description:
 * <p/>
 * <p/>
 * Created by
 * avaleks (alexander.alekseyenko@gmail.com)
 * Date: 04-Aug-2008
 * Time: 13:46:33
 */
public class IBDReporter extends AbstractModel implements NodeAttributeProvider {

    protected double[] ibdweights;
    protected double[][] ibdForward;
    protected double[][] ibdBackward;
    protected double[] diag;
    protected boolean weightsKnown;
    protected HKY substitutionModel;
    protected TreeModel treeModel;
    protected BranchRateModel branchRateModel;
    protected Parameter mutationParameter;

    IBDReporter(Parameter mutationParameter, TreeModel treeModel, BranchRateModel branchRateModel, AbstractSubstitutionModel substitutionModel) {
        super("IBDReporter");
        this.substitutionModel = (HKY) substitutionModel;
        addModel(this.substitutionModel);
        this.treeModel = treeModel;
        addModel(this.treeModel);
        this.branchRateModel = branchRateModel;
        addModel(this.branchRateModel);
        this.mutationParameter = mutationParameter;
        addParameter(this.mutationParameter);
    }

    public void forwardIBD() {
        int numNodes = treeModel.getNodeCount();
        int stateCount = substitutionModel.getStateCount();
        getDiagonalRates(diag);
        for (int nodeId = 0; nodeId < numNodes; ++nodeId) {
            NodeRef node = treeModel.getNode(nodeId);
            NodeRef parent = treeModel.getParent(node);
            if (parent == null) { // handle the root

            } else if (treeModel.isExternal(node)) { // Handle the tip
                double branchTime = branchRateModel.getBranchRate(treeModel, node) * (treeModel.getNodeHeight(parent) - treeModel.getNodeHeight(node));

                for (int state = 0; state < stateCount; ++state) {
                    ibdForward[nodeId][state] = Math.exp(-diag[state] * branchTime);
                }
            } else { // Handle internal node
                double branchTime = branchRateModel.getBranchRate(treeModel, node) * (treeModel.getNodeHeight(parent) - treeModel.getNodeHeight(node));

                int childCount = treeModel.getChildCount(node);
                for (int state = 0; state < stateCount; ++state) {
                    ibdForward[nodeId][state] = 0;
                    for (int child = 0; child < childCount; ++child) {
                        int childNodeId = treeModel.getChild(node, child).getNumber();
                        ibdForward[nodeId][state] += ibdForward[childNodeId][state];
                    }
                    ibdForward[nodeId][state] *= Math.exp(-diag[state] * branchTime);
                }
            }
        }
    }

    public void backwardIBD(NodeRef node) {
        int stateCount = substitutionModel.getStateCount();
        if (node == null) {
            node = treeModel.getRoot();
            int nodeId = node.getNumber();
            for (int state = 0; state < stateCount; ++state) {
                ibdBackward[nodeId][state] = 0;
            }
        }
        getDiagonalRates(diag);
        int childCount = treeModel.getChildCount(node);
        int nodeId = node.getNumber();
        for (int child = 0; child < childCount; ++child) {
            NodeRef childNode = treeModel.getChild(node, child);
            int childNodeId = childNode.getNumber();
            double branchTime = branchRateModel.getBranchRate(treeModel, childNode) * (treeModel.getNodeHeight(node) - treeModel.getNodeHeight(childNode));
            for (int state = 0; state < stateCount; ++state) {
                ibdBackward[childNodeId][state] = ibdBackward[nodeId][state];
                for (int sibling = 0; sibling < childCount; ++sibling) {
                    if (sibling != child) {
                        int siblingId = treeModel.getChild(node, sibling).getNumber();
                        ibdBackward[childNodeId][state] += ibdForward[siblingId][state];
                    }
                }
                ibdBackward[childNodeId][state] *= Math.exp(-diag[state] * branchTime);
            }
        }
        for (int child = 0; child < childCount; ++child) {
            NodeRef childNode = treeModel.getChild(node, child);
            backwardIBD(childNode);
        }

    }

    public void expectedIBD() {
        int stateCount = substitutionModel.getStateCount();
        int nodeCount = treeModel.getNodeCount();
        if (ibdweights == null) {
            ibdweights = new double[treeModel.getExternalNodeCount()];
            ibdForward = new double[nodeCount][stateCount];
            ibdBackward = new double[nodeCount][stateCount];
            diag = new double[stateCount];
        }

        double[] freq = substitutionModel.getFrequencyModel().getFrequencies();
        forwardIBD();
        backwardIBD(null);
        int numTips = treeModel.getExternalNodeCount();
        for (int i = 0; i < numTips; ++i) {
            ibdweights[i] = 0;
            for (int j = 0; j < stateCount; ++j) {
                ibdweights[i] += ibdBackward[i][j] * freq[j];
            }
        }
    }

    protected void getDiagonalRates(double[] diagonalRates) {
        double kappa = substitutionModel.getKappa();
        double[] freq = substitutionModel.getFrequencyModel().getFrequencies();
        double mutationRate = mutationParameter.getParameterValue(0);
        double beta = 0.5 / ((freq[0] + freq[2]) * (freq[1] + freq[3]) + kappa * (freq[0] * freq[2] + freq[1] * freq[3]));

        diagonalRates[0] = ((freq[1] + freq[3]) + freq[2] * kappa) * mutationRate * beta;
        diagonalRates[1] = ((freq[0] + freq[2]) + freq[3] * kappa) * mutationRate * beta;
        diagonalRates[2] = ((freq[1] + freq[3]) + freq[0] * kappa) * mutationRate * beta;
        diagonalRates[3] = ((freq[0] + freq[2]) + freq[1] * kappa) * mutationRate * beta;
    }

    public String[] getNodeAttributeLabel() {
        return new String[]{"IBDWeight"};
    }

    public String[] getAttributeForNode(Tree tree, NodeRef node) {
        if (!weightsKnown) {
            expectedIBD();
            weightsKnown = true;
        }
        if (tree.isExternal(node)) {
            int nodeNum = node.getNumber();
            return new String[]{Double.toString(ibdweights[nodeNum] + 1)};
        }
        return new String[]{""};
    }

    /**
     * The XML parser
     */

    public static final String IBD_REPORTER_LIKELIHOOD = "ibdReporter";
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return IBD_REPORTER_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {


            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            Parameter mutationParameter = (Parameter) xo.getChild(Parameter.class);
            AbstractSubstitutionModel substitutionModel =
                    (AbstractSubstitutionModel) xo.getChild(AbstractSubstitutionModel.class);

            BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);
            if (branchRateModel == null) {
                branchRateModel = new DefaultBranchRateModel();
            }

            return new IBDReporter(mutationParameter, treeModel, branchRateModel, substitutionModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents a reporter for expected number of tips ibd.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(TreeModel.class),
                new ElementRule(BranchRateModel.class, true),
                new ElementRule(AbstractSubstitutionModel.class),
                new ElementRule(Parameter.class)
        };
    };

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        if (model == branchRateModel || model == treeModel || model == substitutionModel) {
            weightsKnown = false;
        } else {
            System.err.println("Weird call back to IBDReporter from " + model.getModelName());
        }
    }

    protected final void handleParameterChangedEvent(Parameter parameter, int index, ParameterChangeType type) {
        if (parameter == mutationParameter) {
            weightsKnown = false;
        } else {
            System.err.println("Weird call back to IBDReporter from " + parameter.getParameterName());
        }
    }

    protected void storeState() {
    }

    protected void restoreState() {
    }

    protected void acceptState() {
    }
}
