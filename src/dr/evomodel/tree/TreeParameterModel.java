/*
 * TreeParameterModel.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.tree;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeTrait;
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.inference.model.Variable;

/**
 * This class maintains a parameter of length equal to the number of nodes in the tree.
 * It can optionally include the root node. If the root node is not included then this
 * class will track tree changes that change the root node number and swap the parameter
 * values so that the parameter values stay with their node when changes to the tree
 * topology occur.
 *
 * @author Alexei Drummond
 */
public class TreeParameterModel extends AbstractModel implements TreeTrait<Double> {

    private final TreeModel tree;

    // The tree parameter;
    private final Parameter parameter;

    // the index of the root node.
    private int rootNodeNumber;
    private int storedRootNodeNumber;

    private boolean includeRoot = false;

    /**
     * This class constructs a tree parameter, and will set the dimension of the parameter
     * to match the appropriate number of nodes if necessary.
     *
     * @param tree        the tree that this parameter corresponds to
     * @param parameter   the parameter to keep in sync with tree topology moves.
     * @param includeRoot tree if the parameter includes a value associated with the root node.
     */
    public TreeParameterModel(TreeModel tree, Parameter parameter, boolean includeRoot) {

        super("treeParameterModel");
        this.tree = tree;
        this.parameter = parameter;

        this.includeRoot = includeRoot;

        int dim = parameter.getDimension();
        int treeSize = tree.getNodeCount();
        if (!includeRoot) treeSize -= 1;
        if (dim != treeSize) {
            System.err.println("WARNING: setting dimension of parameter to match tree branch count ("
                    + dim + " != " + treeSize + ")");
            parameter.setDimension(treeSize);
        }

        addModel(tree);
        addVariable(parameter);

        rootNodeNumber = tree.getRoot().getNumber();
        storedRootNodeNumber = rootNodeNumber;
    }

    public void handleModelChangedEvent(Model model, Object object, int index) {
        if (model == tree) {
            handleRootMove();
        }
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        int nodeNumber = getNodeNumberFromParameterIndex(index);

        NodeRef node = tree.getNode(nodeNumber);
        assert (node.getNumber() == nodeNumber);

        fireModelChanged(this, nodeNumber);
    }

    protected void storeState() {
        storedRootNodeNumber = rootNodeNumber;
    }

    protected void restoreState() {
        rootNodeNumber = storedRootNodeNumber;
    }

    protected void acceptState() {
    }

    public double getNodeValue(Tree tree, NodeRef node) {

        assert (!tree.isRoot(node) && !includeRoot) : "root node doesn't have a parameter value!";

        assert tree.getRoot().getNumber() == rootNodeNumber :
                "INTERNAL ERROR! node with number " + rootNodeNumber + " should be the root node.";

        int nodeNumber = node.getNumber();
        int index = getParameterIndexFromNodeNumber(nodeNumber);
        return parameter.getParameterValue(index);
    }

    public void setNodeValue(Tree tree, NodeRef node, double value) {

        assert (!tree.isRoot(node) && !includeRoot) : "root node doesn't have a parameter value!";

        assert tree.getRoot().getNumber() == rootNodeNumber :
                "INTERNAL ERROR! node with number " + rootNodeNumber + " should be the root node.";

        int nodeNumber = node.getNumber();
        int index = getParameterIndexFromNodeNumber(nodeNumber);
        parameter.setParameterValue(index, value);
    }

    public String getBranchAttributeLabel() {
        return parameter.getId();
    }

    public String getAttributeForBranch(Tree tree, NodeRef node) {
        return Double.toString(getNodeValue(tree, node));
    }

    private int getNodeNumberFromParameterIndex(int parameterIndex) {
        if (!includeRoot && parameterIndex >= tree.getRoot().getNumber()) return parameterIndex + 1;
        return parameterIndex;
    }

    private int getParameterIndexFromNodeNumber(int nodeNumber) {
        if (!includeRoot && nodeNumber > tree.getRoot().getNumber()) return nodeNumber - 1;
        return nodeNumber;
    }

    private void handleRootMove() {

        if (!includeRoot) {

            final int newRootNodeNumber = tree.getRoot().getNumber();

            if (rootNodeNumber > newRootNodeNumber) {

                final double oldValue = parameter.getParameterValue(newRootNodeNumber);

                final int end = Math.min(parameter.getDimension() - 1, rootNodeNumber);
                for (int i = newRootNodeNumber; i < end; i++) {
                    parameter.setParameterValue(i, parameter.getParameterValue(i + 1));
                }

                parameter.setParameterValue(end, oldValue);

            } else if (rootNodeNumber < newRootNodeNumber) {

                final int end = Math.min(parameter.getDimension() - 1, newRootNodeNumber);

                final double oldValue = parameter.getParameterValue(end);

                for (int i = end; i > rootNodeNumber; i--) {
                    parameter.setParameterValue(i, parameter.getParameterValue(i - 1));
                }

                parameter.setParameterValue(rootNodeNumber, oldValue);
            }
            rootNodeNumber = newRootNodeNumber;
        }
    }

    /**
     * @return the tree model that this parameter is synchronized with
     */
    public TreeModel getTreeModel() {
        return tree;
    }

    public String[] getNodeAttributeLabel() {
        return new String[]{parameter.getId()};
    }

    public String[] getAttributeForNode(Tree tree, NodeRef node) {
        return new String[]{getAttributeForBranch(tree, node)};
    }

    public String getTraitName() {
        return null;
    }

    public Intent getIntent() {
        return null;
    }

    public Class getTraitClass() {
        return null;
    }

    public int getDimension() {
        return 0;
    }

    public Double[] getTrait(Tree tree, NodeRef node) {
        return new Double[0];
    }

    public String[] getTraitString(Tree tree, NodeRef node) {
        return new String[0];
    }
}
