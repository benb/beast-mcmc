/*
 * SwapOperator.java
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

import dr.inference.model.Parameter;
import dr.math.MathUtils;
import dr.xml.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A generic operator swapping a number of pairs in a multi-dimensional parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: SwapOperator.java,v 1.10 2005/06/14 10:40:34 rambaut Exp $
 */
public class SwapOperator extends SimpleMCMCOperator {

    public final static String SWAP_OPERATOR = "swapOperator";
    private int size = 1;

	public SwapOperator(Parameter parameter, int size) {
		this.parameter = parameter;
        this.size = size;
        if (parameter.getDimension() < 2*size) {
            throw new IllegalArgumentException();
        }

        int dimension = parameter.getDimension();
        ArrayList list = new ArrayList();
        for (int i = 0; i < dimension; i++) {
            list.add(new Integer(i));
        }
        masterList = Collections.unmodifiableList(list);
	}
	
	/** @return the parameter this operator acts on. */
	public Parameter getParameter() { return parameter; }

	/**
	 * swap the values in two random parameter slots.
	 */
	public final double doOperation() {

        ArrayList allIndices = new ArrayList(masterList);
        int left, right;

        for (int i = 0; i < size; i++) {
            left = ((Integer)allIndices.remove(MathUtils.nextInt(allIndices.size()))).intValue();
            right = ((Integer)allIndices.remove(MathUtils.nextInt(allIndices.size()))).intValue();
            double value1 = parameter.getParameterValue(left);
            double value2 = parameter.getParameterValue(right);
            parameter.setParameterValue(left, value2);
            parameter.setParameterValue(right, value1);
        }

		return 0.0;
	}

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
		
		public String getParserName() { return SWAP_OPERATOR; }
		
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			
			Parameter parameter = (Parameter)xo.getChild(Parameter.class);
            int weight = xo.getIntegerAttribute("weight");
            int size = xo.getIntegerAttribute("size");

            boolean autoOptimize = xo.getBooleanAttribute("autoOptimize");
            if (autoOptimize) throw new XMLParseException("swapOperator can't be optimized!");

            System.out.println("Creating swap operator for parameter " + parameter.getParameterName() + " (weight=" + weight + ")");

            SwapOperator so = new SwapOperator(parameter, size);
            so.setWeight(weight);

            return so;
		}
		
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "This element represents an operator that swaps values in a multi-dimensional parameter.";
		}
		
		public Class getReturnType() { return SwapOperator.class; }
		
		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {

            AttributeRule.newIntegerRule("weight"),
            AttributeRule.newIntegerRule("size"),
            AttributeRule.newBooleanRule("autoOptimize"),
            new ElementRule(Parameter.class)
		};
	};
	
	public String getOperatorName() { return "swapOperator(" + parameter.getParameterName() + ")"; }
	
	public String getPerformanceSuggestion() {
		return "No suggestions";
	}
	
	//PRIVATE STUFF
	
	private Parameter parameter = null;
    private List masterList = null;
}
