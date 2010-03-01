/*
 * UniformOperator.java
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
import dr.inferencexml.operators.UniformOperatorParser;
import dr.math.MathUtils;

/**
 * A generic uniform sampler/operator for use with a multi-dimensional parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: UniformOperator.java,v 1.16 2005/06/14 10:40:34 rambaut Exp $
 */
public class UniformOperator extends SimpleMCMCOperator {

    public UniformOperator(Parameter parameter, double weight) {
        this.parameter = parameter;
        setWeight(weight);
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * change the parameter and return the hastings ratio.
     */
    public final double doOperation() {

        final int index = MathUtils.nextInt(parameter.getDimension());
        final Bounds<Double> bounds = parameter.getBounds();
        final double lower = bounds.getLowerLimit(index);
        final double upper = bounds.getUpperLimit(index);
        final double newValue = (MathUtils.nextDouble() * (upper - lower)) + lower;

        parameter.setParameterValue(index, newValue);

//		System.out.println(newValue + "[" + lower + "," + upper + "]");
        return 0.0;
    }

    //MCMCOperator INTERFACE
    public final String getOperatorName() {
        return "uniform(" + parameter.getParameterName() + ")";
    }

    public final void optimize(double targetProb) {

        throw new RuntimeException("This operator cannot be optimized!");
    }

    public boolean isOptimizing() {
        return false;
    }

    public void setOptimizing(boolean opt) {
        throw new RuntimeException("This operator cannot be optimized!");
    }

    public String getPerformanceSuggestion() {
        return "";
//        final double acceptance = Utils.getAcceptanceProbability(this);
//        if ( acceptance < getMinimumAcceptanceLevel()) {
//            return "";
//        } else if ( acceptance > getMaximumAcceptanceLevel() ) {
//            return "";
//        } else {
//            return "";
//        }
    }

    public String toString() {
        return UniformOperatorParser.UNIFORM_OPERATOR + "(" + parameter.getParameterName() + ")";
    }

    //PRIVATE STUFF

    private Parameter parameter = null;
}
