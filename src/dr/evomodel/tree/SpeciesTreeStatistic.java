/*
 * SpeciesTreeStatistic.java
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
import dr.evolution.util.Taxon;
import dr.inference.model.BooleanStatistic;
import dr.inference.model.Statistic;
import dr.xml.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Interface for a phylogenetic tree that may contain population level data
 *
 * @version $Id: SpeciesTreeStatistic.java,v 1.14 2005/07/11 14:06:25 rambaut Exp $
 *
 * @author Alexei Drummond
 */
public class SpeciesTreeStatistic extends BooleanStatistic implements TreeStatistic {

	public static final String SPECIES_TREE_STATISTIC = "speciesTreeStatistic";

	public SpeciesTreeStatistic(String name, Tree speciesTree, Tree populationTree) {
		
		super(name);
		this.speciesTree = speciesTree;
		this.popTree = populationTree;
	}
	
	public void setTree(Tree tree) { this.popTree = tree; }
	public Tree getTree() { return popTree; }
	
	public int getDimension() { return 1; }
	    
	/**
	 * @return true if the population tree is compatible with the species tree
	 */
    public boolean getBoolean(int dim) {
	
		if (popTree.getNodeHeight(popTree.getRoot()) < speciesTree.getNodeHeight(speciesTree.getRoot())) {
			return false;	
		}
		
		return isCompatible(popTree.getRoot(), null); 
	}
	
	
	private boolean isCompatible(NodeRef popNode, Set<String> species) {
		
		//int n = popNode.getNumber() - popTree.getExternalNodeCount();
		
		if (popTree.isExternal(popNode)) {
			Taxon speciesTaxon = (Taxon)popTree.getTaxonAttribute(popNode.getNumber(), "species");
			species.add(speciesTaxon.getId());
		} else {
		
			Set<String> speciesTaxa = new HashSet<String>();
			
			int childCount = popTree.getChildCount(popNode);
			for (int i = 0; i < childCount; i++) {
				if (!isCompatible(popTree.getChild(popNode, i), speciesTaxa)) {
					return false;
				}
			}
			
			if (species != null) {
				species.addAll(speciesTaxa);
				
				NodeRef speciesNode = Tree.Utils.getCommonAncestorNode(speciesTree, speciesTaxa);
				if (popTree.getNodeHeight(popNode) < speciesTree.getNodeHeight(speciesNode)) { 
					return false; 
				}
			}
		}
		
		return true;
	}
	
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
	
		public String getParserName() { return SPECIES_TREE_STATISTIC; }
	
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
			
			String name;
			if (xo.hasAttribute(NAME)) {
				name = xo.getStringAttribute(NAME);
			} else {
				name = xo.getId();
			}
			Tree speciesTree = (Tree)xo.getElementFirstChild("speciesTree");
			Tree popTree = (Tree)xo.getElementFirstChild("populationTree");
			return new SpeciesTreeStatistic(name, speciesTree, popTree);
		}
		
		public String getParserDescription() {
			return "A statistic that returns true if the given population tree is compatible with the species tree. " + 
				"Compatibility is defined as the compatibility of the timings of the events, so that incompatibility arises " + 
				"if two individuals in the population tree coalescent before their species do in the species tree.";
		}

		public Class getReturnType() { return Statistic.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
	
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new StringAttributeRule("name", "A name for this statistic primarily for the purposes of logging", true),
			new ElementRule("speciesTree", 
				new XMLSyntaxRule[] { new ElementRule(Tree.class)}),
			new ElementRule("populationTree", 
				new XMLSyntaxRule[] { new ElementRule(Tree.class)})
		};
	};
	
	private Tree speciesTree;
	private Tree popTree;
}

