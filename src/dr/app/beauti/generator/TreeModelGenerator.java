/*
 * TreeModelGenerator.java
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

package dr.app.beauti.generator;

import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.ClockType;
import dr.app.beauti.options.PartitionTreeModel;
import dr.app.beauti.util.XMLWriter;
import dr.evomodel.coalescent.CoalescentSimulator;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.TreeModelParser;
import dr.evoxml.UPGMATreeParser;
import dr.inference.model.ParameterParser;
import dr.util.Attribute;
import dr.xml.XMLParser;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class TreeModelGenerator extends Generator {

    public TreeModelGenerator(BeautiOptions options, ComponentFactory[] components) {
        super(options, components);
    }

    /**
     * Write tree model XML block.
     *
     * @param model
     * @param writer the writer
     */
    void writeTreeModel(PartitionTreeModel model, XMLWriter writer) {

        setModelPrefix(model.getPrefix());

        final String treeModelName = modelPrefix + TreeModel.TREE_MODEL; // treemodel.treeModel or treeModel

        writer.writeComment("Generate a tree model");
        writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>(XMLParser.ID, treeModelName), false);

        final String STARTING_TREE = InitialTreeGenerator.STARTING_TREE;

        switch (model.getStartingTreeType()) {
            case USER:
                writer.writeTag("tree", new Attribute.Default<String>(XMLParser.IDREF, modelPrefix + STARTING_TREE), true);
                break;
            case UPGMA:
                writer.writeTag(UPGMATreeParser.UPGMA_TREE,
                        new Attribute.Default<String>(XMLParser.IDREF, modelPrefix + STARTING_TREE), true);
                break;
            case RANDOM:
                writer.writeTag(CoalescentSimulator.COALESCENT_TREE,
                        new Attribute.Default<String>(XMLParser.IDREF, modelPrefix + STARTING_TREE), true);
                break;
            default:
                throw new IllegalArgumentException("Unknown StartingTreeType");
        }

        writer.writeOpenTag(TreeModelParser.ROOT_HEIGHT);
        writer.writeTag(ParameterParser.PARAMETER,
                new Attribute.Default<String>(XMLParser.ID, treeModelName + "." + CoalescentSimulator.ROOT_HEIGHT), true);
        writer.writeCloseTag(TreeModelParser.ROOT_HEIGHT);


        writer.writeOpenTag(TreeModelParser.NODE_HEIGHTS, new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "true"));
        writer.writeTag(ParameterParser.PARAMETER,
                new Attribute.Default<String>(XMLParser.ID, treeModelName + "." + "internalNodeHeights"), true);
        writer.writeCloseTag(TreeModelParser.NODE_HEIGHTS);

        writer.writeOpenTag(TreeModelParser.NODE_HEIGHTS,
                new Attribute[]{
                        new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "true"),
                        new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "true")
                });
        writer.writeTag(ParameterParser.PARAMETER,
                new Attribute.Default<String>(XMLParser.ID, treeModelName + "." + "allInternalNodeHeights"), true);
        writer.writeCloseTag(TreeModelParser.NODE_HEIGHTS);

//        int randomLocalClockCount = 0;
//        int autocorrelatedClockCount = 0;
//        for (PartitionData pd : model.getAllPartitionData()) { // only the PDs linked to this tree model        
//        	PartitionClockModel clockModel = pd.getPartitionClockModel();
//        	switch (clockModel.getClockType()) {
//	        	case AUTOCORRELATED_LOGNORMAL: autocorrelatedClockCount += 1; break;
//	        	case RANDOM_LOCAL_CLOCK: randomLocalClockCount += 1; break;
//        	}
//        }
//        
//        if (autocorrelatedClockCount > 1 || randomLocalClockCount > 1 || autocorrelatedClockCount + randomLocalClockCount > 1) {
//        	//FAIL
//            throw new IllegalArgumentException("clock model/tree model combination not implemented by BEAST yet!");
//        } 
        // move to validateClockTreeModelCombination(PartitionTreeModel model)

        int[] count = validateClockTreeModelCombination(model);

//    	if (autocorrelatedClockCount == 1) {
//        if (count[0] == 1) {
//                writer.writeOpenTag(TreeModelParser.NODE_RATES,
//                        new Attribute[]{
//                                new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "false"),
//                                new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "true"),
//                                new Attribute.Default<String>(TreeModelParser.LEAF_NODES, "true")
//                        });
//                writer.writeTag(ParameterParser.PARAMETER,
//                        new Attribute.Default<String>(XMLParser.ID, treeModelName + "." + TreeModelParser.NODE_RATES), true);
//                writer.writeCloseTag(TreeModelParser.NODE_RATES);
//
//                writer.writeOpenTag(TreeModelParser.NODE_RATES,
//                        new Attribute[]{
//                                new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "true"),
//                                new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "false"),
//                                new Attribute.Default<String>(TreeModelParser.LEAF_NODES, "false")
//                        });
//                writer.writeTag(ParameterParser.PARAMETER,
//                        new Attribute.Default<String>(XMLParser.ID,
//                                treeModelName + "." + RateEvolutionLikelihood.ROOTRATE), true);
//                writer.writeCloseTag(TreeModelParser.NODE_RATES);
////    	} else if (randomLocalClockCount == 1 ) {
//        } else 
        if (count[1] == 1) {
            writer.writeOpenTag(TreeModelParser.NODE_RATES,
                    new Attribute[]{
                            new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "false"),
                            new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "true"),
                            new Attribute.Default<String>(TreeModelParser.LEAF_NODES, "true")
                    });
            writer.writeTag(ParameterParser.PARAMETER,
                    new Attribute.Default<String>(XMLParser.ID, modelPrefix + ClockType.LOCAL_CLOCK + "." + "rates"), true);
            writer.writeCloseTag(TreeModelParser.NODE_RATES);

            writer.writeOpenTag(TreeModelParser.NODE_TRAITS,
                    new Attribute[]{
                            new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "false"),
                            new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "true"),
                            new Attribute.Default<String>(TreeModelParser.LEAF_NODES, "true")
                    });
            writer.writeTag(ParameterParser.PARAMETER,
                    new Attribute.Default<String>(XMLParser.ID, modelPrefix + ClockType.LOCAL_CLOCK + "." + "changes"), true);
            writer.writeCloseTag(TreeModelParser.NODE_TRAITS);
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_TREE_MODEL, writer);

        writer.writeCloseTag(TreeModel.TREE_MODEL);

//        if (autocorrelatedClockCount == 1) {
//        if (count[0] == 1) {
//            writer.writeText("");
//            writer.writeOpenTag(CompoundParameter.COMPOUND_PARAMETER,
//                    new Attribute[]{new Attribute.Default<String>(XMLParser.ID, treeModelName + "." + "allRates")});
//            writer.writeTag(ParameterParser.PARAMETER,
//                    new Attribute.Default<String>(XMLParser.IDREF, treeModelName + "." + TreeModelParser.NODE_RATES), true);
//            writer.writeTag(ParameterParser.PARAMETER,
//                    new Attribute.Default<String>(XMLParser.IDREF, treeModelName + "." + RateEvolutionLikelihood.ROOTRATE), true);
//            writer.writeCloseTag(CompoundParameter.COMPOUND_PARAMETER);
//        }
    }
}
