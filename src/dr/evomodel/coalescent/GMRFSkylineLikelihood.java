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


import dr.evolution.tree.Tree;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.Likelihood;
import dr.inference.model.MatrixParameter;
import dr.inference.model.Parameter;
import dr.xml.*;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SymmTridiagEVD;
import no.uib.cipr.matrix.SymmTridiagMatrix;

/**
 * A likelihood function for a Gaussian Markov random field on a log population size trajectory.
 *
 * @author Jen Tom
 * @author Erik Bloomquist
 * @author Vladimir Minin
 * @author Marc Suchard
 * @version $Id: GMRFSkylineLikelihood.java,v 1.3 2007/03/20 22:40:04 msuchard Exp $
 */
public class GMRFSkylineLikelihood extends CoalescentLikelihood {

    // PUBLIC STUFF

    public static final String SKYLINE_LIKELIHOOD = "gmrfSkyLineLikelihood";
    public static final String POPULATION_PARAMETER = "populationSizes";
    public static final String PRECISION_PARAMETER = "precisionParameter";
    public static final String POPULATION_TREE = "populationTree";
    public static final String LAMBDA_PARAMETER = "lambdaParameter";
    public static final String BETA_PARAMETER = "betaParameter";
    public static final String COVARIATE_MATRIX = "covariateMatrix";
    public static final double LOG_TWO_TIMES_PI = 1.837877;

    // PRIVATE STUFF

    protected Parameter popSizeParameter;
    protected Parameter precisionParameter;
    protected Parameter lambdaParameter;
    protected Parameter betaParameter;
    protected double[] gmrfWeights;
    protected int fieldLength;
    protected double[] coalescentIntervals;
    protected double[] storedCoalescentIntervals;
    protected double[] sufficientStatistics;
    protected double[] storedSufficientStatistics;

    protected SymmTridiagMatrix weightMatrix;
    protected SymmTridiagMatrix storedWeightMatrix;
    protected MatrixParameter dMatrix;

    public GMRFSkylineLikelihood() {
        super(SKYLINE_LIKELIHOOD);
    }

    public GMRFSkylineLikelihood(Tree tree, Parameter popParameter, Parameter precParameter,
                                 Parameter lambda, Parameter beta, MatrixParameter dMatrix) {
        super(SKYLINE_LIKELIHOOD);

        this.popSizeParameter = popParameter;
        this.precisionParameter = precParameter;
        this.lambdaParameter = lambda;
        this.betaParameter = beta;
        this.dMatrix = dMatrix;

        int tips = tree.getExternalNodeCount();
        fieldLength = popSizeParameter.getDimension();
        if (tips - fieldLength != 1) {
            throw new IllegalArgumentException("Number of tips (" + tips + ") must be one greater than number of pop sizes (" + fieldLength + ")");
        }

        this.tree = tree;
        if (tree instanceof TreeModel) {
            addModel((TreeModel) tree);
        }
        addParameter(popSizeParameter);
        addParameter(precisionParameter);
        addParameter(lambdaParameter);
        if (betaParameter != null)
            addParameter(betaParameter);

        setupIntervals();
        coalescentIntervals = new double[fieldLength];
        storedCoalescentIntervals = new double[fieldLength];
        sufficientStatistics = new double[fieldLength];
        storedSufficientStatistics = new double[fieldLength];

        setupGMRFWeights();

        addStatistic(new DeltaStatistic());

        initializationReport();

    }

    public void initializationReport() {
        System.out.println("Creating a GMRF smoothed skyline model:");
        System.out.println("\tPopulation sizes: " + popSizeParameter.getDimension());
        System.out.println("\tIf you publish results using this model, please reference: Minin, Bloomquist and Suchard (in submission).");
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public double getLogLikelihood() {
        if (!likelihoodKnown) {
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    public double[] getSufficientStatistics() {
        return sufficientStatistics;
    }

    protected void setupGMRFWeights() {

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

        //Set up the weight Matrix
        double[] offdiag = new double[fieldLength - 1];
        double[] diag = new double[fieldLength];

        //First set up the offdiagonal entries;
        for (int i = 0; i < fieldLength - 1; i++) {
            offdiag[i] = -2.0 / (coalescentIntervals[i] + coalescentIntervals[i + 1]);
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

    public SymmTridiagMatrix getCopyWeightMatrix() {
        return weightMatrix.copy();
    }


    protected void storeState() {
        super.storeState();
        System.arraycopy(coalescentIntervals, 0, storedCoalescentIntervals, 0, coalescentIntervals.length);
        System.arraycopy(sufficientStatistics, 0, storedSufficientStatistics, 0, sufficientStatistics.length);
        storedWeightMatrix = weightMatrix.copy();
    }


    protected void restoreState() {
        super.restoreState();
        System.arraycopy(storedCoalescentIntervals, 0, coalescentIntervals, 0, storedCoalescentIntervals.length);
        System.arraycopy(storedSufficientStatistics, 0, sufficientStatistics, 0, storedSufficientStatistics.length);
        weightMatrix = storedWeightMatrix;

    }


    protected void handleParameterChangedEvent(Parameter parameter, int index) {
        likelihoodKnown = false;
        // Parameters (precision and popsizes do not change intervals or GMRF Q matrix

    }

    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     */
    public double calculateLogLikelihood() {

        if (!intervalsKnown) {
            // intervalsKnown -> false when handleModelChanged event occurs in super.
            setupIntervals();
            setupGMRFWeights();
        }

        // Matrix operations taken from block update sampler to calculate data likelihood and field prior

        double currentLike = 0;
        DenseVector diagonal1 = new DenseVector(fieldLength);
        DenseVector currentGamma = new DenseVector(popSizeParameter.getParameterValues());

        for (int i = 0; i < fieldLength; i++) {
            currentLike += -currentGamma.get(i) - sufficientStatistics[i] * Math.exp(-currentGamma.get(i));
        }


        SymmTridiagMatrix currentQ = getScaledWeightMatrix(precisionParameter.getParameterValue(0), lambdaParameter.getParameterValue(0));
        currentQ.mult(currentGamma, diagonal1);

//        currentLike += 0.5 * logGeneralizedDeterminant(currentQ) - 0.5 * currentGamma.dot(diagonal1);

        currentLike += 0.5 * (fieldLength - 1)*Math.log(precisionParameter.getParameterValue(0)) - 0.5*currentGamma.dot(diagonal1);
        if (lambdaParameter.getParameterValue(0) == 1) {
            currentLike -= (fieldLength - 1) / 2.0 * LOG_TWO_TIMES_PI;
        } else {
            currentLike -= fieldLength / 2.0 * LOG_TWO_TIMES_PI;
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

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return SKYLINE_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            XMLObject cxo = (XMLObject) xo.getChild(POPULATION_PARAMETER);
            Parameter popParameter = (Parameter) cxo.getChild(Parameter.class);

            cxo = (XMLObject) xo.getChild(PRECISION_PARAMETER);
            Parameter precParameter = (Parameter) cxo.getChild(Parameter.class);

            cxo = (XMLObject) xo.getChild(POPULATION_TREE);
            TreeModel treeModel = (TreeModel) cxo.getChild(TreeModel.class);

            Parameter lambda;
            if (xo.getChild(LAMBDA_PARAMETER) != null) {
                cxo = (XMLObject) xo.getChild(LAMBDA_PARAMETER);
                lambda = (Parameter) cxo.getChild(Parameter.class);
            } else {
                lambda = new Parameter.Default(1.0);
            }

            Parameter beta = null;
            if (xo.getChild(BETA_PARAMETER) != null) {
                cxo = (XMLObject) xo.getChild(BETA_PARAMETER);
                beta = (Parameter) cxo.getChild(Parameter.class);
            }

            MatrixParameter dMatrix = null;
            if (xo.getChild(COVARIATE_MATRIX) != null) {
                cxo = (XMLObject) xo.getChild(COVARIATE_MATRIX);
                dMatrix = (MatrixParameter) cxo.getChild(MatrixParameter.class);
            }

            if ((dMatrix != null && beta == null) || (dMatrix == null && beta != null))
                throw new XMLParseException("Must specify both a set of regression coefficients and a design matrix.");

            if (dMatrix != null) {
                if (dMatrix.getRowDimension() != popParameter.getDimension())
                    throw new XMLParseException("Design matrix row dimension must equal the population parameter length.");
                if (dMatrix.getColumnDimension() != beta.getDimension())
                    throw new XMLParseException("Design matrix column dimension must equal the regression coefficient length.");
            }

            return new GMRFSkylineLikelihood(treeModel, popParameter, precParameter,
                    lambda, beta, dMatrix);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the likelihood of the tree given the population size vector.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(POPULATION_PARAMETER, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }),
                new ElementRule(PRECISION_PARAMETER, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }),
                new ElementRule(POPULATION_TREE, new XMLSyntaxRule[]{
                        new ElementRule(TreeModel.class)
                })
        };
    };


}