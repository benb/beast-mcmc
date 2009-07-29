/*
 * TreeLikelihood.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.beagle.treelikelihood;

import beagle.Beagle;
import beagle.BeagleFactory;
import dr.evolution.alignment.PatternList;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.TaxonList;
import dr.evomodel.beagle.parsers.TreeLikelihoodParser;
import dr.evomodel.beagle.sitemodel.BranchSiteModel;
import dr.evomodel.beagle.sitemodel.SiteRateModel;
import dr.evomodel.beagle.substmodel.EigenDecomposition;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.DefaultBranchRateModel;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * TreeLikelihoodModel - implements a Likelihood Function for sequences on a tree.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Marc Suchard
 * @version $Id: TreeLikelihood.java,v 1.31 2006/08/30 16:02:42 rambaut Exp $
 */

public class BeagleTreeLikelihood extends AbstractTreeLikelihood {

    /**
     * Constructor.
     */
    public BeagleTreeLikelihood(PatternList patternList,
                                TreeModel treeModel,
                                BranchSiteModel branchSiteModel,
                                SiteRateModel siteRateModel,
                                BranchRateModel branchRateModel,
                                boolean useAmbiguities,
                                int deviceNumber,
                                boolean preferSinglePrecision
    ) {

        super(TreeLikelihoodParser.TREE_LIKELIHOOD, patternList, treeModel);

        try {
            final Logger logger = Logger.getLogger("dr.evomodel");

            logger.info("Using BEAGLE TreeLikelihood");

            this.siteRateModel = siteRateModel;
            addModel(this.siteRateModel);

            this.branchSiteModel = branchSiteModel;
            addModel(branchSiteModel);

            if (branchRateModel != null) {
                this.branchRateModel = branchRateModel;
                logger.info("Branch rate model used: " + branchRateModel.getModelName());
            } else {
                this.branchRateModel = new DefaultBranchRateModel();
            }
            addModel(this.branchRateModel);

            this.categoryCount = this.siteRateModel.getCategoryCount();

            this.tipCount = treeModel.getExternalNodeCount();

            internalNodeCount = nodeCount - tipCount;

            int compactPartialsCount = tipCount;
            int partialsCount = 0;
            if (useAmbiguities) {
                // if we are using ambiguities then we don't use tip partials
                compactPartialsCount = 0;
                partialsCount = tipCount;
            }
            partialsCount += 2 * categoryCount * internalNodeCount;
            int matrixCount = 2 * categoryCount * (nodeCount - 1);

            // override use preference on useAmbiguities based on actual ability of the likelihood core
//            if (!beagle.canHandleTipPartials()) {
//                useAmbiguities = false;
//            }
//            if (!beagle.canHandleTipStates()){
//                useAmbiguities = true;
//            }

            //           dynamicRescaling = likelihoodCore.canHandleDynamicRescaling();

            beagle = BeagleFactory.loadBeagleInstance(
                    tipCount,
                    partialsCount,
                    compactPartialsCount,
                    stateCount,
                    patternCount,
                    categoryCount,
                    matrixCount
            );

            partialBufferIndicators = new int[partialsCount];
            storedPartialBufferIndicators = new int[partialsCount];
            eigenIndicators = new int[categoryCount];
            storedEigenIndicators = new int[categoryCount];
            matrixIndicators = new int[matrixCount];
            storedMatrixIndicators = new int[matrixCount];
            rootBufferIndicies = new int[categoryCount];
            storedRootBufferIndicies = new int[categoryCount];

            for (int i = 0; i < tipCount; i++) {
                // Find the id of tip i in the patternList
                String id = treeModel.getTaxonId(i);
                int index = patternList.getTaxonIndex(id);

                if (index == -1) {
                    throw new TaxonList.MissingTaxonException("Taxon, " + id + ", in tree, " + treeModel.getId() +
                            ", is not found in patternList, " + patternList.getId());
                } else {
                    if (useAmbiguities) {
                        setPartials(beagle, patternList, index, i);
                    } else {
                        setStates(beagle, patternList, index, i);
                    }
                }
            }

            updateSubstitutionModel = true;
            updateSiteModel = true;

        } catch (TaxonList.MissingTaxonException mte) {
            throw new RuntimeException(mte.toString());
        }
        hasInitialized = true;
    }

    /**
     * Sets the partials from a sequence in an alignment.
     */
    protected final void setPartials(Beagle beagle,
                                     PatternList patternList,
                                     int sequenceIndex,
                                     int nodeIndex) {
        double[] partials = new double[patternCount * stateCount];

        boolean[] stateSet;

        int v = 0;
        for (int i = 0; i < patternCount; i++) {

            int state = patternList.getPatternState(sequenceIndex, i);
            stateSet = dataType.getStateSet(state);

            for (int j = 0; j < stateCount; j++) {
                if (stateSet[j]) {
                    partials[v] = 1.0;
                } else {
                    partials[v] = 0.0;
                }
                v++;
            }
        }

        beagle.setPartials(nodeIndex, partials);
    }

    /**
     * Sets the partials from a sequence in an alignment.
     */
    protected final void setStates(Beagle beagle,
                                   PatternList patternList,
                                   int sequenceIndex,
                                   int nodeIndex) {
        int i;

        int[] states = new int[patternCount];

        for (i = 0; i < patternCount; i++) {

            states[i] = patternList.getPatternState(sequenceIndex, i);
        }

        beagle.setTipStates(nodeIndex, states);
    }


    // **************************************************************
    // ModelListener IMPLEMENTATION
    // **************************************************************

    /**
     * Handles model changed events from the submodels.
     */
    protected void handleModelChangedEvent(Model model, Object object, int index) {

        if (model == treeModel) {
            if (object instanceof TreeModel.TreeChangedEvent) {

                if (((TreeModel.TreeChangedEvent) object).isNodeChanged()) {
                    // If a node event occurs the node and its two child nodes
                    // are flagged for updating (this will result in everything
                    // above being updated as well. Node events occur when a node
                    // is added to a branch, removed from a branch or its height or
                    // rate changes.
                    updateNodeAndChildren(((TreeModel.TreeChangedEvent) object).getNode());

                } else if (((TreeModel.TreeChangedEvent) object).isTreeChanged()) {
                    // Full tree events result in a complete updating of the tree likelihood
                    // Currently this event type is not used.
                    System.err.println("Full tree update event - these events currently aren't used\n" +
                            "so either this is in error or a new feature is using them so remove this message.");
                    updateAllNodes();
                } else {
                    // Other event types are ignored (probably trait changes).
                    //System.err.println("Another tree event has occured (possibly a trait change).");
                }
            }

        } else if (model == branchRateModel) {
            if (index == -1) {
                updateAllNodes();
            } else {
                updateNode(treeModel.getNode(index));
            }

        } else if (model == branchSiteModel) {

            updateSubstitutionModel = true;
            updateAllNodes();

        } else if (model == siteRateModel) {

            updateSubstitutionModel = true;
            updateSiteModel = true;
            updateAllNodes();

        } else {

            throw new RuntimeException("Unknown componentChangedEvent");
        }

        super.handleModelChangedEvent(model, object, index);
    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    /**
     * Stores the additional state other than model components
     */
    protected void storeState() {

        System.arraycopy(partialBufferIndicators, 0, storedPartialBufferIndicators, 0, partialBufferIndicators.length);
        System.arraycopy(eigenIndicators, 0, storedEigenIndicators, 0, eigenIndicators.length);
        System.arraycopy(matrixIndicators, 0, storedMatrixIndicators, 0, matrixIndicators.length);
        System.arraycopy(rootBufferIndicies, 0, storedRootBufferIndicies, 0, rootBufferIndicies.length);
        super.storeState();

    }

    /**
     * Restore the additional stored state
     */
    protected void restoreState() {
        int[] tmp = storedPartialBufferIndicators;
        storedPartialBufferIndicators = partialBufferIndicators;
        partialBufferIndicators = tmp;

        tmp = storedEigenIndicators;
        storedEigenIndicators = eigenIndicators;
        eigenIndicators = tmp;

        tmp = storedMatrixIndicators;
        storedMatrixIndicators = matrixIndicators;
        matrixIndicators = tmp;

        tmp = storedRootBufferIndicies;
        storedRootBufferIndicies = rootBufferIndicies;
        rootBufferIndicies = tmp;

        super.restoreState();

    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    protected double calculateLogLikelihood() {

        if (patternLogLikelihoods == null) {
            patternLogLikelihoods = new double[patternCount];
        }

        if (branchUpdateIndices == null) {
            branchUpdateIndices = new int[nodeCount];
            branchLengths = new double[nodeCount];
            edgeLengths = new double[nodeCount];
        }

        if (operations == null) {
            operations = new int[nodeCount * 3];
        }

        branchUpdateCount = 0;
        operationCount = 0;

        final NodeRef root = treeModel.getRoot();
        traverse(treeModel, root, null);

        if (updateSubstitutionModel) {
            // we are currently assuming a homogenous model...
            EigenDecomposition ed = branchSiteModel.getEigenDecomposition(0, 0);
            beagle.setEigenDecomposition(
                    0, // we are only dealing with a single matrix over all categories
                    ed.getEigenVectors(),
                    ed.getInverseEigenVectors(),
                    ed.getEigenValues());
        }

        double[] categoryRates = this.siteRateModel.getCategoryRates();
        double[] categoryProportions = this.siteRateModel.getCategoryProportions();
        double[] frequencies = branchSiteModel.getStateFrequencies(0);

        int k = 0;
        for (int i = 0; i < categoryCount; i++) {
            for (int j = 0; j < nodeCount; j++) {
                edgeLengths[k] = categoryRates[i] * branchLengths[j];
                k++;
            }
        }
        beagle.updateTransitionMatrices(0, branchUpdateIndices, null, null, edgeLengths, branchUpdateCount);

        beagle.updatePartials(operations, operationCount, false);

        nodeEvaluationCount += operationCount;

        beagle.calculateRootLogLikelihoods(rootBufferIndicies, categoryProportions, frequencies, patternLogLikelihoods);

        double logL = 0.0;
        for (int i = 0; i < patternCount; i++) {
            logL += patternLogLikelihoods[i] * patternWeights[i];
        }

        // Attempt dynamic rescaling if over/under-flow
        if (logL == Double.NaN || logL == Double.POSITIVE_INFINITY ) {

            System.err.println("Potential under/over-flow; going to attempt a partials rescaling.");
            updateAllNodes();
            branchUpdateCount = 0;
            operationCount = 0;
            traverse(treeModel, root, null);
            beagle.updatePartials(operations, operationCount,true);
            beagle.calculateRootLogLikelihoods(rootBufferIndicies, categoryProportions, frequencies, patternLogLikelihoods);

            logL = 0.0;
            for (int i = 0; i < patternCount; i++) {
                logL += patternLogLikelihoods[i] * patternWeights[i];
            }

        }

        //********************************************************************
        // after traverse all nodes and patterns have been updated --
        //so change flags to reflect this.
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = false;
        }

        updateSubstitutionModel = false;
        updateSiteModel = false;
        //********************************************************************

        return logL;
    }

    private int getPartialsBufferIndex(int nodeIndex, int category, int indicator) {
        if (nodeIndex < tipCount) {
            return tipCount;
        }

        return tipCount +
                (indicator * internalNodeCount * categoryCount) +
                (internalNodeCount * category) +
                (nodeIndex - tipCount);
    }

    /**
     * Traverse the tree calculating partial likelihoods.
     */
    private boolean traverse(Tree tree, NodeRef node, int[] operatorNumber) {

        boolean update = false;

        int nodeNum = node.getNumber();

        NodeRef parent = tree.getParent(node);

        if (operatorNumber != null) {
            operatorNumber[0] = -1;
        }

        // First update the transition probability matrix(ices) for this branch
        if (parent != null && updateNode[nodeNum]) {

            final double branchRate = branchRateModel.getBranchRate(tree, node);

            // Get the operational time of the branch
            final double branchTime = branchRate * (tree.getNodeHeight(parent) - tree.getNodeHeight(node));

            if (branchTime < 0.0) {
                throw new RuntimeException("Negative branch length: " + branchTime);
            }

            branchUpdateIndices[branchUpdateCount] = nodeNum;
            branchLengths[branchUpdateCount] = branchTime;
            branchUpdateCount++;

            update = true;
        }

        // If the node is internal, update the partial likelihoods.
        if (!tree.isExternal(node)) {

            // Traverse down the two child nodes
            NodeRef child1 = tree.getChild(node, 0);
            final int[] op1 = new int[] { -1 };
            final boolean update1 = traverse(tree, child1, op1);

            NodeRef child2 = tree.getChild(node, 1);
            final int[] op2 = new int[] { -1 };
            final boolean update2 = traverse(tree, child2, op2);

            // If either child node was updated then update this node too
            if (update1 || update2) {

                int x = operationCount * 5;
                operations[x] = nodeNum;
                operations[x + 1] = child1.getNumber(); // source node 1
                operations[x + 2] = child1.getNumber(); // source matrix 1
                operations[x + 3] = child2.getNumber(); // source node 2
                operations[x + 4] = child2.getNumber(); // source matrix 2

                operationCount ++;

                update = true;
            }
        }

        return update;

    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    private int[] branchUpdateIndices;
    private double[] edgeLengths;
    private double[] branchLengths;
    private int branchUpdateCount;

    private int[] operations;
    private int operationCount;

    private int[] partialBufferIndicators;
    private int[] storedPartialBufferIndicators;
    private int[] eigenIndicators;
    private int[] storedEigenIndicators;
    private int[] matrixIndicators;
    private int[] storedMatrixIndicators;
    private int[] rootBufferIndicies;
    private int[] storedRootBufferIndicies;

    private final int tipCount;
    private final int internalNodeCount;


    /**
     * the branch-site model for these sites
     */
    protected final BranchSiteModel branchSiteModel;

    /**
     * the site model for these sites
     */
    protected final SiteRateModel siteRateModel;
    /**
     * the branch rate model
     */
    protected final BranchRateModel branchRateModel;

    /**
     * the pattern likelihoods
     */
    protected double[] patternLogLikelihoods = null;

    /**
     * the number of rate categories
     */
    protected int categoryCount;

    /**
     * an array used to transfer tip partials
     */
    protected double[] tipPartials;

    /**
     * the BEAGLE library instance
     */
    protected Beagle beagle;

    /**
     * Flag to specify that the substitution model has changed
     */
    protected boolean updateSubstitutionModel;

    /**
     * Flag to specify that the site model has changed
     */
    protected boolean updateSiteModel;

    private int nodeEvaluationCount = 0;

    public int getNodeEvaluationCount() {
        return nodeEvaluationCount;
    }

//    /***
//     * Flag to specify if LikelihoodCore supports dynamic rescaling
//     */
//    private boolean dynamicRescaling = false;
}