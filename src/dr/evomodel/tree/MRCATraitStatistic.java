/*
 * TMRCAStatistic.java
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
import dr.evolution.util.Taxa;
import dr.evolution.util.TaxonList;
import dr.inference.model.Statistic;
import dr.xml.*;

import java.util.Set;

/**
 * A statistic that tracks the time of MRCA of a set of taxa
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: TMRCAStatistic.java,v 1.21 2005/07/11 14:06:25 rambaut Exp $
 */
public class MRCATraitStatistic extends Statistic.Abstract implements TreeStatistic {

    public static final String MRCA_TRAIT_STATISTIC = "mrcaTraitStatistic";
    public static final String MRCA = "mrca";
    public static final String NAME = "name";
    public static final String TRAIT = "trait";

    public MRCATraitStatistic(String name, String trait, TreeModel tree, TaxonList taxa) throws Tree.MissingTaxonException {
        super(name);
        this.tree = tree;
        this.trait = trait;
        this.leafSet = Tree.Utils.getLeavesForTaxa(tree, taxa);
        this.isRate = trait.equals("rate");
    }

    public void setTree(Tree tree) {
        this.tree = (TreeModel) tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        return 1;
    }

    /**
     * @return the height of the MRCA node.
     */
    public double getStatisticValue(int dim) {

        NodeRef node = Tree.Utils.getCommonAncestorNode(tree, leafSet);
        if (node == null) throw new RuntimeException("No node found that is MRCA of " + leafSet);
        if (isRate) {
            return tree.getNodeRate(node);
        }
        return (Double) tree.getNodeTrait(node, trait);
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return MRCA_TRAIT_STATISTIC;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String name = xo.getAttribute(NAME, xo.getId());
            String trait = xo.getStringAttribute(TRAIT);

            TreeModel tree = (TreeModel) xo.getChild(TreeModel.class);
            TaxonList taxa = (TaxonList) xo.getElementFirstChild(MRCA);

            try {
                return new MRCATraitStatistic(name, trait, tree, taxa);
            } catch (Tree.MissingTaxonException mte) {
                throw new XMLParseException("Taxon, " + mte + ", in " + getParserName() + "was not found in the tree.");
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that has as its value the height of the most recent common ancestor of a set of taxa in a given tree";
        }

        public Class getReturnType() {
            return TMRCAStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(TreeModel.class),
                new StringAttributeRule("name", "A name for this statistic primarily for the purposes of logging", true),
                new StringAttributeRule("trait", "The name of the trait (can be rate)"),
                AttributeRule.newBooleanRule("rate", true),
                new ElementRule(MRCA,
                        new XMLSyntaxRule[]{new ElementRule(Taxa.class)})
        };
    };

    private TreeModel tree = null;
    private Set<String> leafSet = null;
    private String trait;
    private boolean isRate;

}