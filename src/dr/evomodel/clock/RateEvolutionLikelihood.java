package dr.evomodel.clock;

import dr.inference.model.AbstractModel;
import dr.inference.model.Likelihood;
import dr.inference.model.Parameter;
import dr.inference.model.Model;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.tree.TreeModel;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;

import java.util.logging.Logger;

/**
 * Abstract superclass of likelihoods of rate evolution through time.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Michael Defoin Platel
 */
public abstract class RateEvolutionLikelihood extends AbstractModel implements BranchRateModel, Likelihood {

    public static final String RATES = "rates";
    public static final String EPISODIC = "episodic";
    public static final String LOGSPACE = "logspace";

    // the index of the root node.
    private int rootNodeNumber;

    private final int rateCount;
    private final double[] rates;

    private boolean ratesKnown = false;


    public RateEvolutionLikelihood(String name, TreeModel treeModel, Parameter ratesParameter, Parameter rootRateParameter, boolean isEpisodic) {

        super(name);

        this.treeModel = treeModel;
        addModel(treeModel);

        this.ratesParameter = ratesParameter;
        addParameter(ratesParameter);

        this.rootRateParameter = rootRateParameter;
        addParameter(rootRateParameter);

        rateCount = treeModel.getNodeCount() - 1;
        rates = new double[rateCount + 1];

        if (ratesParameter.getDimension() != rateCount) {
            throw new IllegalArgumentException("The rates parameter must be of dimension nodeCount-1");
        }

        if (rootRateParameter.getDimension() != 1) {
            throw new IllegalArgumentException("The root rate parameter must be of dimension 1");
        }

        ratesKnown = false;

        rootNodeNumber = treeModel.getRoot().getNumber();

        this.isEpisodic = isEpisodic;

        setupRates();

        Logger.getLogger("dr.evomodel").info("AutoCorrelated Relaxed Clock: " + name + (isEpisodic ? " (episodic)." : "."));

    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    public final void handleModelChangedEvent(Model model, Object object, int index) {

        ratesKnown = false;
        likelihoodKnown = false;
        fireModelChanged();
    }

    protected void handleParameterChangedEvent(Parameter parameter, int index) {
        ratesKnown = false;
        likelihoodKnown = false;
        fireModelChanged();
    }

    protected void storeState() {
    }

    protected void restoreState() {
        ratesKnown = false;
        likelihoodKnown = false;
    }

    protected void acceptState() {
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Get the model.
     *
     * @return the model.
     */
    public Model getModel() {
        return this;
    }

    public final double getLogLikelihood() {
        if (!getLikelihoodKnown()) {
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    public void makeDirty() {
        likelihoodKnown = false;
    }

    /**
     * Called to decide if the likelihood must be calculated. Can be overridden
     * (for example, to always return false).
     *
     * @return the likelihood.
     */
    protected boolean getLikelihoodKnown() {
        return likelihoodKnown;
    }

    /**
     * Get the log likelihood of the rate changes in this tree.
     *
     * @return the log likelihood.
     */
    private double calculateLogLikelihood() {
        NodeRef root = treeModel.getRoot();
        NodeRef node1 = treeModel.getChild(root, 0);
        NodeRef node2 = treeModel.getChild(root, 1);

        return calculateLogLikelihood(root, node1) + calculateLogLikelihood(root, node2);
    }

    /**
     * Recursively calculate the log likelihood of the rate changes in the given tree.
     *
     * @return the partial log likelihood of the rate changes below the given node plus the
     *         branch directly above.
     */
    private double calculateLogLikelihood(NodeRef parent, NodeRef node) {

        double logL, length;
        if (treeModel.isRoot(parent))
            length = treeModel.getBranchLength(node) / 2.;
        else
            length = treeModel.getBranchLength(node);

        logL = branchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node),
                length);

        //System.out.print(parent.getNumber() + " " + getBranchRate(treeModel, parent)+ " " + node.getNumber() + " " + getBranchRate(treeModel, node) + " " + treeModel.getBranchLength(node) + " " + logL + ", ");

        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            logL += calculateLogLikelihood(node, treeModel.getChild(node, i));
        }
        return logL;
    }

    public String toString() {
        return Double.toString(getLogLikelihood());
    }

    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    /**
     * @return the log columns.
     */
    public dr.inference.loggers.LogColumn[] getColumns() {
        return new dr.inference.loggers.LogColumn[]{
                new RateEvolutionLikelihood.LikelihoodColumn(getId())
        };
    }

    private class LikelihoodColumn extends dr.inference.loggers.NumberColumn {
        public LikelihoodColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            return getLogLikelihood();
        }
    }

    public double getBranchRate(Tree tree, NodeRef node) {

        if (!ratesKnown) {
            setupRates();
            ratesKnown = true;
        }

        return rates[node.getNumber()];
    }

    public boolean isEpisodic() {
        return isEpisodic;
    }

    /**
     * @return the log likelihood of the rate change from the parent to the given node.
     */
    abstract double branchRateChangeLogLikelihood(double parentRate, double childRate, double time);

    // **************************************************************
    // Private members
    // **************************************************************

    /**
     * Sets the appropriate root rate for this model of rate change.
     */
    private void setupRates() {

        rootNodeNumber = treeModel.getRoot().getNumber();
        rates[rootNodeNumber] = rootRateParameter.getParameterValue(0);

        int j = 0;
        for (int i = 0; i < rateCount + 1; i++) {
            if (i != rootNodeNumber) {
                rates[i] = ratesParameter.getParameterValue(j);
                j++;
            }
        }
    }

    public String getBranchAttributeLabel() {
        return "rate";
    }

    public String getAttributeForBranch(Tree tree, NodeRef node) {
        return Double.toString(getBranchRate(tree, node));
    }

    private double logLikelihood;
    private boolean likelihoodKnown = false;

    private final TreeModel treeModel;
    private final Parameter ratesParameter;
    private final Parameter rootRateParameter;
    private final boolean isEpisodic;

}
