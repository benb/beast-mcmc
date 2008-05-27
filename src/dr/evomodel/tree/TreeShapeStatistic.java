/*
 * TreeShapeStatistic.java
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

package dr.evomodel.tree;

import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeShape;
import dr.inference.model.Statistic;
import dr.xml.*;


/**
 * A statistic that returns the values of various tree shape statistics.
 *
 * @version $Id: TreeShapeStatistic.java,v 1.4 2005/07/11 14:06:25 rambaut Exp $
 *
 * @author Alexei Drummond
 *
 */
public class TreeShapeStatistic extends Statistic.Abstract implements TreeStatistic {

	public static final String TREE_SHAPE_STATISTIC = "treeShapeStatistics";
	public static final String TARGET = "target";
	
	public TreeShapeStatistic(String name, TreeModel target) {

		super(name);
		this.target = target;
		ultrametric = Tree.Utils.isUltrametric(target);
	}

	public void setTree(Tree tree) { this.target = tree; }
	public Tree getTree() { return target; }
	
	public int getDimension() { 
		if (ultrametric) return 5;
		return 4;	
	}
	
	public String getDimensionName(int dim) {
		switch (dim) {
			case 0: return "N-bar";
			case 1: return "N-bar-var";
			case 2: return "C";
			case 3: return "B1";
			case 4: return "gamma";
		}
		throw new IllegalArgumentException("Dimension doesn't exist!");
	}
		
	/** @return value. */
	public double getStatisticValue(int dim) {
		
		switch (dim) {
			case 0: return TreeShape.getNBarStatistic(target);
			case 1: return TreeShape.getVarNBarStatistic(target);
			case 2: return TreeShape.getCStatistic(target);
			case 3: return TreeShape.getB1Statistic(target);
			case 4: return TreeShape.getGammaStatistic(target);
		}
		throw new IllegalArgumentException("Dimension doesn't exist!");
	}
	
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
	
		public String getParserName() { return TREE_SHAPE_STATISTIC; }
	
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			
			String name;
			if (xo.hasAttribute(NAME)) {
				name = xo.getStringAttribute(NAME);
			} else {
				name = xo.getId();
			}
			TreeModel target = (TreeModel)xo.getElementFirstChild(TARGET);
			
			return new TreeShapeStatistic(name, target);
		}
		
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "A statistic that reports a handful of tree shape statistics on the given target tree.";
		}

		public Class getReturnType() { return TreeMetricStatistic.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new StringAttributeRule(NAME, "A name for this statistic primarily for the purposes of logging", true),
			new ElementRule(TARGET, 
				new XMLSyntaxRule[] { new ElementRule(TreeModel.class) })
		};

	};
	
	private Tree target = null;
	private boolean ultrametric = false;
}
