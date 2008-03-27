/*
 * NewCoalescentLikelihood.java
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

package dr.evomodel.coalescent;

import dr.evolution.coalescent.Coalescent;
import dr.evolution.coalescent.DemographicFunction;
import dr.evolution.coalescent.Intervals;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.*;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.*;
import dr.xml.*;

import java.util.*;


/**
 * A likelihood function for the coalescent. Takes a tree and a demographic model.
 *
 * Parts of this class were derived from C++ code provided by Oliver Pybus.
 *
 * @version $Id: NewCoalescentLikelihood.java,v 1.6 2005/05/24 20:25:57 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class CoalescentLikelihood extends AbstractModel implements Likelihood, Units {

	// PUBLIC STUFF

	public static final String COALESCENT_LIKELIHOOD = "coalescentLikelihood";
	public static final String MODEL = "model";
	public static final String POPULATION_TREE = "populationTree";

	public static final String INCLUDE = "include";
	public static final String EXCLUDE = "exclude";

	public CoalescentLikelihood(Tree tree,
	                            TaxonList includeSubtree,
	                            List<TaxonList> excludeSubtrees,
	                            DemographicModel demoModel) throws Tree.MissingTaxonException {
		this(COALESCENT_LIKELIHOOD, tree, includeSubtree, excludeSubtrees, demoModel);
	}

	public CoalescentLikelihood(String name,
	                            Tree tree,
	                            TaxonList includeSubtree,
	                            List<TaxonList> excludeSubtrees,
	                            DemographicModel demoModel) throws Tree.MissingTaxonException {

		super(name);

		this.tree = tree;
		this.demoModel = demoModel;

		if (includeSubtree != null) {
			includedLeafSet = Tree.Utils.getLeavesForTaxa(tree, includeSubtree);
		} else {
			includedLeafSet = null;
		}

		if (excludeSubtrees != null) {
			excludedLeafSets = new Set[excludeSubtrees.size()];
			for (int i =0; i < excludeSubtrees.size(); i++) {
				excludedLeafSets[i] = Tree.Utils.getLeavesForTaxa(tree, excludeSubtrees.get(i));
			}
		} else {
			excludedLeafSets = new Set[0];
		}

		addModel(demoModel);

		if (tree instanceof TreeModel) {
			addModel((TreeModel)tree);
		}

		intervals = new Intervals(tree.getNodeCount());
		storedIntervals = new Intervals(tree.getNodeCount());
		eventsKnown = false;

		addStatistic(new DeltaStatistic());

		likelihoodKnown = false;
	}

	// **************************************************************
	// ModelListener IMPLEMENTATION
	// **************************************************************

	protected final void handleModelChangedEvent(Model model, Object object, int index) {
		if (model == tree) {
			// treeModel has changed so recalculate the intervals
			eventsKnown = false;
		} else {
			// demoModel has changed so we don't need to recalculate the intervals
		}

		likelihoodKnown = false;
	}

	// **************************************************************
	// ParameterListener IMPLEMENTATION
	// **************************************************************

	protected final void handleParameterChangedEvent(Parameter parameter, int index) { } // No parameters to respond to

	// **************************************************************
	// Model IMPLEMENTATION
	// **************************************************************

	/**
	 * Stores the precalculated state: in this case the intervals
	 */
	protected final void storeState() {
		// copy the intervals into the storedIntervals
		storedIntervals.copyIntervals(intervals);

		storedEventsKnown = eventsKnown;
		storedLikelihoodKnown = likelihoodKnown;
		storedLogLikelihood = logLikelihood;
	}

	/**
	 * Restores the precalculated state: that is the intervals of the tree.
	 */
	protected final void restoreState() {
		// swap the intervals back
		Intervals tmp = storedIntervals;
		storedIntervals = intervals;
		intervals = tmp;

		eventsKnown = storedEventsKnown;
		likelihoodKnown = storedLikelihoodKnown;
		logLikelihood = storedLogLikelihood;

		if (!eventsKnown) {
			likelihoodKnown = false;
		}
	}

	protected final void acceptState() { } // nothing to do

	// **************************************************************
	// Likelihood IMPLEMENTATION
	// **************************************************************

	public final Model getModel() { return this; }

	public final double getLogLikelihood() {
		if (!likelihoodKnown) {
			logLikelihood = calculateLogLikelihood();
			likelihoodKnown = true;
		}
		return logLikelihood;
	}

	public final void makeDirty() {
		likelihoodKnown = false;
		eventsKnown = false;
	}

	/**
	 * Calculates the log likelihood of this set of coalescent intervals,
	 * given a demographic model.
	 */
	public double calculateLogLikelihood() {

		if (!eventsKnown) {
			setupIntervals();
		}

		DemographicFunction demoFunction = demoModel.getDemographicFunction();

		return Coalescent.calculateLogLikelihood(intervals, demoFunction);
	}

	/**
	 * Recalculates all the intervals from the tree model.
	 */
	protected final void setupIntervals() {

		intervals.resetEvents();
		collectTimes(tree, tree.getRoot(), intervals);
		// force a calculation of the intervals...
		intervals.getIntervalCount();

		eventsKnown = true;
	}


	/**
	 * extract coalescent times and tip information into ArrayList times from tree.
	 * @param tree the tree
	 * @param node the node to start from
	 * @param intervals the intervals object to store the events
	 */
	private void collectTimes(Tree tree, NodeRef node, Intervals intervals) {

		intervals.addCoalescentEvent(tree.getNodeHeight(node));

		for (int i = 0; i < tree.getChildCount(node); i++) {
			NodeRef child = tree.getChild(node, i);

			if (tree.isExternal(child)) {

				intervals.addSampleEvent(tree.getNodeHeight(child));

			} else {

				collectTimes(tree, child, intervals);
			}
		}
	}


	// **************************************************************
	// Loggable IMPLEMENTATION
	// **************************************************************

	/**
	 * @return the log columns.
	 */
	public final dr.inference.loggers.LogColumn[] getColumns() {
		return new dr.inference.loggers.LogColumn[] {
				new LikelihoodColumn(getId())
		};
	}

	private final class LikelihoodColumn extends dr.inference.loggers.NumberColumn {
		public LikelihoodColumn(String label) { super(label); }
		public double getDoubleValue() { return getLogLikelihood(); }
	}

	public String toString() {
		return Double.toString(logLikelihood);

	}


	// **************************************************************
	// Units IMPLEMENTATION
	// **************************************************************

	/**
	 * Sets the units these coalescent intervals are
	 * measured in.
	 */
	public final void setUnits(Type u)
	{
		demoModel.setUnits(u);
	}

	/**
	 * Returns the units these coalescent intervals are
	 * measured in.
	 */
	public final Type getUnits()
	{
		return demoModel.getUnits();
	}

	// ****************************************************************
	// Inner classes
	// ****************************************************************

	public class DeltaStatistic extends Statistic.Abstract {

		public DeltaStatistic() {
			super("delta");
		}

		public int getDimension() { return 1; }

		public double getStatisticValue(int i) {
			throw new RuntimeException("Not implemented");
//			return IntervalList.Utils.getDelta(intervals);
		}

	}

	// ****************************************************************
	// Private and protected stuff
	// ****************************************************************

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() { return COALESCENT_LIKELIHOOD; }

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			XMLObject cxo = (XMLObject)xo.getChild(MODEL);
			DemographicModel demoModel = (DemographicModel)cxo.getChild(DemographicModel.class);

			cxo = (XMLObject)xo.getChild(POPULATION_TREE);
			TreeModel treeModel = (TreeModel)cxo.getChild(TreeModel.class);

			TaxonList includeSubtree = null;

			if (xo.hasSocket(INCLUDE)) {
				includeSubtree = (TaxonList)xo.getSocketChild(INCLUDE);
			}

			List<TaxonList> excludeSubtrees = new ArrayList<TaxonList>();

			if (xo.hasSocket(EXCLUDE)) {
				cxo = (XMLObject)xo.getChild(EXCLUDE);
				for (int i =0; i < cxo.getChildCount(); i++) {
					excludeSubtrees.add((TaxonList)cxo.getChild(i));
				}
			}

			try {
				return new CoalescentLikelihood(treeModel, includeSubtree, excludeSubtrees, demoModel);
			} catch (Tree.MissingTaxonException mte) {
				throw new XMLParseException("treeModel missing a taxon from taxon list in " + getParserName() + " element");
			}
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "This element represents the likelihood of the tree given the demographic function.";
		}

		public Class getReturnType() { return Likelihood.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
				new ElementRule(MODEL, new XMLSyntaxRule[] {
						new ElementRule(DemographicModel.class)
				}, "The demographic model which describes the coalescent rate over time"),
				new ElementRule(POPULATION_TREE, new XMLSyntaxRule[] {
						new ElementRule(TreeModel.class)
				}, "The treeModel"),
				new ElementRule(INCLUDE, new XMLSyntaxRule[] {
						new ElementRule(Taxa.class)
				}, "An optional subset of taxa on which to calculate the likelihood (should be monophyletic)", true),
				new ElementRule(EXCLUDE, new XMLSyntaxRule[] {
						new ElementRule(Taxa.class, 1, Integer.MAX_VALUE)
				}, "One or more subsets of taxa which should be excluded from calculate the likelihood (should be monophyletic)", true)
		};
	};



	// ****************************************************************
	// Private and protected stuff
	// ****************************************************************


	/** The demographic model. */
	private DemographicModel demoModel = null;

	/** The tree. */
	private Tree tree = null;
	private final Set<String> includedLeafSet;
	private final Set[] excludedLeafSets;

	/** The intervals. */
	private Intervals intervals = null;

	/** The stored values for intervals. */
	private Intervals storedIntervals = null;

	private boolean eventsKnown = false;
	private boolean storedEventsKnown = false;

	private double logLikelihood;
	private double storedLogLikelihood;
	private boolean likelihoodKnown = false;
	private boolean storedLikelihoodKnown = false;
}