/*
 * SpeciationModel.java
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

package dr.evomodel.speciation;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Units;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;

/**
 * This interface provides methods that describe a speciation model.
 * @author Andrew Rambaut
 */
public abstract class SpeciationModel extends AbstractModel implements Units {

	public SpeciationModel(String modelName, Type units) {
		super(modelName);
		setUnits(units);
	}

    //
    //  "fixed" part of likelihood
    //
    public abstract double logTreeProbability(int taxonCount);
	//
	// Per node part of likelihood
	//
	public abstract double logNodeProbability(Tree tree, NodeRef node);

    public abstract boolean includeExternalNodesInLikelihoodCalculation();

    /**
     * Generic likelihood calculation
     * @param tree
     * @return
     */
    public double calculateTreeLogLikelihood(Tree tree) {
        final int taxonCount = tree.getExternalNodeCount();
        double logL = logTreeProbability(taxonCount);

        for (int j = 0; j < tree.getInternalNodeCount(); j++) {
            logL += logNodeProbability(tree, tree.getInternalNode(j));
        }

        if (includeExternalNodesInLikelihoodCalculation()) {
            for (int j = 0; j < taxonCount; j++) {
                logL += logNodeProbability(tree, tree.getExternalNode(j));
            }
        }

        return logL;
    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************
	
	protected void handleModelChangedEvent(Model model, Object object, int index) {
		// no intermediates need to be recalculated...
	}
	
	protected void handleParameterChangedEvent(Parameter parameter, int index) {
		// no intermediates need to be recalculated...
	}
	
	protected void storeState() {} // no additional state needs storing
	protected void restoreState() {} // no additional state needs restoring	
	protected void acceptState() {} // no additional state needs accepting	

    // **************************************************************
    // Units IMPLEMENTATION
    // **************************************************************

	/**
	 * Units in which population size is measured.
	 */
	private Type units;

	/**
	 * sets units of measurement.
	 *
	 * @param u units
	 */
	public void setUnits(Type u)
	{
		units = u;
	}

	/**
	 * returns units of measurement.
	 */
	public Type getUnits()
	{
		return units;
	}
}