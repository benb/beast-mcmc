/*
 * PrecisionMatrixGibbsOperator.java
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

package dr.evomodel.operators;

import dr.evolution.tree.NodeRef;
import dr.evomodel.continuous.AbstractMultivariateTraitLikelihood;
import dr.evomodel.continuous.SampledMultivariateTraitLikelihood;
import dr.evomodel.continuous.FullyConjugateMultivariateTraitLikelihood;
import dr.evomodel.tree.TreeModel;
import dr.inference.distribution.MultivariateDistributionLikelihood;
import dr.inference.model.MatrixParameter;
import dr.inference.model.Parameter;
import dr.inference.operators.GibbsOperator;
import dr.inference.operators.MCMCOperator;
import dr.inference.operators.OperatorFailedException;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.distributions.WishartDistribution;
import dr.math.distributions.WishartSufficientStatistics;
import dr.math.matrixAlgebra.IllegalDimension;
import dr.math.matrixAlgebra.SymmetricMatrix;
//import dr.math.matrixAlgebra.Matrix;
import dr.xml.*;

/**
 * @author Marc Suchard
 */
public class PrecisionMatrixGibbsOperator extends SimpleMCMCOperator implements GibbsOperator {

    public static final String VARIANCE_OPERATOR = "precisionGibbsOperator";
    public static final String PRECISION_MATRIX = "precisionMatrix";
    public static final String TREE_MODEL = "treeModel";
    public static final String OUTCOME = "outcome";
    public static final String MEAN = "mean";
    public static final String PRIOR = "prior";
    public static final String TRAIT_MODEL = "traitModel";

    private final AbstractMultivariateTraitLikelihood traitModel;
    private final MatrixParameter precisionParam;
    //    private WishartDistribution priorDistribution;
    private final double priorDf;
    private SymmetricMatrix priorInverseScaleMatrix;
    private final TreeModel treeModel;
    private final int dim;
    private double numberObservations;
    private final String traitName;
    private final boolean isSampledTraitLikelihood;

    public PrecisionMatrixGibbsOperator(
            AbstractMultivariateTraitLikelihood traitModel,
            WishartDistribution priorDistribution,
            double weight) {
        super();
        this.traitModel = traitModel;
        this.precisionParam = (MatrixParameter) traitModel.getDiffusionModel().getPrecisionParameter();
//        this.priorDistribution = priorDistribution;
        this.priorDf = priorDistribution.df();
        this.priorInverseScaleMatrix = null;
        if (priorDistribution.scaleMatrix() != null)
            this.priorInverseScaleMatrix =
                    (SymmetricMatrix) (new SymmetricMatrix(priorDistribution.scaleMatrix())).inverse();
        setWeight(weight);
        this.treeModel = traitModel.getTreeModel();
        traitName = traitModel.getTraitName();
        dim = precisionParam.getRowDimension(); // assumed to be square

        isSampledTraitLikelihood = (traitModel instanceof SampledMultivariateTraitLikelihood);

        if (!isSampledTraitLikelihood && !(traitModel instanceof FullyConjugateMultivariateTraitLikelihood)) {
            throw new RuntimeException("Only implemented for a SampledMultivariateTraitLikelihood and " +
                    "FullyConjugateMultivariateTraitLikelihood");
        }
    }

    public int getStepCount() {
        return 1;
    }

//    private void incrementScaledSquareMatrix(double[][] out, double[][] in, double scalar, int dim) {
//        for (int i = 0; i < dim; i++) {
//            for (int j = 0; j < dim; j++) {
//                out[i][j] += scalar * in[i][j];
//            }
//        }
//    }

//    private void zeroSquareMatrix(double[][] out, int dim) {
//        for (int i = 0; i < dim; i++) {
//            for (int j = 0; j < dim; j++) {
//                out[i][j] = 0.0;
//            }
//        }
//    }

    private void incrementOuterProduct(double[][] S,
                                       FullyConjugateMultivariateTraitLikelihood integratedLikelihood) {


        final WishartSufficientStatistics sufficientStatistics = integratedLikelihood.getWishartStatistics();
        final double[][] outerProducts = sufficientStatistics.getScaleMatrix();

        final double df = sufficientStatistics.getDf();
//        final double df = 2;

//        final double df = integratedLikelihood.getTotalTreePrecision();

//        System.err.println("OuterProducts = \n" + new Matrix(outerProducts));
//        System.err.println("Total tree DF  = " + df);
//        System.exit(-1);

        for (int i = 0; i < outerProducts.length; i++) {
            System.arraycopy(outerProducts[i], 0, S[i], 0, S[i].length);
        }
        numberObservations = df;
    }

    private void incrementOuterProduct(double[][] S, NodeRef node) {

        if (!treeModel.isRoot(node)) {

            NodeRef parent = treeModel.getParent(node);
            double[] parentTrait = treeModel.getMultivariateNodeTrait(parent, traitName);
            double[] childTrait = treeModel.getMultivariateNodeTrait(node, traitName);
            double time = traitModel.getRescaledBranchLength(node);

            if (time > 0) {

                double sqrtTime = Math.sqrt(time);

                double[] delta = new double[dim];

                for (int i = 0; i < dim; i++)
                    delta[i] = (childTrait[i] - parentTrait[i]) / sqrtTime;

                for (int i = 0; i < dim; i++) {            // symmetric matrix,
                    for (int j = i; j < dim; j++)
                        S[j][i] = S[i][j] += delta[i] * delta[j];
                }
                numberObservations += 1; // This assumes a *single* observation per tip
            }
        }
        // recurse down tree
        for (int i = 0; i < treeModel.getChildCount(node); i++)
            incrementOuterProduct(S, treeModel.getChild(node, i));
    }

    public double[][] getOperationScaleMatrixAndSetObservationCount() {

        // calculate sum-of-the-weighted-squares matrix over tree
        double[][] S = new double[dim][dim];
        SymmetricMatrix S2;
        SymmetricMatrix inverseS2 = null;
        numberObservations = 0; // Need to reset, as incrementOuterProduct can be recursive

        if (isSampledTraitLikelihood) {
            incrementOuterProduct(S, treeModel.getRoot());
        } else { // IntegratedTraitLikelihood
            incrementOuterProduct(S, (FullyConjugateMultivariateTraitLikelihood) traitModel);
        }

        try {
            S2 = new SymmetricMatrix(S);
            if (priorInverseScaleMatrix != null)
                S2 = priorInverseScaleMatrix.add(S2);
            inverseS2 = (SymmetricMatrix) S2.inverse();

        } catch (IllegalDimension illegalDimension) {
            illegalDimension.printStackTrace();
        }

        assert inverseS2 != null;

        return inverseS2.toComponents();
    }

    public double doOperation() throws OperatorFailedException {

        final double[][] scaleMatrix = getOperationScaleMatrixAndSetObservationCount();
        final double treeDf = numberObservations;
        final double df = priorDf + treeDf;

        double[][] draw = WishartDistribution.nextWishart(df, scaleMatrix);

        for (int i = 0; i < dim; i++) {
            Parameter column = precisionParam.getParameter(i);
            for (int j = 0; j < dim; j++)
                column.setParameterValueQuietly(j, draw[j][i]);
        }
        precisionParam.fireParameterChangedEvent();

        return 0;
    }

    public String getPerformanceSuggestion() {
        return null;
    }

    public String getOperatorName() {
        return VARIANCE_OPERATOR;
    }

    public static dr.xml.XMLObjectParser PARSER = new dr.xml.AbstractXMLObjectParser() {

        public String getParserName() {
            return VARIANCE_OPERATOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double weight = xo.getDoubleAttribute(WEIGHT);
            AbstractMultivariateTraitLikelihood traitModel = (AbstractMultivariateTraitLikelihood) xo.getChild(AbstractMultivariateTraitLikelihood.class);

            MatrixParameter precMatrix = (MatrixParameter) traitModel.getDiffusionModel().getPrecisionParameter();

            MultivariateDistributionLikelihood prior = (MultivariateDistributionLikelihood) xo.getChild(MultivariateDistributionLikelihood.class);
            if (!(prior.getDistribution() instanceof WishartDistribution))
                throw new RuntimeException("Only a Wishart distribution is conjugate for Gibbs sampling");

            // Make sure precMatrix is square and dim(precMatrix) = dim(parameter)

            if (precMatrix.getColumnDimension() != precMatrix.getRowDimension())
                throw new XMLParseException("The variance matrix is not square");

            return new PrecisionMatrixGibbsOperator(
                    traitModel, (WishartDistribution) prior.getDistribution(), weight
            );
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a multivariate normal random walk operator on a given parameter.";
        }

        public Class getReturnType() {
            return MCMCOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(WEIGHT),
                new ElementRule(AbstractMultivariateTraitLikelihood.class),
                new ElementRule(MultivariateDistributionLikelihood.class),
        };
    };
}
