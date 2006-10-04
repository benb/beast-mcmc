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
 * A generic operator for use with a sum-constrained vector parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: DeltaExchangeOperator.java,v 1.18 2005/06/14 10:40:34 rambaut Exp $
 */
public class DeltaExchangeOperator extends SimpleMCMCOperator implements CoercableMCMCOperator {
	
	public static final String DELTA_EXCHANGE = "deltaExchange";
	public static final String DELTA = "delta";
	public static final String INTEGER_OPERATOR = "integer";
	
	public DeltaExchangeOperator(Parameter parameter, double delta, int weight, boolean isIntegerOperator, int mode) {
		this.parameter = parameter;
		this.delta = delta;
		this.weight = weight;
		this.mode = mode;
		this.isIntegerOperator = isIntegerOperator;
		
		if (isIntegerOperator && delta != Math.round(delta)) {
			throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
		}
	}
	
	/** @return the parameter this operator acts on. */
	public Parameter getParameter() { return parameter; }

	/** change the parameter and return the hastings ratio. 
	 * performs a delta exchange operation between two scalars in the vector
	 * and return the hastings ratio.
	 */
	public final double doOperation() throws OperatorFailedException {
		
		// get two dimensions
		int dim1 = MathUtils.nextInt(parameter.getDimension());
		int dim2 = MathUtils.nextInt(parameter.getDimension());
		while (dim1 == dim2) {
			dim2 = MathUtils.nextInt(parameter.getDimension());
		}

		double scalar1 = parameter.getParameterValue(dim1);
		double scalar2 = parameter.getParameterValue(dim2);
		
		// exchange a random delta
		double d = MathUtils.nextDouble() * delta;
		scalar1 -= d; 
		scalar2 += d;
		
		if (isIntegerOperator) {
			scalar1 = Math.round(scalar1);
			scalar2 = Math.round(scalar2);
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
	public final String getOperatorName() { return parameter.getParameterName(); }

	public double getCoercableParameter() {
		return Math.log(delta);
	}
	
	public void setCoercableParameter(double value) {
		delta = Math.exp(value);
	}
	
	public double getRawParameter() {
		return delta;
	}
	
	public int getMode() { 
		return mode; 
	}


	public double getTargetAcceptanceProbability() { return 0.234;}
	public double getMinimumAcceptanceLevel() { return 0.1;}
	public double getMaximumAcceptanceLevel() { return 0.4;}
	public double getMinimumGoodAcceptanceLevel() { return 0.20; }
	public double getMaximumGoodAcceptanceLevel() { return 0.30; }
	
	public int getWeight() { return weight; }

	public void setWeight(int w) { weight = w; }

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
	
	public String toString() { return getOperatorName() + "(windowsize=" + delta + ")"; }
	
	public static dr.xml.XMLObjectParser PARSER = new dr.xml.AbstractXMLObjectParser() {
		
		public String getParserName() { return DELTA_EXCHANGE; }
		
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
		
			int mode = CoercableMCMCOperator.DEFAULT;
			if (xo.hasAttribute(AUTO_OPTIMIZE)) {
				if (xo.getBooleanAttribute(AUTO_OPTIMIZE)) {
					mode = CoercableMCMCOperator.COERCION_ON;
				} else {
					mode = CoercableMCMCOperator.COERCION_OFF;
				}
			}
			
			boolean isIntegerOperator = false;
			if (xo.hasAttribute(INTEGER_OPERATOR)) {
				isIntegerOperator = xo.getBooleanAttribute(INTEGER_OPERATOR);
			}
			
			int weight = xo.getIntegerAttribute(WEIGHT);
			double delta = xo.getDoubleAttribute(DELTA);
			
			if (delta <= 0.0) {
				throw new XMLParseException("delta must be greater than 0.0");
			}
			
			Parameter parameter = (Parameter)xo.getChild(Parameter.class);	
			
			return new DeltaExchangeOperator(parameter, delta, weight, isIntegerOperator, mode);
		}
		
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "This element returns a scale operator on a given parameter.";
		}
		
		public Class getReturnType() { return MCMCOperator.class; }
		
		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			AttributeRule.newDoubleRule(DELTA),
			AttributeRule.newIntegerRule(WEIGHT),
			AttributeRule.newBooleanRule(AUTO_OPTIMIZE, true),
			AttributeRule.newBooleanRule(INTEGER_OPERATOR, true),
			new ElementRule(Parameter.class)
		};
	
	};
	// Private instance variables
	
	private Parameter parameter = null;
	private double delta = 0.02;
	private int weight = 1;
	private int mode = CoercableMCMCOperator.DEFAULT;
	private boolean isIntegerOperator = false;
}
