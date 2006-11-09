/*
 * AdvancedTreeLikelihood.java
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

package dr.evomodel.treelikelihood; 

import dr.evolution.alignment.PatternList;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxa;
import dr.evolution.util.TaxonList;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.DefaultBranchRateModel;
import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.substmodel.FrequencyModel;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.Likelihood;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

/**
 * AdvancedTreeLikelihood - implements a Likelihood Function for sequences on a tree.
 * This one has some advanced models such as multiple site models for different clades.
 * This only makes sense if those clades are being constrained to remain monophyletic.
 *
 * @version $Id: AdvancedTreeLikelihood.java,v 1.11 2006/01/10 16:48:27 rambaut Exp $
 *
 * @author Andrew Rambaut
 */

public class AdvancedTreeLikelihood extends AbstractTreeLikelihood {

	public static final String ADVANCED_TREE_LIKELIHOOD = "advancedTreeLikelihood";
	public static final String CLADE = "clade";
	public static final String INCLUDE_STEM = "includeStem";
	public static final String TIPS = "tips";
    public static final String DELTA = "delta";

    /**
     * Constructor.
     */   	
    public AdvancedTreeLikelihood(	PatternList patternList,
    								TreeModel treeModel,
    								SiteModel siteModel,
                            	    BranchRateModel branchRateModel,
   									boolean useAmbiguities,
                                       boolean useScaling)
	{
    	
		super(ADVANCED_TREE_LIKELIHOOD, patternList, treeModel);

		try {
			this.siteModel = siteModel;
			addModel(siteModel);
			
			this.frequencyModel = siteModel.getFrequencyModel();
			addModel(frequencyModel);
	    	
	    	if (!siteModel.integrateAcrossCategories()) {
	    		throw new RuntimeException("AdvancedTreeLikelihood can only use SiteModels that require integration across categories");
	    	}
	    	
	 		this.categoryCount = siteModel.getCategoryCount();
	   		   		
			if (patternList.getDataType() instanceof dr.evolution.datatype.Nucleotides) {

				if (NativeNucleotideLikelihoodCore.isAvailable()) {
				
					Logger.getLogger("dr.evomodel").info("AdvancedTreeLikelihood using native nucleotide likelihood core.");
					likelihoodCore = new NativeNucleotideLikelihoodCore();
				} else {
				
					Logger.getLogger("dr.evomodel").info("AdvancedTreeLikelihood Java nucleotide likelihood core.");
					likelihoodCore = new NucleotideLikelihoodCore();
				}
				
			} else if (patternList.getDataType() instanceof dr.evolution.datatype.AminoAcids) {
				Logger.getLogger("dr.evomodel").info("AdvancedTreeLikelihood Java amino acid likelihood core.");
				likelihoodCore = new AminoAcidLikelihoodCore();
			} else {
				Logger.getLogger("dr.evomodel").info("AdvancedTreeLikelihood Java general likelihood core.");
				likelihoodCore = new GeneralLikelihoodCore(patternList.getStateCount());
			}
						
            if (branchRateModel != null) {
                this.branchRateModel = branchRateModel;
                Logger.getLogger("dr.evomodel").info("Branch rate model used: " + branchRateModel.getModelName());
            } else {
                this.branchRateModel = new DefaultBranchRateModel();
            }
            addModel(this.branchRateModel);

			probabilities = new double[stateCount * stateCount];

			likelihoodCore.initialize(nodeCount, patternCount, categoryCount, true, useScaling);
			
			int extNodeCount = treeModel.getExternalNodeCount();
			int intNodeCount = treeModel.getInternalNodeCount();

			for (int i = 0; i < extNodeCount; i++) {
				// Find the id of tip i in the patternList
				String id = treeModel.getTaxonId(i);
				int index = patternList.getTaxonIndex(id);
				
				if (index == -1) {
					throw new TaxonList.MissingTaxonException("Taxon, " + id + ", in tree, " + treeModel.getId() + 
																", is not found in patternList, " + patternList.getId());
				}
				
				if (useAmbiguities) {
					setPartials(likelihoodCore, patternList, categoryCount, index, i);
				} else {
					setStates(likelihoodCore, patternList, index, i);
				}
			}
			
			for (int i = 0; i < intNodeCount; i++) {
				likelihoodCore.createNodePartials(extNodeCount + i);
			}
		} catch (TaxonList.MissingTaxonException mte) {
			throw new RuntimeException(mte.toString());
		}

   	}

    /**
     * Add an additional siteModel for a clade in the tree.
     */   	
    public void addCladeSiteModel(	SiteModel siteModel,
   									TaxonList taxonList,
   									boolean includeStem ) throws Tree.MissingTaxonException 
	{
        Logger.getLogger("dr.evomodel").info("SiteModel added for clade.");
    	cladeSiteModels.add(new Clade(siteModel, taxonList, includeStem));
		addModel(siteModel);
		commonAncestorsKnown = true;
	}
	
    /**
     * Add an additional siteModel for the tips of the tree.
     */   	
    public void addTipsSiteModel(SiteModel siteModel) 
	{
        Logger.getLogger("dr.evomodel").info("SiteModel added for tips.");
    	tipsSiteModel = siteModel;
		addModel(siteModel);
	}
	
    private void addDeltaParameter(Parameter deltaParameter) {
        Logger.getLogger("dr.evomodel").info("Delta parameter added for tips.");
        this.deltaParameter = deltaParameter;
        addParameter(deltaParameter);
    }

    // **************************************************************
    // ParameterListener IMPLEMENTATION
    // **************************************************************

    protected void handleParameterChangedEvent(Parameter parameter, int index) {
        // deltaParameter has changed...
        updateAllNodes();
        super.handleParameterChangedEvent(parameter, index);
    }

	// **************************************************************
    // ModelListener IMPLEMENTATION
    // **************************************************************
	
	/**
	 * Handles model changed events from the submodels. 
	 */
	protected void handleModelChangedEvent(Model model, Object object, int index) {

		if (model == treeModel) {
			if (object instanceof TreeModel.TreeChangedEvent) {

                if (((TreeModel.TreeChangedEvent)object).isNodeChanged()) {

				    updateNodeAndChildren(((TreeModel.TreeChangedEvent)object).getNode());

                } else {
                    updateAllNodes();

                    commonAncestorsKnown = false;

                }
			}
				
        } else if (model == branchRateModel) {
            updateAllNodes();

		} else if (model == frequencyModel) {
			
			updateAllNodes();
				
		} else if (model instanceof SiteModel) {
			
			if (model == siteModel) {
				
				updateAllNodes();
					
			} else if (model == tipsSiteModel) {
			
				updateAllNodes();
				
			} else {
			
				// find the siteModel in the additional siteModel list
				NodeRef node = null;
				for (int i = 0, n = cladeSiteModels.size(); i < n; i++) {
					Clade clade = (Clade)cladeSiteModels.get(i);

					if (!commonAncestorsKnown) {
						clade.findMRCA();
					}
					
					if (clade.getSiteModel() == model) {
						node = treeModel.getNode(clade.getNode());
					}
				}
				commonAncestorsKnown = true;

				updateNodeAndDescendents(node);
			}
						
		} else {
		
			throw new RuntimeException("Unknown componentChangedEvent");
		}
		
		super.handleModelChangedEvent(model, object, index);
	}
	
	// **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************
	
	/**
	 * Stores the additional state other than model components
	 */
	protected void storeState() {
		
		likelihoodCore.storeState();
		super.storeState();
		
	}
	
	/**
	 * Restore the additional stored state
	 */
	protected void restoreState() {
	
		likelihoodCore.restoreState();
		super.restoreState();
			
	}
	
    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

	/**
     * Calculate the log likelihood of the current state.
     * @return the log likelihood.
     */
    protected double calculateLogLikelihood() {
	 
		NodeRef root = treeModel.getRoot();
					
		if (rootPartials == null) {
			rootPartials = new double[patternCount * stateCount];
		}
		
		if (patternLogLikelihoods == null) {
			patternLogLikelihoods = new double[patternCount];
		}
		
		if (!commonAncestorsKnown) {
			for (int i = 0, n = cladeSiteModels.size(); i < n; i++) {
				((Clade)cladeSiteModels.get(i)).findMRCA();
			}
			commonAncestorsKnown = true;
		}

		traverse(treeModel, root, siteModel);
		
		//********************************************************************
		// after traverse all nodes and patterns have been updated -- 
		//so change flags to reflect this.
		for (int i = 0; i < nodeCount; i++) {
			updateNode[i] = false;
		}
		//********************************************************************
		
		double logL = 0.0;
		
		for (int i = 0; i < patternCount; i++) {
			logL += patternLogLikelihoods[i] * patternWeights[i];
		}
		
        if (Double.isNaN(logL)) {
            throw new RuntimeException("Likelihood NaN");
        }

		return logL;
    }

	/**
     * Traverse the tree calculating partial likelihoods.
     * @return whether the partials for this node were recalculated.
     */
	private final boolean traverse(Tree tree, NodeRef node, SiteModel currentSiteModel) {
	
		boolean update = false;
		
		int nodeNum = node.getNumber();
		
		SiteModel nextSiteModel = currentSiteModel;
		
		if (tipsSiteModel != null && tree.isExternal(node)) {
			currentSiteModel = tipsSiteModel;
		} else {
			for (int i = 0, n = cladeSiteModels.size(); i < n; i++) {
				Clade clade = (Clade)cladeSiteModels.get(i);
				if (clade.getNode() == nodeNum) {
					nextSiteModel = clade.getSiteModel();
					
					if (clade.includeStem()) {
						currentSiteModel = nextSiteModel;
					}
					break;
				}
			}
		}
		
		NodeRef parent = tree.getParent(node);
		
		// First update the transition probability matrix(ices) for this branch
		if (parent != null && updateNode[nodeNum]) {

	   		double branchRate = branchRateModel.getBranchRate(tree, node);

			// Get the operational time of the branch
	   		double branchTime = branchRate * ( tree.getNodeHeight(parent) - tree.getNodeHeight(node) );
	   		if (branchTime < 0.0) {
	   			throw new RuntimeException("Negative branch length: " + branchTime);
	   		}

            if (tree.isExternal(node) && deltaParameter != null) {
                branchTime += deltaParameter.getParameterValue(0);
            }

	   		for (int i = 0; i < categoryCount; i++) {
				
				currentSiteModel.getTransitionProbabilitiesForCategory(i, branchTime, probabilities);
				likelihoodCore.setNodeMatrix(nodeNum, i, probabilities);
			}
						
			update = true;
		}

		// If the node is internal, update the partial likelihoods.
		if (!tree.isExternal(node)) {
			
			int nodeCount = tree.getChildCount(node);
			if (nodeCount != 2)
				throw new RuntimeException("binary trees only!");
				
			// Traverse down the two child nodes
			NodeRef child1 = tree.getChild(node, 0);
			boolean update1 = traverse(tree, child1, nextSiteModel);
			
			NodeRef child2 = tree.getChild(node, 1);
			boolean update2 = traverse(tree, child2, nextSiteModel);
			
			// If either child node was updated then update this node too
			if (update1 || update2) {
				
				int childNum1 = child1.getNumber();				
				int childNum2 = child2.getNumber();
			
				likelihoodCore.calculatePartials(childNum1, childNum2, nodeNum);
				
				if (parent == null) {
					// No parent this is the root of the tree - 
					// calculate the pattern likelihoods
					double[] frequencies = frequencyModel.getFrequencies();						
					double[] proportions = currentSiteModel.getCategoryProportions();
					
					likelihoodCore.integratePartials(nodeNum, proportions, rootPartials);
					likelihoodCore.calculateLogLikelihoods(rootPartials, frequencies, patternLogLikelihoods);
				}
																		
				update = true;					
			}
		}
				
		return update;

	}
	
    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

	public org.w3c.dom.Element createElement(org.w3c.dom.Document d) {
		throw new RuntimeException("createElement not implemented");
	}


	/** 
	 * The XML parser
	 */
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
				
		public String getParserName() { return ADVANCED_TREE_LIKELIHOOD; }
	
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {
		
			PatternList patternList = (PatternList)xo.getChild(PatternList.class);
			TreeModel treeModel = (TreeModel)xo.getChild(TreeModel.class);
			SiteModel siteModel = (SiteModel)xo.getChild(SiteModel.class);
			
            boolean useScaling = false;

            BranchRateModel branchRateModel = (BranchRateModel)xo.getChild(BranchRateModel.class);

			AdvancedTreeLikelihood treeLikelihood = new AdvancedTreeLikelihood(patternList, treeModel, siteModel, branchRateModel, false, useScaling);

			if (xo.hasSocket(TIPS)) {
				SiteModel siteModel2 = (SiteModel)xo.getSocketChild(TIPS);
				treeLikelihood.addTipsSiteModel(siteModel2);
			}

            Parameter deltaParameter = null;
            if (xo.hasSocket(DELTA)) {
                deltaParameter = (Parameter)xo.getSocketChild(DELTA);
                treeLikelihood.addDeltaParameter(deltaParameter);
            }

			for (int i = 0; i < xo.getChildCount(); i++) {
				if (xo.getChild(i) instanceof XMLObject) {
				
					XMLObject xoc = (XMLObject)xo.getChild(i);
					if (xoc.getName().equals(CLADE)) {
					
						SiteModel siteModel2 = (SiteModel)xoc.getChild(SiteModel.class);
						TaxonList taxonList = (TaxonList)xoc.getChild(TaxonList.class);
						
						boolean includeStem = false;
						
						if (xoc.hasAttribute(INCLUDE_STEM)) {
							includeStem = xoc.getBooleanAttribute(INCLUDE_STEM);
							
							if (taxonList.getTaxonCount()==1 && !includeStem) {
								throw new XMLParseException("The site model is only applied to 1 taxon and therefore must include the stem branch"); 
							}
						} else if (taxonList.getTaxonCount()==1) {
							includeStem = true;
						}
						
						try {
						
							treeLikelihood.addCladeSiteModel(siteModel2, taxonList, includeStem);
							
						} catch (Tree.MissingTaxonException mte) {
							throw new XMLParseException("Taxon, " + mte + ", in " + getParserName() + " was not found in the tree.");
						}
					}
					
				}
			}
			
			return treeLikelihood;
		}
		
		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		
		public String getParserDescription() {
			return "This element represents the likelihood of a patternlist on a tree given the site model.";
		}
		
		public Class getReturnType() { return Likelihood.class; }
		
		public XMLSyntaxRule[] getSyntaxRules() { return rules; }
		
		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new ElementRule(TIPS, SiteModel.class, "A siteModel that will be applied only to the tips.", 0, 1),
            new ElementRule(DELTA, Parameter.class, "A parameter that specifies the amount of extra substitutions per site at each tip.", 0, 1),
			new ElementRule(CLADE,
				new XMLSyntaxRule[] {
					AttributeRule.newBooleanRule(INCLUDE_STEM, true, "determines whether or not the stem branch above this clade is included in the siteModel."),
					new ElementRule(Taxa.class, "A set of taxa which defines a clade to apply a different site model to"),
					new ElementRule(SiteModel.class, "A siteModel that will be applied only to this clade")
				}, 0, Integer.MAX_VALUE),
			new ElementRule(PatternList.class),
			new ElementRule(TreeModel.class),
			new ElementRule(SiteModel.class),
            new ElementRule(BranchRateModel.class, true)
		};
	};

    private class Clade {
		Clade(SiteModel siteModel, TaxonList taxonList, boolean includeStem) throws Tree.MissingTaxonException {
			this.siteModel = siteModel;
			this.leafSet = Tree.Utils.getLeavesForTaxa(treeModel, taxonList);
			this.includeStem = includeStem;
			if (taxonList.getTaxonCount() == 1) {
				this.includeStem = true;
			}
			
			findMRCA();
		}
		
		void findMRCA() {
			node = Tree.Utils.getCommonAncestorNode(treeModel, leafSet).getNumber();
		}
		
		int getNode() { return node; }
		boolean includeStem() { return includeStem; }
		SiteModel getSiteModel() { return this.siteModel; }
		
		SiteModel siteModel;
		Set leafSet;
		int node;
		boolean includeStem;
	};
	
	// **************************************************************
	// INSTANCE VARIABLES
	// **************************************************************

	/** the frequency model for these sites */
	protected FrequencyModel frequencyModel = null;

	/** the site model for these sites */
	protected SiteModel siteModel = null;
	
    /** the branch rate model  */
    protected BranchRateModel branchRateModel = null;

	/** the site model for the tips */
	protected SiteModel tipsSiteModel = null;

    protected Parameter deltaParameter = null;

	/** the site models for specific clades */
	protected ArrayList cladeSiteModels = new ArrayList();
	
	private boolean commonAncestorsKnown = true;

	/** the root partial likelihoods */
    protected double[] rootPartials = null;

	/** the pattern likelihoods */
    protected double[] patternLogLikelihoods = null;

	/** the number of rate categories */
	protected int categoryCount;
		
	/** an array used to store transition probabilities */
	protected double[] probabilities;
	
   /** the LikelihoodCore */
	protected LikelihoodCore likelihoodCore;
}