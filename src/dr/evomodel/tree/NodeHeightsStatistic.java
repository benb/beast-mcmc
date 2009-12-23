/*
 * RateStatistic.java
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

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.inference.model.Statistic;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.Arrays;

/**
 * A statistic that reports the heights of all the nodes in sorted order with the option
 * of grouping (for skyline-type plots).
 *
 * @author Andrew Rambaut
 * @version $Id:$
 */
public class NodeHeightsStatistic extends Statistic.Abstract implements TreeStatistic {

    public static final String NODE_HEIGHTS_STATISTIC = "nodeHeightsStatistic";

    public NodeHeightsStatistic(String name, Tree tree) {
        this(name, tree, null);
    }

    public NodeHeightsStatistic(String name, Tree tree, Parameter groupSizes) {
        super(name);
        this.tree = tree;
        this.groupSizes = groupSizes;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        if (groupSizes != null) {
            return groupSizes.getDimension();
        }
        return tree.getInternalNodeCount();
    }

    /**
     * @return the total length of all the branches in the tree
     */
    public double getStatisticValue(int dim) {
        if (dim == 0) {
            // This assumes that each dimension will be called in turn, so
            // the call for dim 0 updates the array.
            calculateHeights();
        }

        return heights[dim];
    }

    private void calculateHeights() {
        heights = new double[tree.getInternalNodeCount()];

        for (int i = 0; i < heights.length; i++) {
            heights[i] = tree.getNodeHeight(tree.getInternalNode(i));
        }
        Arrays.sort(heights);

        if (groupSizes != null) {
            double[] allHeights = heights;
            heights = new double[groupSizes.getDimension()];
            int k = 0;
            for (int i = 0; i < groupSizes.getDimension(); i++) {
                k += groupSizes.getValue(i);
                heights[i] = allHeights[k - 1];
            }
        }
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return NODE_HEIGHTS_STATISTIC;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String name = xo.getAttribute(NAME, xo.getId());
            Tree tree = (Tree) xo.getChild(Tree.class);

            Parameter groupSizes = (Parameter) xo.getChild(Parameter.class);

            return new NodeHeightsStatistic(name, tree, groupSizes);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that returns the heights of each internal node in increasing order (or groups them by a group size parameter)";
        }

        public Class getReturnType() {
            return RateStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newStringRule(NAME, true),
                new ElementRule(TreeModel.class),
                new ElementRule(Parameter.class, true),
        };
    };

    private Tree tree = null;
    private Parameter groupSizes = null;
    private double[] heights = null;
}