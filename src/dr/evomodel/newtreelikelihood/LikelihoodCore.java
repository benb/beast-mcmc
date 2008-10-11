/*
 * LikelihoodCore.java
 *
 * Copyright (C) 2002-2007 Alexei Drummond and Andrew Rambaut
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

package dr.evomodel.newtreelikelihood;

import dr.evomodel.substmodel.SubstitutionModel;

/**
 * LikelihoodCore - An interface describing the core likelihood functions.
 *
 * @author Andrew Rambaut
 * @version $Id: LikelihoodCore.java,v 1.15 2006/08/29 18:07:23 rambaut Exp $
 */

public interface LikelihoodCore {

    /**
     * initializes partial likelihood arrays.
     * @param nodeCount the number of nodes in the tree (internal and external)
     * @param patternCount the number of patterns
     * @param matrixCount the number of rate categories
     */
    void initialize(int nodeCount, int patternCount, int matrixCount);

    /**
     * cleans up and deallocates arrays.
     */
    void finalize() throws Throwable;

    /**
     * Sets partials for a tip - these are numbered from 0 and remain
     * constant throughout the run.
     * @param tipIndex the tip index
     * @param partials an array of partial likelihoods (patternCount * stateCount)
     */
    void setTipPartials(int tipIndex, double[] partials);

    /**
     * Called when the substitution model has been updated so precalculations
     * can be obtained.
     */
    void updateSubstitutionModel(SubstitutionModel substitutionModel);

    /**
     * Specify the branch lengths that are being updated. These will be used to construct
     * the transition probability matrices for each branch.
     * @param branchUpdateIndices the node indices of the branches to be updated
     * @param branchLengths the branch lengths of the branches to be updated
     * @param branchUpdateCount the number of branch updates
     * @param matrixRates the relative rates for each rate category
     */
    void updateMatrices(int[] branchUpdateIndices, double[] branchLengths, int branchUpdateCount,
                        double[] matrixRates);

    /**
     * Specify the updates to be made. This specifies which partials are to be
     * calculated and by giving the dependencies between the operations, they
     * can be done in parallel if required.
     * @param operations an array of partial likelihood calculations to be done.
     *      This is an array of triplets of node numbers specifying the two source
     *      (descendent) nodes and the destination node for which the partials are
     *      being calculated.
     * @param dependencies an array of dependencies for each of the operations
     *      This is an array of pairs of integers for each of the operations above.
     *      The first of each pair specifies which future operation is dependent
     *      on this one. The second is just a boolean (0,1) as to whether this operation
     *      is dependent on another. If these dependencies are not used then the
     *      operations can safely be done in order.
     * @param operationCount the number of operators
     */
    void updatePartials(int[] operations, int[] dependencies, int operationCount);

    /**
     * Calculates pattern log likelihoods at a node.
     *
     * @param rootNodeIndex the index of the root node
     * @param frequencies an array of state frequencies
     * @param proportions the proportion of patterns in each rate category
     * @param outLogLikelihoods an array into which the log likelihoods will go
     */
    void calculateLogLikelihoods(int rootNodeIndex, double[] frequencies, double[] proportions, double[] outLogLikelihoods);

    /**
     * Store current state
     */
    void storeState();

    /**
     * Restore the stored state
     */
    void restoreState();

}