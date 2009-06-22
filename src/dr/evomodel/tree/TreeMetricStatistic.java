/*
 * TreeMetricStatistic.java
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

import dr.evolution.tree.BranchScoreMetric;
import dr.evolution.tree.CladeMetric;
import dr.evolution.tree.Tree;
import dr.inference.model.Statistic;
import dr.xml.*;
import jebl.evolution.treemetrics.BilleraMetric;
import jebl.evolution.treemetrics.CladeHeightMetric;
import jebl.evolution.treemetrics.RobinsonsFouldMetric;
import jebl.evolution.treemetrics.RootedTreeMetric;
import jebl.evolution.trees.SimpleRootedTree;

/**
 * A statistic that returns the distance between two trees.
 * <p/>
 * Currently supports the following metrics,
 * 1. compare - returns a 0 for identity of topology, 1 otherwise.
 * 2. Billera tree distance.
 * 3. ROBINSONS FOULD
 * 4. Clade height
 * 5. Branch Score
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Joseph Heled
 * @author Sebastian Hoehna
 * @version $Id: TreeMetricStatistic.java,v 1.14 2005/07/11 14:06:25 rambaut Exp $
 */
public class TreeMetricStatistic extends Statistic.Abstract implements TreeStatistic {

    public static final String TREE_METRIC_STATISTIC = "treeMetricStatistic";

    public static final String TARGET = "target";

    public static final String REFERENCE = "reference";

    public static final String METHOD = "method";

    enum Method {
        TOPOLOGY, BILLERA, ROBINSONSFOULD, CLADEHEIGHTM, BRANCHSCORE, CLADEMETRIC
    }

    public TreeMetricStatistic(String name, Tree target, Tree reference, Method method) {
        super(name);

        this.target = target;
        this.method = method;

        switch (method) {
            case TOPOLOGY: {
                this.referenceNewick = Tree.Utils.uniqueNewick(reference, reference.getRoot());
                break;
            }
            default: {
                jreference = Tree.Utils.asJeblTree(reference);
                break;
            }
        }

        switch (method) {
            case BILLERA:
                metric = new BilleraMetric();
                break;
            case ROBINSONSFOULD:
                metric = new RobinsonsFouldMetric();
                break;
            case CLADEHEIGHTM:
                metric = new CladeHeightMetric();
                break;
            case BRANCHSCORE:
                metric = new BranchScoreMetric();
                break;
            case CLADEMETRIC:
                metric = new CladeMetric();
                break;
        }
    }

    public void setTree(Tree tree) {
        this.target = tree;
    }

    public Tree getTree() {
        return target;
    }

    public int getDimension() {
        return 1;
    }

    /**
     * @return value.
     */
    public double getStatisticValue(int dim) {

        if (method == Method.TOPOLOGY) {
            return compareTreesByTopology();
        }

        return metric.getMetric(jreference, Tree.Utils.asJeblTree(target));
    }

    private double compareTreesByTopology() {
        final String tar = Tree.Utils.uniqueNewick(target, target.getRoot());
        return tar.equals(referenceNewick) ? 0.0 : 1.0;
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return TREE_METRIC_STATISTIC;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Method m = Method.TOPOLOGY;
            if (xo.hasAttribute(METHOD)) {
                final String s = xo.getStringAttribute(METHOD);
                m = Method.valueOf(s.toUpperCase());
            }

            final String name = xo.getAttribute(NAME, xo.hasId() ? xo.getId() : m.name());
            final Tree target = (Tree) xo.getElementFirstChild(TARGET);
            final Tree reference = (Tree) xo.getElementFirstChild(REFERENCE);

            return new TreeMetricStatistic(name, target, reference, m);
        }

        // ************************************************************************
        // AbstractXMLObjectParser
        // implementation
        // ************************************************************************

        public String getParserDescription() {
            return "A statistic that returns the distance between two trees. "
                    + " with method=\"topology\", return a 0 for identity and a 1 for difference. "
                    + "With other methods return the distance metric associated with that method.";
        }

        public Class getReturnType() {
            return TreeMetricStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new StringAttributeRule(
                        NAME,
                        "A name for this statistic primarily for the purposes of logging",
                        true),
                new StringAttributeRule(METHOD, "comparision method ("
                        + methodNames(",") + ")", true),
                new ElementRule(TARGET, new XMLSyntaxRule[]{new ElementRule(
                        Tree.class)}),
                new ElementRule(REFERENCE, new XMLSyntaxRule[]{new ElementRule(
                        Tree.class)}),};

    };

    private static String methodNames(String s) {
        String r = "";
        for (Method m : Method.values()) {
            if (r.length() > 0)
                r = r + s;
            r = r + m.name();
        }
        return r;
    }

    private final Method method;

    private Tree target = null;

    private String referenceNewick = null;

    private SimpleRootedTree jreference = null;

    RootedTreeMetric metric = null;
}
