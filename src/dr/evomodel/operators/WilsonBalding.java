/*
 * WilsonBalding.java
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

package dr.evomodel.operators;

import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evomodel.coalescent.ConstantPopulationModel;
import dr.evomodel.tree.TreeModel;
import dr.inference.operators.MCMCOperator;
import dr.inference.operators.OperatorFailedException;
import dr.math.MathUtils;
import dr.xml.*;

/**
 * Implements the unweighted wilson-balding brach swapping move.
 * WARNING: Because of the way it handles root moves this
 * move only works for constant populations!!!
 *
 * @author Alexei Drummond
 * @version $Id: WilsonBalding.java,v 1.38 2005/06/14 10:40:34 rambaut Exp $
 */
public class WilsonBalding extends AbstractTreeOperator {

    public static final String WILSON_BALDING = "wilsonBalding";
    public static final String DEMOGRAPHIC_MODEL = "demographicModel";

    private double logq;
    private TreeModel tree = null;
    private ConstantPopulationModel demoModel;
    private int tipCount;


    public WilsonBalding(TreeModel tree, ConstantPopulationModel demoModel, double weight) {
        this.tree = tree;
        this.demoModel = demoModel;
        tipCount = tree.getExternalNodeCount();
        setWeight(weight);
    }

    public double doOperation() throws OperatorFailedException {

        proposeTree();
        if (tree.getExternalNodeCount() != tipCount) {
            int newCount = tree.getExternalNodeCount();
            throw new RuntimeException("Lost some tips in modified SPR! (" +
                    tipCount + "-> " + newCount + ")");
        }
        //System.out.println("last accepted deviation: " + getDeviation());
        //System.out.println("logq=" + logq);
        return logq;
    }

    /**
     * WARNING: Assumes strictly bifurcating tree.
     */
    public void proposeTree() throws OperatorFailedException {

        NodeRef i;
        double delta, oldMinAge, newMinAge, newRange, oldRange, newAge, q;

        //Bchoose

        //for (int n =0; n < tree.getNodeCount(); n++) {
        //	System.out.println(n + " " + ( (tree.getNode(n) == null) ? "null" : tree.getNode(n).getId()));
        //}

        // choose a random node avoiding root
        final int nodeCount = tree.getNodeCount();
        do {
            i = tree.getNode(MathUtils.nextInt(nodeCount));
        } while (tree.getRoot() == i);
        final NodeRef iP = tree.getParent(i);

        // choose another random node to insert i above
        NodeRef j = tree.getNode(MathUtils.nextInt(nodeCount));
        NodeRef k = tree.getParent(j);

        // make sure that the target branch <k, j> is above the subtree being moved
        while ((k != null && tree.getNodeHeight(k) <= tree.getNodeHeight(i)) || (i == j)) {
            j = tree.getNode(MathUtils.nextInt(nodeCount));
            k = tree.getParent(j);
        }

        // disallow moves that change the root.
        if (j == tree.getRoot() || iP == tree.getRoot()) {
            throw new OperatorFailedException("Root changes not allowed!");
        }

        if (k == iP || j == iP || k == i) throw new OperatorFailedException("move failed");

        final NodeRef CiP = getOtherChild(tree, iP, i);
        NodeRef PiP = tree.getParent(iP);

//		ConstantPopulation demoFunc = null;
//		if (demoModel != null && demoModel.getDemographicFunction() instanceof ConstantPopulation) {
//			demoFunc = (ConstantPopulation)demoModel.getDemographicFunction();
//		}

//        if (j == tree.getRoot()) {
//			if (demoModel != null) {
//				delta = -demoFunc.getN0() * Math.log(MathUtils.nextDouble());
//			} else {
//				delta = tree.getNodeHeight(j) * MathUtils.nextDouble();
//			}
//			newAge = tree.getNodeHeight(j) + delta;
//
//			PiP = tree.getParent(iP);
//			oldMinAge = Math.max(tree.getNodeHeight(i), tree.getNodeHeight(CiP));
//			oldRange = tree.getNodeHeight(PiP) - oldMinAge;
//
//			if (demoFunc == null) {
//				q = tree.getNodeHeight(j) / oldRange;
//			} else {
//				q = Math.exp(delta/demoFunc.getN0())*demoFunc.getN0()/oldRange;
//			}
//		} else if (iP == tree.getRoot()) {
//
//			newMinAge = Math.max(tree.getNodeHeight(i), tree.getNodeHeight(j));
//			newRange = tree.getNodeHeight(k) - newMinAge;
//			newAge = newMinAge + (MathUtils.nextDouble()*newRange);
//
//			if (demoFunc == null) {
//				if (tree.getNodeHeight(iP) > (tree.getNodeHeight(CiP) * 2)) throw new OperatorFailedException("too big");
//				q = newRange / tree.getNodeHeight(CiP);
//			} else {
//				q = (tree.getNodeHeight(CiP)-tree.getNodeHeight(iP))/demoFunc.getN0() + Math.log(newRange/demoFunc.getN0());
//			}
//		} else {
        newMinAge = Math.max(tree.getNodeHeight(i), tree.getNodeHeight(j));
        newRange = tree.getNodeHeight(k) - newMinAge;
        newAge = newMinAge + (MathUtils.nextDouble() * newRange);
        oldMinAge = Math.max(tree.getNodeHeight(i), tree.getNodeHeight(CiP));
        oldRange = tree.getNodeHeight(PiP) - oldMinAge;
        q = newRange / Math.abs(oldRange);
        //System.out.println(newRange + "/" + oldRange + "=" + q);
//		}

        //Bupdate

        tree.beginTreeEdit();

        if (j == tree.getRoot()) {

            // 1. remove edges <iP, CiP>
            tree.removeChild(iP, CiP);
            tree.removeChild(PiP, iP);

            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>
            tree.addChild(iP, j);
            tree.addChild(PiP, CiP);

            // iP is the new root
            tree.setRoot(iP);

        } else if (iP == tree.getRoot()) {

            // 1. remove edges <k, j>, <iP, CiP>, <PiP, iP>
            tree.removeChild(k, j);
            tree.removeChild(iP, CiP);

            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>
            tree.addChild(iP, j);
            tree.addChild(k, iP);

            //CiP is the new root
            tree.setRoot(CiP);

        } else {
            // 1. remove edges <k, j>, <iP, CiP>, <PiP, iP>
            tree.removeChild(k, j);
            tree.removeChild(iP, CiP);
            tree.removeChild(PiP, iP);

            // 2. add edges <k, iP>, <iP, j>, <PiP, CiP>
            tree.addChild(iP, j);
            tree.addChild(k, iP);
            tree.addChild(PiP, CiP);
        }

        tree.setNodeHeight(iP, newAge);

        try {
            tree.endTreeEdit();
        } catch (MutableTree.InvalidTreeException ite) {
            throw new OperatorFailedException(ite.toString());
        }

        logq = Math.log(q);
    }

    public double getMinimumAcceptanceLevel() {
        return 0.01;
    }

    public String getPerformanceSuggestion() {
        if (MCMCOperator.Utils.getAcceptanceProbability(this) < getMinimumAcceptanceLevel()) {
            return "";
        } else if (MCMCOperator.Utils.getAcceptanceProbability(this) > getMaximumAcceptanceLevel()) {
            return "";
        } else {
            return "";
        }
    }

    public String getOperatorName() {
        return WILSON_BALDING + "(" + tree.getId() + ")";
    }

    public static dr.xml.XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return WILSON_BALDING;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            ConstantPopulationModel demoModel = null;
            if (xo.hasAttribute(DEMOGRAPHIC_MODEL)) {
                demoModel = (ConstantPopulationModel) xo.getElementFirstChild(DEMOGRAPHIC_MODEL);
            }

            double weight = xo.getDoubleAttribute(WEIGHT);

            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);

            return new WilsonBalding(treeModel, demoModel, weight);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(WEIGHT),
                new ElementRule(DEMOGRAPHIC_MODEL,
                        new XMLSyntaxRule[]{new ElementRule(ConstantPopulationModel.class)}, true),
                new ElementRule(TreeModel.class)
        };

        public String getParserDescription() {
            return "An operator which performs the Wilson-Balding move on a tree";
        }

        public Class getReturnType() {
            return WilsonBalding.class;
        }
    };
}
