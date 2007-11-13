/*
 * FlexibleTree.java
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

package dr.evolution.tree;

import dr.evolution.util.MutableTaxonListListener;
import dr.evolution.util.Taxon;
import dr.util.Attributable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * data structure for binary rooted trees
 *
 * @version $Id: FlexibleTree.java,v 1.34 2006/07/02 21:14:52 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 *
 */
public class FlexibleTree implements MutableTree {

	/** Constructor tree with no nodes. Use adoptNodes to add some nodes. */
	public FlexibleTree() {
		root = null;
	}

	/** clone constructor */
	public FlexibleTree(Tree tree) {

		setUnits(tree.getUnits());

		root = new FlexibleNode(tree, tree.getRoot());

		nodeCount = tree.getNodeCount();
		internalNodeCount = tree.getInternalNodeCount();
		externalNodeCount = tree.getExternalNodeCount();


        nodes = new FlexibleNode[nodeCount];

		FlexibleNode node = root;
		do {
			node = (FlexibleNode)Tree.Utils.postorderSuccessor(this, node);
			if ((node.getNumber() >= externalNodeCount && node.isExternal()) ||
				(node.getNumber() < externalNodeCount && !node.isExternal())) {
				throw new RuntimeException("Error cloning tree: node numbers are incompatible");
			}
			nodes[node.getNumber()] = node;
		} while (node != root);

		heightsKnown = tree.hasNodeHeights();
		lengthsKnown = tree.hasBranchLengths();
	}

	/** clone constructor */
	public FlexibleTree(FlexibleNode root) {

		adoptNodes(root);

		this.heightsKnown = true;
		this.lengthsKnown = true;

	}

	/** clone constructor */
	public FlexibleTree(FlexibleNode root, boolean heightsKnown, boolean lengthsKnown) {

		adoptNodes(root);

		this.heightsKnown = heightsKnown;
		this.lengthsKnown = lengthsKnown;

	}

	/**
	 * @return a copy of this tree
	 */
	public Tree getCopy() {
		return new FlexibleTree(this);
	}

	/**
	 * Adopt a node hierarchy as its own. Only called by the FlexibleTree(FlexibleNode, TaxonList).
	 * This creates the node list and stores the nodes in post-traversal order.
	 */
	protected void adoptNodes(FlexibleNode node) {

		if (inEdit) throw new RuntimeException("Mustn't be in an edit transaction to call this method!");

		internalNodeCount = 0;
		externalNodeCount = 0;

		root = node;

		do {
			node = (FlexibleNode)Tree.Utils.postorderSuccessor(this, node);
			if (node.isExternal()) {
				externalNodeCount++;
			} else
				internalNodeCount++;

		} while(node != root);

		nodeCount = internalNodeCount + externalNodeCount;
		//System.out.println("internal count = " + internalNodeCount);
		//System.out.println("external count = " + externalNodeCount);

		nodes = new FlexibleNode[nodeCount];

		node = root;
		int i = 0;
		int j = externalNodeCount;

		do {
			node = (FlexibleNode)Tree.Utils.postorderSuccessor(this, node);
			//System.out.print("node = " + node.getId() + " ");
			if (node.isExternal()) {
				node.setNumber(i);
				//System.out.println("  leaf number " + i);
				nodes[i] = node;
				i++;
			} else {
				node.setNumber(j);
				//System.out.println("  ancestor number " + j);
				nodes[j] = node;
				j++;
			}
		} while(node != root);
	}

	/**
	 * Return the units that this tree is expressed in.
	 */
	public final int getUnits() {
		return units;
	}

	/**
	 * Sets the units that this tree is expressed in.
	 */
	public final void setUnits(int units) {
		this.units = units;
	}

	/**
	 * @return a count of the number of nodes (internal + external) in this
	 * tree.
	 */
	public int getNodeCount() {
		return nodeCount;
	}

	public boolean hasNodeHeights() {
		return heightsKnown;
	}

	public double getNodeHeight(NodeRef node) {
		if (!heightsKnown) {
			calculateNodeHeights();
		}
		return ((FlexibleNode)node).getHeight();
	}

	public boolean hasBranchLengths() {
		return lengthsKnown;
	}

	public double getBranchLength(NodeRef node) {
		if (!lengthsKnown) {
			calculateBranchLengths();
		}
		return ((FlexibleNode)node).getLength();
	}

	public double getNodeRate(NodeRef node) { return ((FlexibleNode)node).getRate(); }
	public Taxon getNodeTaxon(NodeRef node) { return ((FlexibleNode)node).getTaxon(); }
	public int getChildCount(NodeRef node) { return ((FlexibleNode)node).getChildCount(); }
	public boolean isExternal(NodeRef node) { return ((FlexibleNode)node).getChildCount() == 0; }
	public boolean isRoot(NodeRef node) { return (node == root); }
	public NodeRef getChild(NodeRef node, int i) { return ((FlexibleNode)node).getChild(i); }
	public NodeRef getParent(NodeRef node) { return ((FlexibleNode)node).getParent(); }


	public final NodeRef getExternalNode(int i) {
		return nodes[i];
	}

	public final NodeRef getInternalNode(int i) {
		return nodes[i+externalNodeCount];
	}

	public final NodeRef getNode(int i) {
		return nodes[i];
	}

	/**
	 * Returns the number of external nodes.
	 */
	public final int getExternalNodeCount() {
		return externalNodeCount;
	}

	/**
	 * Returns the ith internal node.
	 */
	public final int getInternalNodeCount() {
		return internalNodeCount;
	}

	/**
	 * Returns the root node of this tree.
	 */
	public final NodeRef getRoot() {
		return root;
	}

	/**
	 * Set a new node as root node.
	 */
	public final void setRoot(NodeRef r) {

		if (!inEdit) throw new RuntimeException("Must be in edit transaction to call this method!");

		if (!(r instanceof FlexibleNode)) { throw new IllegalArgumentException(); }
		root = (FlexibleNode)r;
	}


	/**
	 * @return the height of the root node.
	 */
	public final double getRootHeight() {
		return getNodeHeight(root);
	}

	/**
	 * Set the height of the root node.
	 */
	public final void setRootHeight(double height) {
		setNodeHeight(root, height);

		fireTreeChanged();
	}


	public void addChild(NodeRef p, NodeRef c) {
		if (!inEdit) throw new RuntimeException("Must be in edit transaction to call this method!");
		FlexibleNode parent = (FlexibleNode)p;
		FlexibleNode child = (FlexibleNode)c;
		if (parent.hasChild(child)) throw new IllegalArgumentException("Child already existists in parent");

		parent.addChild(child);
	}

	public void removeChild(NodeRef p, NodeRef c) {

		if (!inEdit) throw new RuntimeException("Must be in edit transaction to call this method!");

		FlexibleNode parent = (FlexibleNode)p;
		FlexibleNode child = (FlexibleNode)c;

		for (int i = 0; i < parent.getChildCount(); i++) {
			if (parent.getChild(i) == child) {
				parent.removeChild(i);
				return;
			}
		}
	}

	public void beginTreeEdit() {
		inEdit = true;
	}

	public void endTreeEdit() {
		inEdit = false;

		fireTreeChanged();
	}

	public void setNodeHeight(NodeRef n, double height) {
		if (!heightsKnown) {
			calculateNodeHeights();
		}
		FlexibleNode node = (FlexibleNode)n;
		node.setHeight(height);

		lengthsKnown = false;

		fireTreeChanged();
	}

	public void setBranchLength(NodeRef n, double length) {
		if (!lengthsKnown) {
			calculateBranchLengths();
		}

		FlexibleNode node = (FlexibleNode)n;
		node.setLength(length);

		heightsKnown = false;

		fireTreeChanged();
	}

	public void setNodeRate(NodeRef n, double rate) {
		FlexibleNode node = (FlexibleNode)n;
		node.setRate(rate);

		fireTreeChanged();
	}

	/**
	 * Set the node heights from the current branch lengths.
	 */
	protected void calculateNodeHeights() {

		if (!lengthsKnown) {
			throw new IllegalArgumentException("Branch lengths not known");
		}

		nodeLengthsToHeights((FlexibleNode)getRoot(), 0.0);

		double maxHeight = 0.0;
		FlexibleNode node;
		for (int i = 0; i < getExternalNodeCount(); i++) {
			node = (FlexibleNode)getExternalNode(i);
			if (node.getHeight() > maxHeight) {
				maxHeight = node.getHeight();
			}
		}

		for (int i = 0; i < getNodeCount(); i++) {
			node = (FlexibleNode)getNode(i);
			node.setHeight(maxHeight - node.getHeight());
		}

		heightsKnown = true;
	}

	/**
	 * Set the node heights from the current node branch lengths. Actually
	 * sets distance from root so the heights then need to be reversed.
	 */
	private void nodeLengthsToHeights(FlexibleNode node, double height) {

		double newHeight = height;

		if (node.getLength() > 0.0) {
			newHeight += node.getLength();
		}

		node.setHeight(newHeight);

		for (int i = 0; i < node.getChildCount(); i++) {
			nodeLengthsToHeights(node.getChild(i), newHeight);
		}
	}

	/**
	 * Calculate branch lengths from the current node heights.
	 */
	protected void calculateBranchLengths() {

		nodeHeightsToLengths((FlexibleNode)getRoot(), getRootHeight());

		lengthsKnown = true;
	}

	/**
	 * Calculate branch lengths from the current node heights.
	 */
	private void nodeHeightsToLengths(FlexibleNode node, double height) {

		node.setLength(height - node.getHeight());

		for (int i = 0; i < node.getChildCount(); i++) {
			nodeHeightsToLengths(node.getChild(i), node.getHeight());
		}

	}

	/**
	 * Re-root the tree on the branch above the given node at the given height.
	 */
	public void changeRoot(NodeRef node, double height) {

		FlexibleNode node1 = (FlexibleNode)node;
		FlexibleNode parent = node1.getParent();
		if (parent == null || parent == root) {
			// the node is already the root so nothing to do...
			return;
		}

		beginTreeEdit();

		double l1 = height - getNodeHeight(node);
		if (l1 < 0.0) {
			throw new IllegalArgumentException("New root height less than the node's height");
		}

		double l2 = getNodeHeight(parent) - height;
		if (l2 < 0.0) {
			throw new IllegalArgumentException("New root height above the node's parent's height");
		}

		if (!lengthsKnown) {
			calculateBranchLengths();
		}

		FlexibleNode parent2 = parent.getParent();

		swapParentNode(parent, parent2, null);

		// the root is now free so use it as the root again
		parent.removeChild(node1);
		root.addChild(node1);
		root.addChild(parent);

		node1.setLength(l1);
		parent.setLength(l2);

		heightsKnown = false;

		endTreeEdit();
	}

	/**
	 * Work up through the tree putting the parent into the child.
	 */
	private void swapParentNode(FlexibleNode node, FlexibleNode parent, FlexibleNode child) {

		if (parent != null) {
			FlexibleNode parent2 = parent.getParent();

			swapParentNode(parent, parent2, node);

			if (child != null) {
				node.removeChild(child);
				child.addChild(node);
				node.setLength(child.getLength());
			}

		} else {
			// First remove child from the root
			node.removeChild(child);
			if (node.getChildCount() > 1) {
				throw new IllegalArgumentException("Trees must be binary");
			}

			FlexibleNode tmp = node.getChild(0);
			node.removeChild(tmp);
			child.addChild(tmp);
			tmp.setLength(tmp.getLength() + child.getLength());
		}

	}

	/**
	 * Resolve tree so that it is fully bifurcating. Resolving nodes arbitrarily
	 */
	public void resolveTree() {

		for (int i = 0; i < getInternalNodeCount(); i++) {

			FlexibleNode node = ((FlexibleNode)getInternalNode(i));

			if (node.getChildCount() > 2) {
				resolveNode(node);

			}
		}

		adoptNodes(root);

		fireTreeChanged();
	}

	/**
	 * Resolve a node  so that it is fully bifurcating.
	 */
	private void resolveNode(FlexibleNode node) {

		while (node.getChildCount() > 2) {
			FlexibleNode node0 = node.getChild(0);
			FlexibleNode node1 = node.getChild(1);

			node.removeChild(node0);
			node.removeChild(node1);

			FlexibleNode node2 = node.getShallowCopy();
			node2.addChild(node0);
			node2.addChild(node1);

			node.addChild(node2);
		}
	}

	/**
	 * Sets an named attribute for a given node.
	 * @param node the node whose attribute is being set.
	 * @param name the name of the attribute.
	 * @param value the new value of the attribute.
	 */
	public void setNodeAttribute(NodeRef node, String name, Object value) {
		((FlexibleNode)node).setAttribute(name, value);

		fireTreeChanged();
	}

	/**
	 * @return an object representing the named attributed for the given node.
	 * @param node the node whose attribute is being fetched.
	 * @param name the name of the attribute of interest.
	 */
	public Object getNodeAttribute(NodeRef node, String name) {
		return ((FlexibleNode)node).getAttribute(name);
	}

    /**
     * @return an interator of attribute names available for this node.
     * @return a key set of attribute names available for this node.
     */
    public Iterator getNodeAttributeNames(NodeRef node) {
        return ((FlexibleNode)node).getAttributeNames();
    }

    // **************************************************************
    // TaxonList IMPLEMENTATION
    // **************************************************************

	/**
	 * @return a count of the number of taxa in the list.
	 */
	public int getTaxonCount() {
		return getExternalNodeCount();
	}

	/**
	 * @return the ith taxon in the list.
	 */
	public Taxon getTaxon(int taxonIndex) {
		return ((FlexibleNode)getExternalNode(taxonIndex)).getTaxon();
	}

	/**
	 * @return the ID of the taxon of the ith external node. If it doesn't have
	 * a taxon, returns the ID of the node itself.
	 */
	public String getTaxonId(int taxonIndex) {
		Taxon taxon = getTaxon(taxonIndex);
		if (taxon != null)
			return taxon.getId();
		else
			return ((FlexibleNode)getExternalNode(taxonIndex)).getId();
	}

	/**
	 * returns the index of the taxon with the given id.
	 */
	public int getTaxonIndex(String id) {
		for (int i = 0, n = getTaxonCount(); i < n; i++) {
			if (getTaxonId(i).equals(id)) return i;
		}
		return -1;
	}

	/**
	 * returns the index of the given taxon.
	 */
	public int getTaxonIndex(Taxon taxon) {
		for (int i = 0, n = getTaxonCount(); i < n; i++) {
			if (getTaxon(i) == taxon) return i;
		}
		return -1;
	}

	/**
	 * @return an object representing the named attributed for the taxon of the given
	 * external node. If the node doesn't have a taxon then the nodes own attribute
	 * is returned.
	 * @param taxonIndex the index of the taxon whose attribute is being fetched.
	 * @param name the name of the attribute of interest.
	 */
	public Object getTaxonAttribute(int taxonIndex, String name) {
		Taxon taxon = getTaxon(taxonIndex);
		if (taxon != null)
			return taxon.getAttribute(name);
		else
			return ((FlexibleNode)getExternalNode(taxonIndex)).getAttribute(name);
	}

    // **************************************************************
    // MutableTaxonList IMPLEMENTATION
    // **************************************************************

	public int addTaxon(Taxon taxon) { throw new IllegalArgumentException("Cannot add taxon to a MutableTree"); }
	public boolean removeTaxon(Taxon taxon) { throw new IllegalArgumentException("Cannot add taxon to a MutableTree"); }

	/**
	 * Sets the ID of the taxon of the ith external node. If it doesn't have
	 * a taxon, sets the ID of the node itself.
	 */
	public void setTaxonId(int taxonIndex, String id) {
		Taxon taxon = getTaxon(taxonIndex);
		if (taxon != null)
			taxon.setId(id);
		else
			((FlexibleNode)getExternalNode(taxonIndex)).setId(id);

		fireTreeChanged();
		fireTaxaChanged();
	}

	/**
	 * Sets an named attribute for the taxon of a given external node. If the node
	 * doesn't have a taxon then the attribute is added to the node itself.
	 * @param taxonIndex the index of the taxon whose attribute is being set.
	 * @param name the name of the attribute.
	 * @param value the new value of the attribute.
	 */
	public void setTaxonAttribute(int taxonIndex, String name, Object value) {
		Taxon taxon = getTaxon(taxonIndex);
		if (taxon != null)
			taxon.setAttribute(name, value);
		else
			((FlexibleNode)getExternalNode(taxonIndex)).setAttribute(name, value);

		fireTreeChanged();
		fireTaxaChanged();
	}

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

	protected String id = null;

	/**
	 * @return the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 */
	public void setId(String id) {
		this.id = id;

		fireTreeChanged();
	}

    // **************************************************************
    // Attributable IMPLEMENTATION
    // **************************************************************

	private Attributable.AttributeHelper attributes = null;

	/**
	 * Sets an named attribute for this object.
	 * @param name the name of the attribute.
	 * @param value the new value of the attribute.
	 */
	public void setAttribute(String name, Object value) {
		if (attributes == null)
			attributes = new Attributable.AttributeHelper();
		attributes.setAttribute(name, value);

		fireTreeChanged();
	}

	/**
	 * @return an object representing the named attributed for this object.
	 * @param name the name of the attribute of interest.
	 */
	public Object getAttribute(String name) {
		if (attributes == null)
			return null;
		else
			return attributes.getAttribute(name);
	}

	/**
	 * @return an iterator of the attributes that this object has.
	 */
	public Iterator getAttributeNames() {
		if (attributes == null)
			return null;
		else
			return attributes.getAttributeNames();
	}

	public void addMutableTreeListener(MutableTreeListener listener) {
		mutableTreeListeners.add(listener);
	}

	private void fireTreeChanged() {
		for (int i = 0; i < mutableTreeListeners.size(); i++) {
			((MutableTreeListener)mutableTreeListeners.get(i)).treeChanged(this);
		}
	}

	private ArrayList mutableTreeListeners = new ArrayList();

	public void addMutableTaxonListListener(MutableTaxonListListener listener) {
		mutableTaxonListListeners.add(listener);
	}

	private void fireTaxaChanged() {
		for (int i = 0; i < mutableTaxonListListeners.size(); i++) {
			((MutableTaxonListListener)mutableTaxonListListeners.get(i)).taxaChanged(this);
		}
	}

	private ArrayList mutableTaxonListListeners = new ArrayList();

	/**
	 * @return a string containing a newick representation of the tree
	 */
	public String toString() {
		return Tree.Utils.newick(this);
	}

	/**
	 * @return whether two trees have the same topology
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof Tree)) {
			throw new IllegalArgumentException("FlexibleTree.equals can only compare instances of Tree");
		}
		return Tree.Utils.equal(this, (Tree)obj);
	}

    // **************************************************************
    // Private Stuff
    // **************************************************************

	/** root node */
	FlexibleNode root;

	/** list of internal nodes (including root) */
	FlexibleNode[] nodes = null;

	/** number of nodes (including root and tips) */
	int nodeCount;

	/** number of external nodes */
	int externalNodeCount;

	/** number of internal nodes (including root) */
	int internalNodeCount;

	/** holds the units of the trees branches. */
	private int units = SUBSTITUTIONS;

	boolean inEdit = false;

	boolean heightsKnown = false;
	boolean lengthsKnown = false;
}
