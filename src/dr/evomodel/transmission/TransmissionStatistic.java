/*
 * TransmissionStatistic.java
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

package dr.evomodel.transmission;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxon;
import dr.evomodel.tree.TreeStatistic;
import dr.inference.model.BooleanStatistic;
import dr.inference.model.Statistic;
import dr.xml.*;


/**
 * A statistic for the compatibility of a viruses tree with a transmission
 * history. The transmission history consists of a number of
 * hosts with known history of transmission. The viruses tree should have tip
 * attributes specifying which host they are from (host="").
 *
 * @version $Id: TransmissionStatistic.java,v 1.11 2005/06/27 21:19:15 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class TransmissionStatistic extends BooleanStatistic implements TreeStatistic {

	// PUBLIC STUFF

	public static final String TRANSMISSION_STATISTIC = "transmissionStatistic";

	public TransmissionStatistic(String name, Tree hostTree, Tree virusTree) {

		super(name);

		this.hostTree = hostTree;
		this.virusTree = virusTree;

		setupHosts();
	}

	private void setupHosts() {

		hostCount = hostTree.getTaxonCount();

		donorHost = new int[hostCount];
		donorHost[0] = -1;
		transmissionTime = new double[hostCount];
		transmissionTime[0] = Double.POSITIVE_INFINITY;

		setupHosts(hostTree.getRoot());
	}

	private int setupHosts(NodeRef node) {

		int host;

		if (hostTree.isExternal(node)) {
			host = node.getNumber();
		} else {

		// This traversal assumes that the first child is the donor
		// and the second is the recipient

			int host1 = setupHosts(hostTree.getChild(node, 0));
			int host2 = setupHosts(hostTree.getChild(node, 1));

			donorHost[host2] = host1;
			transmissionTime[host2] = hostTree.getNodeHeight(node);

			host = host1;
		}

		return host;
	}

	public void setTree(Tree tree) { this.virusTree = tree; }
	public Tree getTree() { return virusTree; }

	public int getDimension() { return 1; }

	/**
	 * @return true if the population tree is compatible with the species tree
	 */
    public boolean getBoolean(int dim) {

		return (isCompatible(virusTree.getRoot()) != -1);
	}

	private int isCompatible(NodeRef node) {

		double height = virusTree.getNodeHeight(node);
		int host;

		if (virusTree.isExternal(node)) {
			Taxon hostTaxon = (Taxon)virusTree.getTaxonAttribute(node.getNumber(), "host");
			host = hostTree.getTaxonIndex(hostTaxon);

			if (height > transmissionTime[host]) return -1;

		} else {

			// Tree should be bifurcating...
			int host1 = isCompatible(virusTree.getChild(node, 0));
			if (host1 == -1) return -1;

			int host2 = isCompatible(virusTree.getChild(node, 1));
			if (host2 == -1) return -1;

			if (host1 == -1 || host2 == -1);
			while (height > transmissionTime[host1]) {
				host1 = donorHost[host1];
			}

			while (height > transmissionTime[host2]) {
				host2 = donorHost[host2];
			}

			if (host1 != host2) return -1;

			host = host1;
		}

		return host;
	}

	// ****************************************************************
	// Private and protected stuff
	// ****************************************************************

	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() { return TRANSMISSION_STATISTIC; }

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			String name = xo.getStringAttribute("name");
			Tree hostTree = (Tree)xo.getSocketChild("hostTree");
			Tree virusTree = (Tree)xo.getSocketChild("parasiteTree");
			return new TransmissionStatistic(name, hostTree, virusTree);
		}

		public String getParserDescription() {
			return "A statistic that returns true if the given parasite tree is compatible with the host tree.";
		}

		public Class getReturnType() { return Statistic.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new StringAttributeRule("name", "A name for this statistic for the purpose of logging"),
			new ElementRule("hostTree",
				new XMLSyntaxRule[] { new ElementRule(Tree.class) }),
			new ElementRule("parasiteTree",
				new XMLSyntaxRule[] { new ElementRule(Tree.class) })
		};
	};

	/** The host tree. */
	private Tree hostTree = null;

	/** The viruses tree. */
	private Tree virusTree = null;

	/** The number of hosts. */
	private int hostCount;

	/** The donor host for each recipient host (-1 for initial host). */
	private int[] donorHost;

	/** The time of transmission into this host (POSITIVE_INFINITY for initial host). */
	private double[] transmissionTime;
}