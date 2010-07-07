/*
 * ColouredTreeRateModel.java
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

package dr.evomodel.branchratemodel;

import dr.evolution.alignment.PatternList;
import dr.evolution.datatype.DataType;
import dr.evolution.parsimony.FitchParsimony;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeTrait;
import dr.evolution.tree.TreeTraitProvider;
import dr.evolution.util.TaxonList;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.inference.model.Variable;
import dr.math.matrixAlgebra.Vector;

/**
 * This Branch Rate Model takes a ancestral state likelihood and
 * gives the rate for each branch of the tree based on the child state (for now).
 *
 * @author Alexei Drummond
 * @author Marc Suchard
 */
public class DiscreteTraitBranchRateModel extends AbstractBranchRateModel {
    enum Mode {
        NODE_STATES,
        MARKOV_JUMP_PROCESS,
        PARSIMONY
    }

    public static final String DISCRETE_TRAIT_BRANCH_RATE_MODEL = "discreteTraitRateModel";

    protected TreeTrait trait = null;
    private Parameter rateParameter;
    private Parameter relativeRatesParameter;
    private Parameter indicatorParameter;
    protected int traitIndex;
    private boolean normKnown = false;
    private boolean storedNormKnown = false;
    private double norm = 1.0;
    private double storedNorm = 1.0;

    private FitchParsimony fitchParsimony;

    private boolean treeChanged = true;
    private boolean shouldRestoreTree = false;

    private Mode mode;
    private DataType dataType;
//    private int treeInitializeCounter = 0;

    /**
     * A constructor for the (crude) parsimony reconstruction form of this class.
     * @param treeModel
     * @param patternList
     * @param traitIndex
     * @param ratesParameter
     */
    public DiscreteTraitBranchRateModel(TreeModel treeModel, PatternList patternList, int traitIndex, Parameter ratesParameter) {

        this(treeModel, traitIndex, ratesParameter, null, null);

        if (!TaxonList.Utils.getTaxonListIdSet(treeModel).equals(TaxonList.Utils.getTaxonListIdSet(patternList))) {
            throw new IllegalArgumentException("Tree model and pattern list must have the same list of taxa!");
        }

        fitchParsimony = new FitchParsimony(patternList, false);
        mode = Mode.PARSIMONY;
    }

    /**
     * A constructor for a node-sampled discrete trait
     * @param treeModel
     * @param trait
     * @param traitIndex
     * @param rateParameter
     * @param relativeRatesParameter
     * @param indicatorParameter
     */
    public DiscreteTraitBranchRateModel(TreeTraitProvider traitProvider, DataType dataType, TreeModel treeModel,
                                        TreeTrait trait, int traitIndex, Parameter rateParameter,
                                        Parameter relativeRatesParameter, Parameter indicatorParameter) {

        this(treeModel, traitIndex, rateParameter, relativeRatesParameter, indicatorParameter);

//        if (trait.getTreeModel() != treeModel)
//            throw new IllegalArgumentException("Tree Models for ancestral state tree likelihood and target model of these rates must match!");

        this.trait = trait;
        this.dataType = dataType;

        if (trait.getTraitName().equals("states")) {
            // Assume the trait is one or more discrete traits reconstructed at nodes
            mode = Mode.NODE_STATES;
        } else /*if (double[].class.isAssignableFrom(trait.getClass()))*/ {
            // Assume the trait itself is the dwell times for the individual states on the branch above the node
            mode = Mode.MARKOV_JUMP_PROCESS;
        } /* else {
            throw new IllegalArgumentException("The trait class type is not suitable for use in this class.");
        } */

        if (traitProvider instanceof Model) {
            addModel((Model)traitProvider);
        }

        if (trait instanceof Model) {
            addModel((Model)trait); // MAS: Does this ever occur?
        }
    }

    private DiscreteTraitBranchRateModel(TreeModel treeModel, int traitIndex,
                                         Parameter rateParameter, Parameter relativeRatesParameter, Parameter indicatorParameter) {
        super(DISCRETE_TRAIT_BRANCH_RATE_MODEL);
        addModel(treeModel);
        this.traitIndex = traitIndex;

        this.rateParameter = rateParameter;
        addVariable(rateParameter);

        this.relativeRatesParameter = relativeRatesParameter;
        if (relativeRatesParameter != null) {
            addVariable(relativeRatesParameter);
        }

        this.indicatorParameter = indicatorParameter;
        if (indicatorParameter != null) {
            addVariable(indicatorParameter);
        }

//        double[] dwellTimes = getProcessValues(treeModel, treeModel.getExternalNode(0));
//
//        if (indicatorParameter != null) {
//            if (dwellTimes.length != indicatorParameter.getDimension()) {
//                throw new IllegalArgumentException("The dwell times must have same dimension as indicator parameter.");
//            }
//        } else {
//            if (dwellTimes.length != rateParameter.getDimension()) {
//                throw new IllegalArgumentException("The dwell times must have same dimension as rates parameter.");
//            }
//        }
    }

    public void handleModelChangedEvent(Model model, Object object, int index) {
        // TreeModel has changed...
        normKnown = false;
        if (model instanceof TreeModel) {
            treeChanged = true;
            shouldRestoreTree = true;
        }
        fireModelChanged();
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // Rate Parameters have changed
        //ratesCalculated = false;
        normKnown = false;
        fireModelChanged();
    }

    protected void storeState() {
        storedNormKnown = normKnown;
        storedNorm = norm;
        shouldRestoreTree = false;
    }

    protected void restoreState() {
        normKnown = storedNormKnown;
        norm = storedNorm;
        treeChanged = shouldRestoreTree;
    }

    protected void acceptState() {
        // nothing to do
    }

    public double getBranchRate(final Tree tree, final NodeRef node) {

        double rate = getRawBranchRate(tree, node);

//        System.out.println("rate = " + rate);

//        if (!normKnown) {
//            norm = calculateNorm(tree);
//            normKnown = true;
//        }
//        return getRawBranchRate(tree, node) / norm;

        // AR - I am not sure the normalization is required here?

        return rate;
    }

    double calculateNorm(Tree tree) {

        double time = 0.0;
        double rateTime = 0.0;
        for (int i = 0; i < tree.getNodeCount(); i++) {

            NodeRef node = tree.getNode(i);

            if (!tree.isRoot(node)) {

                double branchTime = tree.getBranchLength(node);

                rateTime += getRawBranchRate(tree, node) * branchTime;
                time += branchTime;
            }

        }
        return rateTime / time;
    }

    protected double getRawBranchRate(final Tree tree, final NodeRef node) {

        double rate = 0.0;
        double[] processValues = getProcessValues(tree, node);

        double totalTime = 0;
        if (indicatorParameter != null) {
            double absRate = rateParameter.getParameterValue(0);

            for (int i = 0; i < indicatorParameter.getDimension(); i++) {
                int index = (int)indicatorParameter.getParameterValue(i);
                if (index == 0) {
                    rate += absRate * processValues[i];
                } else {
                    rate += (absRate * (1.0 + relativeRatesParameter.getParameterValue(index - 1))) * processValues[i];
                }
                totalTime += processValues[i];
            }
        } else {
            for (int i = 0; i < rateParameter.getDimension(); i++) {
                rate += rateParameter.getParameterValue(i) * processValues[i];
                totalTime += processValues[i];
            }
        }
        rate /= totalTime;

        return rate;
    }

    /**
     *
     * @param tree
     * @param node
     * @return and array of the total amount of time spent in each of the discrete states along the branch above the given node.
     */
    private double[] getProcessValues(final Tree tree, final NodeRef node) {

        double[] processValues = null;
        double branchTime = tree.getBranchLength(node);

        if (mode == Mode.MARKOV_JUMP_PROCESS) {
            processValues = (double[])trait.getTrait(tree, node);
        } else if (mode == Mode.PARSIMONY) {
            // an approximation to dwell times using parsimony, assuming
            // the state changes midpoint on the tree. Does a weighted
            // average of the equally parsimonious state reconstructions
            // at the top and bottom of each branch.

            if (treeChanged) {
                fitchParsimony.initialize(tree);
                // Debugging test to count work
//                treeInitializeCounter += 1;
//                if (treeInitializeCounter % 10 == 0) {
//                    System.err.println("Cnt: "+treeInitializeCounter);
//                }
                treeChanged = false;
            }
            int[] states = fitchParsimony.getStates(tree, node);
            int[] parentStates = fitchParsimony.getStates(tree, tree.getParent(node));

            processValues = new double[fitchParsimony.getPatterns().getStateCount()];

            for (int state : states) {
                processValues[state] += branchTime / 2;
            }
            for (int state : parentStates) {
                processValues[state] += branchTime / 2;
            }

            for (int i = 0; i < processValues.length; i++) {
                // normalize by the number of equally parsimonious states at each end of the branch
                // processValues should add up to the total branch length
                processValues[i] /= (states.length + parentStates.length) / 2;
            }
        } else if (mode == Mode.NODE_STATES) {
              processValues = new double[dataType.getStateCount()];
//            if (indicatorParameter != null) {
//                // this array should be size #states NOT #rates
//                processValues = new double[indicatorParameter.getDimension()];
//            } else {
//                // this array should be size #states NOT #rates
//                processValues = new double[rateParameter.getDimension()];
//            }

            // if the states are being sampled - then there is only one possible state at each
            // end of the branch.
            int state = ((int[])trait.getTrait(tree, node))[traitIndex];
            processValues[state] += branchTime / 2;
            int parentState = ((int[])trait.getTrait(tree, tree.getParent(node)))[traitIndex];
            processValues[parentState] += branchTime / 2;
        }

        return processValues;
    }

}