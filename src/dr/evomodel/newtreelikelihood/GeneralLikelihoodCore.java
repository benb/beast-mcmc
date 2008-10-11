/*
 * AbstractLikelihoodCore.java
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

package dr.evomodel.newtreelikelihood;

import dr.evomodel.substmodel.SubstitutionModel;
import dr.evomodel.sitemodel.SiteModel;

public class GeneralLikelihoodCore implements LikelihoodCore {

    protected int stateCount;
    protected int nodeCount;
    protected int patternCount;
    protected int partialsSize;
    protected int matrixSize;
    protected int matrixCount;

    protected double[] cMatrix;
    protected double[] storedCMatrix;
    protected double[] eigenValues;
    protected double[] storedEigenValues;

    protected double[] frequencies;
    protected double[] storedFrequencies;
    protected double[] proportions;
    protected double[] categoryRates;
    protected double[] storedCategoryRates;

    protected double[][][] partials;

    protected int[][] states;

    protected double[][][] matrices;

    protected int[] currentMatricesIndices;
    protected int[] storedMatricesIndices;
    protected int[] currentPartialsIndices;
    protected int[] storedPartialsIndices;


    /**
     * Constructor
     *
     * @param stateCount number of states
     */
    public GeneralLikelihoodCore(int stateCount) {
        this.stateCount = stateCount;
    }

    /**
     * initializes partial likelihood arrays.
     *
     * @param nodeCount           the number of nodes in the tree
     * @param patternCount        the number of patterns
     * @param matrixCount         the number of matrices (i.e., number of categories)
     */
    public void initialize(int nodeCount, int patternCount, int matrixCount) {

        this.nodeCount = nodeCount;
        this.patternCount = patternCount;
        this.matrixCount = matrixCount;

        cMatrix = new double[stateCount * stateCount * stateCount];
        storedCMatrix = new double[stateCount * stateCount];

        eigenValues = new double[stateCount];
        storedEigenValues = new double[stateCount];

        frequencies = new double[stateCount];
        storedFrequencies = new double[stateCount];

        categoryRates = new double[matrixCount];
        storedCategoryRates = new double[matrixCount];

        proportions = new double[matrixCount];

        partialsSize = patternCount * stateCount * matrixCount;

        partials = new double[2][nodeCount][];

        currentMatricesIndices = new int[nodeCount];
        storedMatricesIndices = new int[nodeCount];

        currentPartialsIndices = new int[nodeCount];
        storedPartialsIndices = new int[nodeCount];

        states = new int[nodeCount][];

        for (int i = 0; i < nodeCount; i++) {
            partials[0][i] = new double[partialsSize];
            partials[1][i] = new double[partialsSize];
        }

        matrixSize = stateCount * stateCount;

        matrices = new double[2][nodeCount][matrixCount * matrixSize];
    }

    /**
     * cleans up and deallocates arrays.
     */
    public void finalize() throws Throwable  {
        super.finalize();

        nodeCount = 0;
        patternCount = 0;
        matrixCount = 0;

        partials = null;
        currentPartialsIndices = null;
        storedPartialsIndices = null;
        states = null;
        matrices = null;
        currentMatricesIndices = null;
        storedMatricesIndices = null;
    }


    /**
     * Sets partials for a tip
     */
    public void setTipPartials(int tipIndex, double[] partials) {

        if (partials.length < partialsSize) {
            // duplicate out the partials for each matrix

            int k = 0;
            for (int i = 0; i < matrixCount; i++) {
                System.arraycopy(partials, 0, this.partials[0][tipIndex], k, partials.length);
                k += partials.length;
            }
        } else {
            System.arraycopy(partials, 0, this.partials[0][tipIndex], 0, partials.length);
        }
    }

    /**
     * Called when the substitution model has been updated so precalculations
     * can be obtained.
     */
    public void updateSubstitutionModel(SubstitutionModel substitutionModel) {
        for (int i = 0; i < stateCount; i++) {
            substitutionModel.getFrequencyModel().getFrequency(i);
        }
        substitutionModel.getEigenDecomposition(cMatrix, eigenValues);
    }

    /**
     * Called when the site model has been updated so rates and proportions
     * can be obtained.
     */
    public void updateSiteModel(SiteModel siteModel) {
        for (int i = 0; i < matrixCount; i++) {
            categoryRates[i] = siteModel.getRateForCategory(i);
        }
        for (int i = 0; i < matrixCount; i++) {
            proportions[i] = siteModel.getProportionForCategory(i);
        }
    }

    /**
     * Specify the branch lengths that are being updated. These will be used to construct
     * the transition probability matrices for each branch.
     *
     * @param branchUpdateIndices the node indices of the branches to be updated
     * @param branchLengths       the branch lengths of the branches to be updated
     * @param branchUpdateCount   the number of branch updates
     */
    public void updateMatrices(int[] branchUpdateIndices, double[] branchLengths, int branchUpdateCount) {
//        currentMatricesIndices[nodeIndex] = 1 - currentMatricesIndices[nodeIndex];
        for (int i = 0; i < branchUpdateCount; i++) {
            calculateMatrix(branchUpdateIndices[i], branchLengths[i]);
        }
    }

    private void calculateMatrix(int nodeIndex, double branchLength) {
        double[] tmp = new double[stateCount];

        for (int i = 0; i < stateCount; i++) {
            tmp[i] =  Math.exp(eigenValues[i] * branchLength);
        }

        int l = 0;
        int m = 0;
        for (int i = 0; i < stateCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                double sum = 0.0;
                for (int k = 0; k < stateCount; k++) {
                    sum += cMatrix[l] * tmp[k];
                    l++;
                }
                matrices[currentMatricesIndices[nodeIndex]][nodeIndex][m] = sum;
                m++;
            }
        }
    }

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
    public void updatePartials(int[] operations, int[] dependencies, int operationCount) {

        int x = 0;
        for (int op = 0; op < operationCount; op++) {
            int nodeIndex1 = operations[x];
            x++;
            int nodeIndex2 = operations[x];
            x++;
            int nodeIndex3 = operations[x];
            x++;

            double[] matrices1 = matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1];
            double[] matrices2 = matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2];

            double[] partials1 = partials[currentPartialsIndices[nodeIndex1]][nodeIndex1];
            double[] partials2 = partials[currentPartialsIndices[nodeIndex2]][nodeIndex2];
            double[] partials3 = partials[currentPartialsIndices[nodeIndex3]][nodeIndex3];

            double sum1, sum2;

            int u = 0;
            int v = 0;

            for (int l = 0; l < matrixCount; l++) {

                for (int k = 0; k < patternCount; k++) {

                    int w = l * matrixSize;

                    for (int i = 0; i < stateCount; i++) {

                        sum1 = sum2 = 0.0;

                        for (int j = 0; j < stateCount; j++) {
                            sum1 += matrices1[w] * partials1[v + j];
                            sum2 += matrices2[w] * partials2[v + j];

                            w++;
                        }

                        partials3[u] = sum1 * sum2;
                        u++;
                    }
                    v += stateCount;
                }
            }
        }
    }

    /**
     * Calculates pattern log likelihoods at a node.
     *
     * @param rootNodeIndex the index of the root node
     * @param outLogLikelihoods an array into which the log likelihoods will go
     */
    public void calculateLogLikelihoods(int rootNodeIndex, double[] outLogLikelihoods) {

        // @todo I have a feeling this could be done in a single set of nested loops.

        double[] rootPartials = partials[currentPartialsIndices[rootNodeIndex]][rootNodeIndex];

        double[] tmp = new double[patternCount * stateCount];

        int u = 0;
        int v = 0;
        for (int k = 0; k < patternCount; k++) {

            for (int i = 0; i < stateCount; i++) {

                tmp[u] = rootPartials[v] * proportions[0];
                u++;
                v++;
            }
        }


        for (int l = 1; l < matrixCount; l++) {
            u = 0;

            for (int k = 0; k < patternCount; k++) {

                for (int i = 0; i < stateCount; i++) {

                    tmp[u] += rootPartials[v] * proportions[l];
                    u++;
                    v++;
                }
            }
        }

        u = 0;
        for (int k = 0; k < patternCount; k++) {

            double sum = 0.0;
            for (int i = 0; i < stateCount; i++) {

                sum += frequencies[i] * tmp[u];
                u++;
            }
            outLogLikelihoods[k] = Math.log(sum);
        }
    }

//    public void setNodePartialsForUpdate(int nodeIndex) {
//        currentPartialsIndices[nodeIndex] = 1 - currentPartialsIndices[nodeIndex];
//    }

    /**
     * Store current state
     */
    public void storeState() {

        System.arraycopy(cMatrix, 0, storedCMatrix, 0, cMatrix.length);
        System.arraycopy(eigenValues, 0, storedEigenValues, 0, eigenValues.length);

        System.arraycopy(frequencies, 0, storedFrequencies, 0, frequencies.length);
        System.arraycopy(categoryRates, 0, storedCategoryRates, 0, categoryRates.length);

        System.arraycopy(currentMatricesIndices, 0, storedMatricesIndices, 0, nodeCount);
        System.arraycopy(currentPartialsIndices, 0, storedPartialsIndices, 0, nodeCount);
    }

    /**
     * Restore the stored state
     */
    public void restoreState() {
        // Rather than copying the stored stuff back, just swap the pointers...
        double[] tmp = cMatrix;
        cMatrix = storedCMatrix;
        storedCMatrix = tmp;

        tmp = eigenValues;
        eigenValues = storedEigenValues;
        storedEigenValues = tmp;

        tmp = frequencies;
        frequencies = storedFrequencies;
        storedFrequencies = tmp;

        tmp = categoryRates;
        categoryRates = storedCategoryRates;
        storedCategoryRates = tmp;

        int[] tmp3 = currentMatricesIndices;
        currentMatricesIndices = storedMatricesIndices;
        storedMatricesIndices = tmp3;

        int[] tmp4 = currentPartialsIndices;
        currentPartialsIndices = storedPartialsIndices;
        storedPartialsIndices = tmp4;
    }
}