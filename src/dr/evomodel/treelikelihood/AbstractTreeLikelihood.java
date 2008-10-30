/*
 * AbstractTreeLikelihood.java
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
import dr.evolution.datatype.DataType;
import dr.evolution.tree.NodeRef;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.AbstractModel;
import dr.inference.model.Likelihood;
import dr.inference.model.Model;
import dr.inference.model.Parameter;

/**
 * AbstractTreeLikelihood - a base class for likelihood calculators of sites on a tree.
 *
 * @version $Id: AbstractTreeLikelihood.java,v 1.16 2005/06/07 16:27:39 alexei Exp $
 *
 * @author Andrew Rambaut
 */

public abstract class AbstractTreeLikelihood extends AbstractModel implements Likelihood {

	public AbstractTreeLikelihood(String name, PatternList patternList,
	                              TreeModel treeModel)
	{

		super(name);

		this.patternList = patternList;
		this.dataType = patternList.getDataType();
		patternCount = patternList.getPatternCount();
		stateCount = dataType.getStateCount();

		patternWeights = patternList.getPatternWeights();

		this.treeModel = treeModel;
		addModel(treeModel);

		nodeCount = treeModel.getNodeCount();

		updateNode = new boolean[nodeCount];
		for (int i = 0; i < nodeCount; i++) {
			updateNode[i] = true;
		}

		likelihoodKnown = false;

	}

	/**
	 * Sets the partials from a sequence in an alignment.
	 */
	protected final void setStates(LikelihoodCore likelihoodCore, PatternList patternList,
	                               int sequenceIndex, int nodeIndex) {
		int i;

		int[] states = new int[patternCount];

		for (i = 0; i < patternCount; i++) {

			states[i] = patternList.getPatternState(sequenceIndex, i);
		}

		likelihoodCore.setNodeStates(nodeIndex, states);
	}

	/**
	 * Sets the partials from a sequence in an alignment.
	 */
	protected final void setPartials(LikelihoodCore likelihoodCore, PatternList patternList,
	                                 int categoryCount,
	                                 int sequenceIndex, int nodeIndex) {
		double[] partials = new double[patternCount * stateCount];

		boolean[] stateSet;

		int v = 0;
		for (int i = 0; i < patternCount; i++) {

			int state = patternList.getPatternState(sequenceIndex, i);
			stateSet = dataType.getStateSet(state);

			for (int j = 0; j < stateCount; j++) {
				if (stateSet[j]) {
					partials[v] = 1.0;
				} else {
					partials[v] = 0.0;
				}
				v++;
			}
		}

		likelihoodCore.setNodePartials(nodeIndex, partials);
	}

	/**
	 * Sets the partials from a sequence in an alignment.
	 */
	protected final void setMissingStates(LikelihoodCore likelihoodCore, int nodeIndex) {
		int[] states = new int[patternCount];

		for (int i = 0; i < patternCount; i++) {
			states[i] = dataType.getGapState();
		}

		likelihoodCore.setNodeStates(nodeIndex, states);
	}

	/**
	 * Sets the partials from a sequence in an alignment.
	 */
	protected final void setMissingPartials(LikelihoodCore likelihoodCore, int nodeIndex) {
		double[] partials = new double[patternCount * stateCount];

		int v = 0;
		for (int i = 0; i < patternCount; i++) {
			for (int j = 0; j < stateCount; j++) {
				partials[v] = 1.0;
				v++;
			}
		}

		likelihoodCore.setNodePartials(nodeIndex, partials);
	}

	/**
	 * Set update flag for a node and its children
	 */
	protected void updateNode(NodeRef node) {

		updateNode[node.getNumber()] = true;
		likelihoodKnown = false;
	}

	/**
	 * Set update flag for a node and its direct children
	 */
	protected void updateNodeAndChildren(NodeRef node) {
		updateNode[node.getNumber()] = true;

		for (int i = 0; i < treeModel.getChildCount(node); i++) {
			NodeRef child = treeModel.getChild(node, i);
			updateNode[child.getNumber()] = true;
		}
		likelihoodKnown = false;
	}

	/**
	 * Set update flag for a node and all its descendents
	 */
	protected void updateNodeAndDescendents(NodeRef node) {
		updateNode[node.getNumber()] = true;

		for (int i = 0; i < treeModel.getChildCount(node); i++) {
			NodeRef child = treeModel.getChild(node, i);
			updateNodeAndDescendents(child);
		}

		likelihoodKnown = false;
	}

	/**
	 * Set update flag for all nodes
	 */
	protected void updateAllNodes() {
		for (int i = 0; i < nodeCount; i++) {
			updateNode[i] = true;
		}
		likelihoodKnown = false;
	}

	/**
	 * Set update flag for a pattern
	 */
	protected void updatePattern(int i) {
		if (updatePattern != null) {
			updatePattern[i] = true;
		}
		likelihoodKnown = false;
	}

	/**
	 * Set update flag for all patterns
	 */
	protected void updateAllPatterns() {
		if (updatePattern != null) {
			for (int i = 0; i < patternCount; i++) {
				updatePattern[i] = true;
			}
		}
		likelihoodKnown = false;
	}

	public final double[] getPatternWeights() { return patternWeights; }

	// **************************************************************
	// ParameterListener IMPLEMENTATION
	// **************************************************************

	protected void handleParameterChangedEvent(Parameter parameter, int index) {
		// do nothing
	}

	// **************************************************************
	// Model IMPLEMENTATION
	// **************************************************************

	protected void handleModelChangedEvent(Model model, Object object, int index) {
		likelihoodKnown = false;
	}

	/**
	 * Stores the additional state other than model components
	 */
	protected void storeState() {

		storedLikelihoodKnown = likelihoodKnown;
		storedLogLikelihood = logLikelihood;
	}

	/**
	 * Restore the additional stored state
	 */
	protected void restoreState() {

		likelihoodKnown = storedLikelihoodKnown;
		logLikelihood = storedLogLikelihood;
	}

	protected void acceptState() { } // nothing to do

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

	/**
	 * Forces a complete recalculation of the likelihood next time getLikelihood is called
	 */
	public void makeDirty() {
		likelihoodKnown = false;
		updateAllNodes();
		updateAllPatterns();
	}

	protected abstract double calculateLogLikelihood();

	public String toString() {
        getLogLikelihood();
        return getClass().getName() + "(" + logLikelihood + ")";

	}

	// **************************************************************
	// Loggable IMPLEMENTATION
	// **************************************************************

	/**
	 * @return the log columns.
	 */
	public dr.inference.loggers.LogColumn[] getColumns() {
		return new dr.inference.loggers.LogColumn[] {
				new LikelihoodColumn(getId())
		};
	}

	private class LikelihoodColumn extends dr.inference.loggers.NumberColumn {
		public LikelihoodColumn(String label) { super(label); }
		public double getDoubleValue() { return getLogLikelihood(); }
	}

	// **************************************************************
	// INSTANCE VARIABLES
	// **************************************************************

	/** the tree */
	protected TreeModel treeModel = null;

	/** the patternList */
	protected PatternList patternList = null;

	protected DataType dataType = null;

	/** the pattern weights */
	protected double[] patternWeights;

	/** the number of patterns */
	protected int patternCount;

	/** the number of states in the data */
	protected int stateCount;

	/** the number of nodes in the tree */
	protected int nodeCount;

	/** Flags to specify which patterns are to be updated */
	protected boolean [] updatePattern = null;

	/** Flags to specify which nodes are to be updated */
	protected boolean[] updateNode;

	private double logLikelihood;
	private double storedLogLikelihood;
	private boolean likelihoodKnown = false;
	private boolean storedLikelihoodKnown = false;

}