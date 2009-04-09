/*
 * DeltaExchangeOperator.java
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

package dr.inference.operators;

import dr.inference.model.Bounds;
import dr.inference.model.Parameter;
import dr.math.MathUtils;
import dr.xml.*;

/**
 * A generic operator for use with a sum-constrained (possibly weighted) vector parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: DeltaExchangeOperator.java,v 1.18 2005/06/14 10:40:34 rambaut Exp $
 */
public class DeltaExchangeOperator extends AbstractCoercableOperator {

    public static final String DELTA_EXCHANGE = "deltaExchange";
    public static final String DELTA = "delta";
    public static final String INTEGER_OPERATOR = "integer";
    public static final String PARAMETER_WEIGHTS = "parameterWeights";

    public DeltaExchangeOperator(Parameter parameter, int[] parameterWeights, double delta, double weight, boolean isIntegerOperator, CoercionMode mode) {

        super(mode);

        this.parameter = parameter;
        this.delta = delta;
        setWeight(weight);
        this.isIntegerOperator = isIntegerOperator;
        this.parameterWeights = parameterWeights;

        if (isIntegerOperator && delta != Math.round(delta)) {
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * change the parameter and return the hastings ratio.
     * performs a delta exchange operation between two scalars in the vector
     * and return the hastings ratio.
     */
    public final double doOperation() throws OperatorFailedException {

        // get two dimensions
        final int dim = parameter.getDimension();
        final int dim1 = MathUtils.nextInt(dim);
        int dim2 = dim1;
        while (dim1 == dim2) {
            dim2 = MathUtils.nextInt(dim);
        }

        double scalar1 = parameter.getParameterValue(dim1);
        double scalar2 = parameter.getParameterValue(dim2);

        if (isIntegerOperator) {
            int d = MathUtils.nextInt((int) Math.round(delta)) + 1;

            if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
            scalar1 = Math.round(scalar1 - d);
            scalar2 = Math.round(scalar2 + d);
        } else {

            // exchange a random delta
            final double d = MathUtils.nextDouble() * delta;
            scalar1 -= d;
            if (parameterWeights[dim1] != parameterWeights[dim2]) {
                scalar2 += d * (double) parameterWeights[dim1] / (double) parameterWeights[dim2];
            } else {
                scalar2 += d;
            }

        }
        Bounds bounds = parameter.getBounds();

        if (scalar1 < bounds.getLowerLimit(dim1) ||
                scalar1 > bounds.getUpperLimit(dim1) ||
                scalar2 < bounds.getLowerLimit(dim2) ||
                scalar2 > bounds.getUpperLimit(dim2)) {
            throw new OperatorFailedException("proposed values out of range!");
        }
        parameter.setParameterValue(dim1, scalar1);
        parameter.setParameterValue(dim2, scalar2);

        // symmetrical move so return a zero hasting ratio
        return 0.0;
    }

    // Interface MCMCOperator
    public final String getOperatorName() {
        return parameter.getParameterName();
    }

    public double getCoercableParameter() {
        return Math.log(delta);
    }

    public void setCoercableParameter(double value) {
        delta = Math.exp(value);
    }

    public double getRawParameter() {
        return delta;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
    }

    public final String getPerformanceSuggestion() {

        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
        double targetProb = getTargetAcceptanceProbability();

        double d = OperatorUtils.optimizeWindowSize(delta, parameter.getParameterValue(0) * 2.0, prob, targetProb);


        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try decreasing delta to about " + d;
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try increasing delta to about " + d;
        } else return "";
    }

    public String toString() {
        return getOperatorName() + "(windowsize=" + delta + ")";
    }

    public static dr.xml.XMLObjectParser PARSER = new dr.xml.AbstractXMLObjectParser() {

        public String getParserName() {
            return DELTA_EXCHANGE;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            CoercionMode mode = CoercionMode.parseMode(xo);

            final boolean isIntegerOperator = xo.getAttribute(INTEGER_OPERATOR, false);

            double weight = xo.getDoubleAttribute(WEIGHT);
            double delta = xo.getDoubleAttribute(DELTA);

            if (delta <= 0.0) {
                throw new XMLParseException("delta must be greater than 0.0");
            }

            Parameter parameter = (Parameter) xo.getChild(Parameter.class);


            int[] parameterWeights;
            if (xo.hasAttribute(PARAMETER_WEIGHTS)) {
                parameterWeights = xo.getIntegerArrayAttribute(PARAMETER_WEIGHTS);
                System.out.print("Parameter weights for delta exchange are: ");
                for (int parameterWeight : parameterWeights) {
                    System.out.print(parameterWeight + "\t");
                }
                System.out.println();

            } else {
                parameterWeights = new int[parameter.getDimension()];
                for (int i = 0; i < parameterWeights.length; i++) {
                    parameterWeights[i] = 1;
                }
            }

            if (parameterWeights.length != parameter.getDimension()) {
                throw new XMLParseException("parameter weights have the same length as parameter");
            }


            return new DeltaExchangeOperator(parameter, parameterWeights, delta, weight, isIntegerOperator, mode);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a scale operator on a given parameter.";
        }

        public Class getReturnType() {
            return MCMCOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(DELTA),
                AttributeRule.newIntegerArrayRule(PARAMETER_WEIGHTS, true),
                AttributeRule.newDoubleRule(WEIGHT),
                AttributeRule.newBooleanRule(AUTO_OPTIMIZE, true),
                AttributeRule.newBooleanRule(INTEGER_OPERATOR, true),
                new ElementRule(Parameter.class)
        };

    };
    // Private instance variables

    private Parameter parameter = null;
    private final int[] parameterWeights;
    private double delta = 0.02;
    private boolean isIntegerOperator = false;
}
