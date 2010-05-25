package dr.app.beagle.tools;

import dr.app.beagle.evomodel.sitemodel.GammaSiteRateModel;
import dr.app.beagle.evomodel.substmodel.FrequencyModel;
import dr.evolution.alignment.SimpleAlignment;
import dr.evolution.datatype.Codons;
import dr.evolution.datatype.DataType;
import dr.evolution.sequence.Sequence;
import dr.evolution.tree.NodeAttributeProvider;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.inference.markovjumps.MarkovJumpsRegisterAcceptor;
import dr.inference.markovjumps.MarkovJumpsType;
import dr.inference.markovjumps.StateHistory;
import dr.inference.model.Parameter;
import dr.math.MathUtils;

import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Simulates a complete transition history and alignment of sequences given a tree, substitution model and
 * branch rate model.  This code duplicates portions of dr.app.seqgen.SequenceSimulator.  However, SequenceSimulator
 * is wed to dr.evomodel.substmodel.SubstitutionModel which does not emit the infinitesimal generator.
 *
 * @author Marc A. Suchard
 * @author remco@cs.waikato.ac.nz
 */
public class CompleteHistorySimulator extends SimpleAlignment
        implements MarkovJumpsRegisterAcceptor, NodeAttributeProvider {

    /**
     * number of replications
     */
    protected int nReplications;
    /**
     * tree used for generating samples *
     */
    protected Tree tree;
    /**
     * site model used for generating samples *
     */
    protected GammaSiteRateModel siteModel;

    /**
     * branch rate model used for generating samples *
     */
    protected BranchRateModel branchRateModel;

    /**
     * nr of categories in site model *
     */
    int categoryCount;
    /**
     * nr of states in site model *
     */
    int stateCount;

    /**
     * an array used to hold infinitesimal generator
     */
//    protected double[] lambda;
//    protected double[][] probabilities;

    private boolean branchSpecificLambda = false;
    private Parameter branchVariableParameter = null;
    private Parameter branchPossibleValuesParameter = null;

    protected List<double[]> registers;
    protected List<String> jumpTags;
    protected List<MarkovJumpsType> jumpTypes;
    protected List<double[][]> realizedJumps;
    protected int nJumpProcesses = 0;
    protected boolean sumAcrossSites;

    private final Map<String, Integer> idMap = new HashMap<String, Integer>();

    /**
     * Constructor
     *
     * @param tree
     * @param siteModel
     * @param branchRateModel
     * @param nReplications:  nr of samples to generate
     */
//    public CompleteHistorySimulator(Tree tree, GammaSiteRateModel siteModel, BranchRateModel branchRateModel,
//                                    int nReplications) {
//        this(tree, siteModel, branchRateModel, nReplications, false);
//    }
//
//    public CompleteHistorySimulator(Tree tree, GammaSiteRateModel siteModel, BranchRateModel branchRateModel,
//                                    int nReplications, boolean sumAcrossSites) {
//        this(tree, siteModel, branchRateModel, nReplications, sumAcrossSites, null, null);
//
//    }

    public CompleteHistorySimulator(Tree tree, GammaSiteRateModel siteModel, BranchRateModel branchRateModel,
                                    int nReplications, boolean sumAcrossSites,
                                    Parameter branchVariableParameter, Parameter branchPossibleValuesParameter) {
        this.tree = tree;
        this.siteModel = siteModel;
        this.branchRateModel = branchRateModel;
        this.nReplications = nReplications;
        stateCount = this.siteModel.getSubstitutionModel().getDataType().getStateCount();
        categoryCount = this.siteModel.getCategoryCount();

        this.sumAcrossSites = sumAcrossSites;

        List<String> taxaIds = new ArrayList<String>();
        for (int i = 0; i < tree.getTaxonCount(); i++) {
            taxaIds.add(tree.getTaxon(i).getId());
        }

        int k = 1;
        for (String taxaId : taxaIds) {
            idMap.put(taxaId, k);
            k += 1;
        }

        format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(3);

        if (branchVariableParameter != null && branchPossibleValuesParameter != null) {
            if (branchVariableParameter.getDimension() != 1) {
                throw new RuntimeException("branchVariableParameter has the wrong dimension; should be 1");
            }
            if (branchPossibleValuesParameter.getDimension() != tree.getNodeCount()) {
                throw new RuntimeException("branchPossibleValuesParameter has the wrong dimension; should be "
                        + tree.getNodeCount());
            }
            branchSpecificLambda = true;
            this.branchPossibleValuesParameter = branchPossibleValuesParameter;
            this.branchVariableParameter = branchVariableParameter;
            StringBuilder sb = new StringBuilder();
            sb.append("Doing a complete history simulation using branch-specific variables\n\tReplacing variable '");
            sb.append(branchVariableParameter.getId());
            sb.append("' with values from '");
            sb.append(branchPossibleValuesParameter.getId());
            sb.append("'");
            Logger.getLogger("dr.app.beagle.tools").info(sb.toString());            
        }
    }

    /**
     * Convert integer representation of sequence into a Sequence
     *
     * @param seq  integer representation of the sequence
     * @param node used to determine taxon for sequence
     * @return Sequence
     */
    Sequence intArray2Sequence(int[] seq, NodeRef node) {
        String sSeq = "";
        DataType dataType = siteModel.getSubstitutionModel().getDataType();
        for (int i = 0; i < nReplications; i++) {
            if (dataType instanceof Codons) {
                String s = dataType.getTriplet(seq[i]);
                sSeq += s;
            } else {
                char c = dataType.getChar(seq[i]);
                sSeq += c;
            }
        }
        return new Sequence(tree.getNodeTaxon(node), sSeq);
    }


    public void addRegister(Parameter addRegisterParameter, MarkovJumpsType type, boolean scaleByTime) {

        if (registers == null) {
            registers = new ArrayList<double[]>();
        }

        if (jumpTags == null) {
            jumpTags = new ArrayList<String>();
        }

        if (jumpTypes == null) {
            jumpTypes = new ArrayList<MarkovJumpsType>();
        }

        if (realizedJumps == null) {
            realizedJumps = new ArrayList<double[][]>();
        }

        registers.add(addRegisterParameter.getParameterValues());
        jumpTags.add(addRegisterParameter.getId());
        jumpTypes.add(type);
        realizedJumps.add(new double[tree.getNodeCount()][nReplications]);
        nJumpProcesses++;

        // scaleByTime is currently ignored
    }

    public double[] getMarkovJumpsForNodeAndRegister(Tree tree, NodeRef node, int whichRegister) {
        if (this.tree != tree) {
            throw new RuntimeException("Wrong tree!");
        }
        return realizedJumps.get(whichRegister)[node.getNumber()];
    }

    public int getNumberOfJumpProcess() {
        return nJumpProcesses;
    }

//    public double[][] getMarkovJumpsForNode(Tree tree, NodeRef node) {
//        double[][] rtn = new double[nJumpProcesses][];
//        for(int r = 0; r < nJumpProcesses; r++) {
//            rtn[r] = getMarkovJumpsForNodeAndRegister(tree, node, r);
//        }
//        return rtn;
//    }

    public String[] getNodeAttributeLabel() {
        if (nJumpProcesses == 0) {
            return null;
        }
        String[] rtn = new String[nJumpProcesses];
        for (int r = 0; r < nJumpProcesses; r++) {
            rtn[r] = jumpTags.get(r);
        }
        return rtn;
    }

    public String[] getAttributeForNode(Tree tree, NodeRef node) {
        if (nJumpProcesses == 0) {
            return null;
        }
        String[] rtn = new String[nJumpProcesses];
        for (int r = 0; r < nJumpProcesses; r++) {
            rtn[r] = formattedValue(tree, node, r);
        }
        return rtn;
    }

    private String formattedValue(Tree tree, NodeRef node, int jump) {
        StringBuffer sb = new StringBuffer();
        double[] values = getMarkovJumpsForNodeAndRegister(tree, node, jump);
        if (sumAcrossSites) {
            double total = 0;
            for (double x : values) {
                total += x;
            }
            sb.append(total);
        } else {
            sb.append("{");
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(values[i]);
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private NumberFormat format;

    public String toString() {
        StringBuffer sb = new StringBuffer();
        //alignment output
        sb.append("alignment\n");
        sb.append(super.toString());
        sb.append("\n");
        //tree output
        sb.append("tree\n");
        Tree.Utils.newick(tree, tree.getRoot(), true, Tree.BranchLengthType.LENGTHS_AS_TIME,
                format, null,
                (nJumpProcesses > 0 ? new NodeAttributeProvider[]{this} : null),
                null,
                idMap,
                sb);
        sb.append("\n");
        return sb.toString();
    }

    /**
     * perform the actual sequence generation
     *
     * @return alignment containing randomly generated sequences for the nodes in the
     *         leaves of the tree
     */
    public void simulate() {

        double[] lambda = new double[stateCount * stateCount];

        if (!branchSpecificLambda) {
            siteModel.getSubstitutionModel().getInfinitesimalMatrix(lambda); // Assumes a single generator for whole tree
        }

        NodeRef root = tree.getRoot();

        double[] categoryProbs = siteModel.getCategoryProportions();
        int[] category = new int[nReplications];
        for (int i = 0; i < nReplications; i++) {
            category[i] = MathUtils.randomChoicePDF(categoryProbs);
        }

        FrequencyModel frequencyModel = siteModel.getSubstitutionModel().getFrequencyModel();
        int[] seq = new int[nReplications];
        for (int i = 0; i < nReplications; i++) {
            seq[i] = MathUtils.randomChoicePDF(frequencyModel.getFrequencies());
        }

        setDataType(siteModel.getSubstitutionModel().getDataType());

        traverse(root, seq, category, this, lambda);
    }

    /**
     * recursively walk through the tree top down, and add sequence to alignment whenever
     * a leave node is reached.
     *
     * @param node           reference to the current node, for which we visit all children
     * @param parentSequence randomly generated sequence of the parent node
     * @param category       array of categories for each of the sites
     * @param alignment
     */
    private void traverse(NodeRef node, int[] parentSequence, int[] category, SimpleAlignment alignment,
                          double[] lambda) {
        for (int iChild = 0; iChild < tree.getChildCount(node); iChild++) {

            NodeRef child = tree.getChild(node, iChild);
            int[] seq = new int[nReplications];
            StateHistory[] histories = new StateHistory[nReplications];

            if (branchSpecificLambda) {               
                final double branchValue = branchPossibleValuesParameter.getParameterValue(child.getNumber());
                branchVariableParameter.setParameterValue(0, branchValue);
//                System.err.println("trying value = " + branchValue + " for " + child.getNumber());
                siteModel.getSubstitutionModel().getInfinitesimalMatrix(lambda);
            }

            for (int i = 0; i < nReplications; i++) {
                histories[i] = simulateAlongBranch(tree, child, category[i], parentSequence[i], lambda);
                seq[i] = histories[i].getEndingState();
            }

            processHistory(child, histories);

            if (tree.getChildCount(child) == 0) {
                alignment.addSequence(intArray2Sequence(seq, child));
            }
            traverse(tree.getChild(node, iChild), seq, category, alignment, lambda);
        }
    }

    protected void processHistory(NodeRef node, StateHistory[] histories) {
        for (int jump = 0; jump < nJumpProcesses; jump++) {
            double[] register = registers.get(jump);
            MarkovJumpsType type = jumpTypes.get(jump);
            double[] realizedJump = realizedJumps.get(jump)[node.getNumber()];

            for (int i = 0; i < nReplications; i++) {
                if (type == MarkovJumpsType.COUNTS) {
                    realizedJump[i] = histories[i].getTotalRegisteredCounts(register);
                } else if (type == MarkovJumpsType.REWARDS) {
                    realizedJump[i] = histories[i].getTotalReward(register);
                } else {
                    throw new IllegalAccessError("Unknown MarkovJumps type");
                }
            }
        }
    }

    private StateHistory simulateAlongBranch(Tree tree, NodeRef node, int rateCategory, int startingState,
                                             double[] lambda) {

        NodeRef parent = tree.getParent(node);

        final double branchRate = branchRateModel.getBranchRate(tree, node);

        // Get the operational time of the branch
        final double branchTime = branchRate * (tree.getNodeHeight(parent) - tree.getNodeHeight(node));

        if (branchTime < 0.0) {
            throw new RuntimeException("Negative branch length: " + branchTime);
        }

        double branchLength = siteModel.getRateForCategory(rateCategory) * branchTime;

        return StateHistory.simulateUnconditionalOnEndingState(0.0, startingState, branchLength,
                lambda, stateCount);
    }
}
