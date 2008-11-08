/*
 * CoalescentSimulator.java
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

package dr.evolution.coalescent;

import dr.evolution.tree.*;
import dr.evolution.util.*;
import dr.evolution.util.Date;
import dr.math.MathUtils;
import dr.util.HeapSort;

import java.util.*;

/**
 * This class provides the basic engine for coalescent simulation of a given demographic model over a given time period.
 * The input is a set of nodes (some of which may be subtrees) and the output is a new (smaller) set of nodes after
 * some coalescence over a certain time period.
 *
 * $Id: CoalescentSimulator.java,v 1.9 2005/05/24 20:25:55 rambaut Exp $
 *
 * @author Alexei Drummond
 *
 */
public class CoalescentSimulator {
    
    public CoalescentSimulator() {}


	/**
	 * Simulates a coalescent tree, given a taxon list.
	 * @param taxa the set of taxa to simulate a coalescent tree between
	 * @param demoFunction the demographic function to use
	 */
	public SimpleTree simulateTree(TaxonList taxa, DemographicFunction demoFunction) {

        if( taxa.getTaxonCount() == 0 ) return new SimpleTree();

        SimpleNode[] nodes = new SimpleNode[taxa.getTaxonCount()];
		for (int i = 0; i < taxa.getTaxonCount(); i++) {
			nodes[i] = new SimpleNode();
			nodes[i].setTaxon(taxa.getTaxon(i));
		}

		dr.evolution.util.Date mostRecent = null;
		boolean usingDates = false;

		for (int i = 0; i < taxa.getTaxonCount(); i++) {
			if (TaxonList.Utils.hasAttribute(taxa, i, dr.evolution.util.Date.DATE)) {
				usingDates = true;
				dr.evolution.util.Date date = (dr.evolution.util.Date)taxa.getTaxonAttribute(i, dr.evolution.util.Date.DATE);
				if ((date != null) && (mostRecent == null || date.after(mostRecent))) {
					mostRecent = date;
				}
			} else {
				// assume contemporaneous tips
				nodes[i].setHeight(0.0);
			}
		}

		if (usingDates && mostRecent != null ) {
			TimeScale timeScale = new TimeScale(mostRecent.getUnits(), true, mostRecent.getAbsoluteTimeValue());

			for (int i =0; i < taxa.getTaxonCount(); i++) {
				dr.evolution.util.Date date = (dr.evolution.util.Date)taxa.getTaxonAttribute(i, dr.evolution.util.Date.DATE);

				if (date == null) {
					throw new IllegalArgumentException("Taxon, " + taxa.getTaxonId(i) + ", is missing its date");
				}

				nodes[i].setHeight(timeScale.convertTime(date.getTimeValue(), date));
			}
			if (demoFunction.getUnits() != mostRecent.getUnits()) {
				//throw new IllegalArgumentException("The units of the demographic model and the most recent date must match!");
			}
		}

		return new SimpleTree(simulateCoalescent(nodes, demoFunction));
	}

	/**
	 * @return the root node of the given array of nodes after simulation of the coalescent under the given demographic model.
	 */
	public SimpleNode simulateCoalescent(SimpleNode[] nodes, DemographicFunction demographic) {
        // sanity check - disjoint trees

        if( ! Tree.Utils.allDisjoint(nodes) ) {
            throw new RuntimeException("non disjoint trees");
        }

        if( nodes.length == 0 ) {
             throw new IllegalArgumentException("empty nodes set") ;
        }

        for(int attempts = 0; attempts < 1000; ++attempts) {
            SimpleNode[] rootNode = simulateCoalescent(nodes, demographic, 0.0, Double.POSITIVE_INFINITY);
            if( rootNode.length == 1 ) {
                return rootNode[0];
            }
        }

        throw new RuntimeException("failed to merge trees after 1000 tries!");
	}

	public SimpleNode[] simulateCoalescent(SimpleNode[] nodes, DemographicFunction demographic, double currentHeight, double maxHeight) {
        // If only one node, return it
        // continuing results in an infinite loop
        if( nodes.length == 1 ) return nodes;

        double[] heights = new double[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			heights[i] = nodes[i].getHeight();
		}
		int[] indices = new int[nodes.length];
		HeapSort.sort(heights, indices);

		// node list
		nodeList.clear();
		activeNodeCount = 0;
		for (int i = 0; i < nodes.length; i++) {
			nodeList.add(nodes[indices[i]]);
		}
		setCurrentHeight(currentHeight);

		// get at least two tips
		while (getActiveNodeCount() < 2) {
			currentHeight = getMinimumInactiveHeight();
			setCurrentHeight(currentHeight);
		}

		// simulate coalescent events
		double nextCoalescentHeight = currentHeight + DemographicFunction.Utils.getSimulatedInterval(demographic, getActiveNodeCount(), currentHeight);

		while (nextCoalescentHeight < maxHeight && (getNodeCount() > 1)) {

			if (nextCoalescentHeight >= getMinimumInactiveHeight()) {
				currentHeight = getMinimumInactiveHeight();
				setCurrentHeight(currentHeight);
			} else {
				currentHeight = nextCoalescentHeight;
				coalesceTwoActiveNodes(currentHeight);
			}

			if (getNodeCount() > 1) {
				// get at least two tips
				while (getActiveNodeCount() < 2) {
					currentHeight = getMinimumInactiveHeight();
					setCurrentHeight(currentHeight);
				}

	//			nextCoalescentHeight = currentHeight + DemographicFunction.Utils.getMedianInterval(demographic, getActiveNodeCount(), currentHeight);
				nextCoalescentHeight = currentHeight + DemographicFunction.Utils.getSimulatedInterval(demographic, getActiveNodeCount(), currentHeight);
			}
		}

		SimpleNode[] nodesLeft = new SimpleNode[nodeList.size()];
		for (int i = 0; i < nodesLeft.length; i++) {
			nodesLeft[i] = nodeList.get(i);
		}

		return nodesLeft;
	}

	/**
	 * @return the height of youngest inactive node.
	 */
	private double getMinimumInactiveHeight() {
		if (activeNodeCount < nodeList.size()) {
			return (nodeList.get(activeNodeCount)).getHeight();
		} else return Double.POSITIVE_INFINITY;
	}

	/**
	 * Set the current height.
	 */
	private void setCurrentHeight(double height) {
		while (getMinimumInactiveHeight() <= height) {
			activeNodeCount += 1;
		}
	}

	/**
	 * @return the numver of active nodes (equate to lineages)
	 */
	private int getActiveNodeCount() {
		return activeNodeCount;
	}

	/**
	 * @return the total number of nodes both active and inactive
	 */
	private int getNodeCount() {
		return nodeList.size();
	}

	/**
	 * Coalesce two nodes in the active list. This method removes the two (randomly selected) active nodes
	 * and replaces them with the new node at the top of the active list.
	 */
	private void coalesceTwoActiveNodes(double height) {
		int node1 = MathUtils.nextInt(activeNodeCount);
		int node2 = node1;
		while (node2 == node1) {
			node2 = MathUtils.nextInt(activeNodeCount);
		}

		SimpleNode left = nodeList.get(node1);
		SimpleNode right = nodeList.get(node2);

		SimpleNode newNode = new SimpleNode();
		newNode.setHeight(height);
		newNode.addChild(left);
		newNode.addChild(right);

		nodeList.remove(left);
		nodeList.remove(right);

		activeNodeCount -= 2;

		nodeList.add(activeNodeCount, newNode);

		activeNodeCount += 1;

		if (getMinimumInactiveHeight() < height) {
			throw new RuntimeException("This should never happen! Somehow the current active node is older than the next inactive node!");
		}
	}

	private ArrayList<SimpleNode> nodeList = new ArrayList<SimpleNode>();
	private int activeNodeCount = 0;

	public static void main(String[] args) {

		double[] samplingTimes = new double[] {
				0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0
		};

		ExponentialGrowth exponentialGrowth = new ExponentialGrowth(Units.Type.YEARS);
		exponentialGrowth.setN0(10);
		exponentialGrowth.setGrowthRate(0.5);

		ConstantPopulation constantPopulation = new ConstantPopulation(Units.Type.YEARS);
		constantPopulation.setN0(10);

		Taxa taxa = new Taxa();
		int i = 1;
		for (double time : samplingTimes) {
			Taxon taxon = new Taxon("tip" + i);
			taxon.setAttribute("date", new Date(time, Units.Type.YEARS, true));
			i++;
			taxa.addTaxon(taxon);
		}
		CoalescentSimulator simulator = new CoalescentSimulator();
		Tree tree = simulator.simulateTree(taxa, exponentialGrowth);

		List<Double> heights = new ArrayList<Double>();
		for (int j = 0; j < tree.getInternalNodeCount(); j++) {
			heights.add(tree.getNodeHeight(tree.getInternalNode(j)));
		}

		Collections.sort(heights);

		for (int j = 0; j < heights.size(); j++) {
			System.out.println(j + "\t" + heights.get(j));
		}

	}

}