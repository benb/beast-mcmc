/*
 * SkylineLikelihood.java
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

package dr.evomodel.coalescent;


import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.coalescent.GMRFSkyrideLikelihoodParser;
import dr.inference.model.MatrixParameter;
import dr.inference.model.Parameter;
import dr.inference.model.Variable;
import dr.math.MathUtils;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SymmTridiagEVD;
import no.uib.cipr.matrix.SymmTridiagMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * A likelihood function for a Gaussian Markov random field on a log population size trajectory.
 *
 * @author Jen Tom
 * @author Erik Bloomquist
 * @author Vladimir Minin
 * @author Marc Suchard
 * @version $Id: GMRFSkylineLikelihood.java,v 1.3 2007/03/20 22:40:04 msuchard Exp $
 */
public class GMRFSkyrideLikelihood extends OldAbstractCoalescentLikelihood {

	// PUBLIC STUFF

	public static final double LOG_TWO_TIMES_PI = 1.837877;
	public static final boolean TIME_AWARE_IS_ON_BY_DEFAULT = true;

	// PRIVATE STUFF

	protected Parameter popSizeParameter;
	protected Parameter groupSizeParameter;
	protected Parameter precisionParameter;
	protected Parameter lambdaParameter;
	protected Parameter betaParameter;
//	protected double[] gmrfWeights;
	protected int fieldLength;
	protected double[] coalescentIntervals;
	protected double[] storedCoalescentIntervals;
	protected double[] sufficientStatistics;
	protected double[] storedSufficientStatistics;

    private double logFieldLikelihood;
    private double storedLogFieldLikelihood;

	protected SymmTridiagMatrix weightMatrix;
	protected SymmTridiagMatrix storedWeightMatrix;
	protected MatrixParameter dMatrix;
	protected boolean timeAwareSmoothing = TIME_AWARE_IS_ON_BY_DEFAULT;

	public GMRFSkyrideLikelihood() {
		super(GMRFSkyrideLikelihoodParser.SKYLINE_LIKELIHOOD);
	}

	public GMRFSkyrideLikelihood(Tree tree, Parameter popParameter, Parameter groupParameter, Parameter precParameter,
	                             Parameter lambda, Parameter beta, MatrixParameter dMatrix,
	                             boolean timeAwareSmoothing) {
        this(wrapTree(tree), popParameter, groupParameter, precParameter, lambda, beta, dMatrix, timeAwareSmoothing);
    }

    private static List<Tree> wrapTree(Tree tree) {
        List<Tree> treeList = new ArrayList<Tree>();
        treeList.add(tree);
        return treeList;
    }

    public GMRFSkyrideLikelihood(List<Tree> treeList, Parameter popParameter, Parameter groupParameter, Parameter precParameter,
	                             Parameter lambda, Parameter beta, MatrixParameter dMatrix,
	                             boolean timeAwareSmoothing) {

		super(GMRFSkyrideLikelihoodParser.SKYLINE_LIKELIHOOD);

		this.popSizeParameter = popParameter;
		this.groupSizeParameter = groupParameter;
		this.precisionParameter = precParameter;
		this.lambdaParameter = lambda;
		this.betaParameter = beta;
		this.dMatrix = dMatrix;
		this.timeAwareSmoothing = timeAwareSmoothing;

        addVariable(popSizeParameter);
		addVariable(precisionParameter);
		addVariable(lambdaParameter);
		if (betaParameter != null) {
			addVariable(betaParameter);
        }

        setTree(treeList);

        int tips = 0;
        for (Tree tree : treeList) {
            tips += tree.getExternalNodeCount();
            if (tree instanceof TreeModel) {
                addModel((TreeModel) tree);
            }
        }

		fieldLength = popSizeParameter.getDimension();
		if (tips - fieldLength != treeList.size()) {
			throw new IllegalArgumentException("Number of tips (" + tips + ") must be one greater than number of pop sizes (" + fieldLength + ") for each locus");
		}

        // Field length must be set by this point
		setupIntervals();
		coalescentIntervals = new double[fieldLength];
		storedCoalescentIntervals = new double[fieldLength];
		sufficientStatistics = new double[fieldLength];
		storedSufficientStatistics = new double[fieldLength];

		setupGMRFWeights();

		addStatistic(new DeltaStatistic());

		initializationReport();

		/* Force all entries in groupSizeParameter = 1 for compatibility with Tracer */
		if (groupSizeParameter != null) {
			for (int i = 0; i < groupSizeParameter.getDimension(); i++)
				groupSizeParameter.setParameterValue(i, 1.0);
		}
	}

    protected void wrapSetupIntervals() {
        setupIntervals();
    }

    protected void setTree(List<Tree> treeList) {
        if (treeList.size() != 1) {
             throw new RuntimeException("GMRFSkyrideLikelihood only implemented for one tree");
        }
        this.tree = treeList.get(0);
        this.treesSet = null;
    }

	public double[] getCopyOfCoalescentIntervals() {
		return coalescentIntervals.clone();
	}

	public double[] getCoalescentIntervals() {
		return coalescentIntervals;
	}

	public void initializationReport() {
		System.out.println("Creating a GMRF smoothed skyride model:");
		System.out.println("\tPopulation sizes: " + popSizeParameter.getDimension());
		System.out.println("\tIf you publish results using this model, please reference: Minin, Bloomquist and Suchard (2008) Molecular Biology and Evolution, 25, 1459-1471.");
	}

	public static void checkTree(TreeModel treeModel) {

		// todo Should only be run if there exists a zero-length interval

//        TreeModel treeModel = (TreeModel) tree;
		for (int i = 0; i < treeModel.getInternalNodeCount(); i++) {
			NodeRef node = treeModel.getInternalNode(i);
			if (node != treeModel.getRoot()) {
				double parentHeight = treeModel.getNodeHeight(treeModel.getParent(node));
				double childHeight0 = treeModel.getNodeHeight(treeModel.getChild(node, 0));
				double childHeight1 = treeModel.getNodeHeight(treeModel.getChild(node, 1));
				double maxChild = childHeight0;
				if (childHeight1 > maxChild)
					maxChild = childHeight1;
				double newHeight = maxChild + MathUtils.nextDouble() * (parentHeight - maxChild);
				treeModel.setNodeHeight(node, newHeight);
			}
		}
		treeModel.pushTreeChangedEvent();

	}

	// **************************************************************
	// Likelihood IMPLEMENTATION
	// **************************************************************

	public double getLogLikelihood() {
		if (!likelihoodKnown) {
			logLikelihood = calculateLogCoalescentLikelihood();
            logFieldLikelihood = calculateLogFieldLikelihood();
			likelihoodKnown = true;
		}
		return logLikelihood + logFieldLikelihood;
	}

    protected double peakLogCoalescentLikelihood() {
        return logLikelihood;
    }

    protected double peakLogFieldLikelihood() {
        return logFieldLikelihood;
    }

    public double[] getSufficientStatistics() {
	return sufficientStatistics;
    }

    public String toString() {
        return getId() + "(" + Double.toString(getLogLikelihood()) + ")";
    }

    protected void setupSufficientStatistics() {
	    int index = 0;

		double length = 0;
		double weight = 0;
		for (int i = 0; i < getIntervalCount(); i++) {
			length += getInterval(i);
			weight += getInterval(i) * getLineageCount(i) * (getLineageCount(i) - 1);
			if (getIntervalType(i) == CoalescentEventType.COALESCENT) {
				coalescentIntervals[index] = length;
				sufficientStatistics[index] = weight / 2.0;
				index++;
				length = 0;
				weight = 0;
			}
		}
    }

	protected void setupGMRFWeights() {

//		int index = 0;
//
//		double length = 0;
//		double weight = 0;
//		for (int i = 0; i < getIntervalCount(); i++) {
//			length += getInterval(i);
//			weight += getInterval(i) * getLineageCount(i) * (getLineageCount(i) - 1);
//			if (getIntervalType(i) == CoalescentEventType.COALESCENT) {
//				coalescentIntervals[index] = length;
//				sufficientStatistics[index] = weight / 2.0;
//				index++;
//				length = 0;
//				weight = 0;
//
//			}
//
//		}
        setupSufficientStatistics();
		
		//Set up the weight Matrix
		double[] offdiag = new double[fieldLength - 1];
		double[] diag = new double[fieldLength];

		//First set up the offdiagonal entries;

		if (!timeAwareSmoothing) {
			for (int i = 0; i < fieldLength - 1; i++) {
				offdiag[i] = -1.0;
			}
			
			
		} else {
			double rootHeight = tree.getNodeHeight(tree.getRoot());
						
			for (int i = 0; i < fieldLength - 1; i++) {
				offdiag[i] = -2.0 / (coalescentIntervals[i] + coalescentIntervals[i + 1]) * rootHeight;
			}
		}

		//Then set up the diagonal entries;
		for (int i = 1; i < fieldLength - 1; i++)
			diag[i] = -(offdiag[i] + offdiag[i - 1]);

		//Take care of the endpoints
		diag[0] = -offdiag[0];
		diag[fieldLength - 1] = -offdiag[fieldLength - 2];

		weightMatrix = new SymmTridiagMatrix(diag, offdiag);
	}


	public SymmTridiagMatrix getScaledWeightMatrix(double precision) {
		SymmTridiagMatrix a = weightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, a.get(i, i) * precision);
			a.set(i + 1, i, a.get(i + 1, i) * precision);
		}
		a.set(fieldLength - 1, fieldLength - 1, a.get(fieldLength - 1, fieldLength - 1) * precision);
		return a;
	}

	public SymmTridiagMatrix getStoredScaledWeightMatrix(double precision) {
		SymmTridiagMatrix a = storedWeightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, a.get(i, i) * precision);
			a.set(i + 1, i, a.get(i + 1, i) * precision);
		}
		a.set(fieldLength - 1, fieldLength - 1, a.get(fieldLength - 1, fieldLength - 1) * precision);
		return a;
	}

	public SymmTridiagMatrix getScaledWeightMatrix(double precision, double lambda) {
		if (lambda == 1)
			return getScaledWeightMatrix(precision);

		SymmTridiagMatrix a = weightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, precision * (1 - lambda + lambda * a.get(i, i)));
			a.set(i + 1, i, a.get(i + 1, i) * precision * lambda);
		}

		a.set(fieldLength - 1, fieldLength - 1, precision * (1 - lambda + lambda * a.get(fieldLength - 1, fieldLength - 1)));
		return a;
	}

	public double[] getCoalescentIntervalHeights() {
		double[] a = new double[coalescentIntervals.length];

		a[0] = coalescentIntervals[0];

		for (int i = 1; i < a.length; i++) {
			a[i] = a[i - 1] + coalescentIntervals[i];
		}
		return a;
	}

	public SymmTridiagMatrix getCopyWeightMatrix() {
		return weightMatrix.copy();
	}

	public SymmTridiagMatrix getStoredScaledWeightMatrix(double precision, double lambda) {
		if (lambda == 1)
			return getStoredScaledWeightMatrix(precision);

		SymmTridiagMatrix a = storedWeightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, precision * (1 - lambda + lambda * a.get(i, i)));
			a.set(i + 1, i, a.get(i + 1, i) * precision * lambda);
		}

		a.set(fieldLength - 1, fieldLength - 1, precision * (1 - lambda + lambda * a.get(fieldLength - 1, fieldLength - 1)));
		return a;
	}


	protected void storeState() {
		super.storeState();
		System.arraycopy(coalescentIntervals, 0, storedCoalescentIntervals, 0, coalescentIntervals.length);
		System.arraycopy(sufficientStatistics, 0, storedSufficientStatistics, 0, sufficientStatistics.length);
		storedWeightMatrix = weightMatrix.copy();
        storedLogFieldLikelihood = logFieldLikelihood;
	}


	protected void restoreState() {
		super.restoreState();
		System.arraycopy(storedCoalescentIntervals, 0, coalescentIntervals, 0, storedCoalescentIntervals.length);
		System.arraycopy(storedSufficientStatistics, 0, sufficientStatistics, 0, storedSufficientStatistics.length);
		weightMatrix = storedWeightMatrix;
        logFieldLikelihood = storedLogFieldLikelihood;

	}

	protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type){
		likelihoodKnown = false;
        // Parameters (precision and popsizes do not change intervals or GMRF Q matrix
	}

	/**
	 * Calculates the log likelihood of this set of coalescent intervals,
	 * given a demographic model.
     * @return coalescent part of density
	 */
	private double calculateLogCoalescentLikelihood() {

		if (!intervalsKnown) {
			// intervalsKnown -> false when handleModelChanged event occurs in super.
			wrapSetupIntervals();
			setupGMRFWeights();
		}

		// Matrix operations taken from block update sampler to calculate data likelihood and field prior

		double currentLike = 0;
        double[] currentGamma = popSizeParameter.getParameterValues();

		for (int i = 0; i < fieldLength; i++) {
			currentLike += -currentGamma[i] - sufficientStatistics[i] * Math.exp(-currentGamma[i]);
		}

		return currentLike;// + LogNormalDistribution.logPdf(Math.exp(popSizeParameter.getParameterValue(coalescentIntervals.length - 1)), mu, sigma);
	}

    private double calculateLogFieldLikelihood() {

        if (!intervalsKnown) {
            // intervalsKnown -> false when handleModelChanged event occurs in super.
            wrapSetupIntervals();
            setupGMRFWeights();
        }

        double currentLike = 0;
        DenseVector diagonal1 = new DenseVector(fieldLength);
        DenseVector currentGamma = new DenseVector(popSizeParameter.getParameterValues());

        SymmTridiagMatrix currentQ = getScaledWeightMatrix(precisionParameter.getParameterValue(0), lambdaParameter.getParameterValue(0));
        currentQ.mult(currentGamma, diagonal1);

//        currentLike += 0.5 * logGeneralizedDeterminant(currentQ) - 0.5 * currentGamma.dot(diagonal1);

        currentLike += 0.5 * (fieldLength - 1) * Math.log(precisionParameter.getParameterValue(0)) - 0.5 * currentGamma.dot(diagonal1);
        if (lambdaParameter.getParameterValue(0) == 1) {
            currentLike -= (fieldLength - 1) / 2.0 * LOG_TWO_TIMES_PI;
        } else {
            currentLike -= fieldLength / 2.0 * LOG_TWO_TIMES_PI;
        }

        return currentLike;
    }


	public static double logGeneralizedDeterminant(SymmTridiagMatrix X) {
		//Set up the eigenvalue solver
		SymmTridiagEVD eigen = new SymmTridiagEVD(X.numRows(), false);
		//Solve for the eigenvalues
		try {
			eigen.factor(X);
		} catch (NotConvergedException e) {
			throw new RuntimeException("Not converged error in generalized determinate calculation.\n" + e.getMessage());
		}

		//Get the eigenvalues
		double[] x = eigen.getEigenvalues();

		double a = 0;
		for (double d : x) {
			if (d > 0.00001)
				a += Math.log(d);
		}

		return a;
	}


	public Parameter getPrecisionParameter() {
		return precisionParameter;
	}

	public Parameter getPopSizeParameter() {
		return popSizeParameter;
	}

	public Parameter getLambdaParameter() {
		return lambdaParameter;
	}

	public SymmTridiagMatrix getWeightMatrix() {
		return weightMatrix.copy();
	}

	public Parameter getBetaParameter() {
		return betaParameter;
	}

	public MatrixParameter getDesignMatrix() {
		return dMatrix;
	}

	public double calculateWeightedSSE() {
		double weightedSSE = 0;
		double currentPopSize = popSizeParameter.getParameterValue(0);
		double currentInterval = coalescentIntervals[0];
		for (int j = 1; j < fieldLength; j++) {
			double nextPopSize = popSizeParameter.getParameterValue(j);
			double nextInterval = coalescentIntervals[j];
			double delta = nextPopSize - currentPopSize;
			double weight = (currentInterval + nextInterval) / 2.0;
			weightedSSE += delta * delta / weight;
			currentPopSize = nextPopSize;
			currentInterval = nextInterval;
		}
		return weightedSSE;

	}

	// ****************************************************************
	// Private and protected stuff
	// ****************************************************************
}

/*

WinBUGS code to fixed tree:  (A:4.0,(B:2.0,(C:0.5,D:1.0):1.0):2.0)

model {

    stat1 ~ dexp(rate[1])
    stat2 ~ dexp(rate[2])
    stat3 ~ dexp(rate[3])

    rate[1] <- 1 / exp(theta[1])
    rate[2] <- 1 / exp(theta[2])
    rate[3] <- 1 / exp(theta[3])

    theta[1] ~ dnorm(0, 0.001)
    theta[2] ~ dnorm(theta[1], weight[1])
    theta[3] ~ dnorm(theta[2], weight[2])

    weight[1] <- tau / 1.0
    weight[2] <- tau / 1.5

    tau ~ dgamma(1,0.3333)

    stat1 <- 9 / 2
    stat2 <- 6 / 2
    stat3 <- 4 / 2

}
*/