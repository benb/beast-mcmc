/*
 * NewickParser.java
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

package dr.evoxml;

import dr.evolution.io.Importer;
import dr.evolution.io.NewickImporter;
import dr.evolution.tree.*;
import dr.evolution.util.Taxon;
import dr.evolution.util.TimeScale;
import dr.evolution.util.Units;
import dr.xml.*;

import java.io.IOException;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: NewickParser.java,v 1.7 2006/04/25 14:41:08 rambaut Exp $
 */
public class NewickParser extends AbstractXMLObjectParser {

    public static final String NEWICK = "newick";
    public static final String UNITS = "units";
    public static final String RESCALE_HEIGHT = "rescaleHeight";

    public String getParserName() { return NEWICK; }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        int units = Units.SUBSTITUTIONS;
        units = XMLParser.Utils.getUnitsAttr(xo);

        boolean usingDates = true;

        if (xo.hasAttribute(SimpleTreeParser.USING_DATES)) {
            usingDates = xo.getBooleanAttribute(SimpleTreeParser.USING_DATES);
        }

        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < xo.getChildCount(); i++) {
            if (xo.getChild(i) instanceof String) {
                buffer.append((String)xo.getChild(i));
            } else {
                throw new XMLParseException("illegal element in newick element");
            }
        }

        java.io.Reader reader = new java.io.StringReader(buffer.toString());
        NewickImporter importer = new NewickImporter(reader);

        FlexibleTree tree = null;

        try {
            tree = (FlexibleTree)importer.importTree(null);
        } catch (IOException ioe) {
            throw new XMLParseException("error parsing tree in newick element");
        } catch (NewickImporter.BranchMissingException bme) {
            throw new XMLParseException("branch missing in tree in newick element");
        } catch (Importer.ImportException ime) {
            throw new XMLParseException("error parsing tree in newick element");
        }

        tree.setUnits(units);

        for (int i = 0; i < tree.getTaxonCount(); i++) {

            FlexibleNode node = (FlexibleNode)tree.getExternalNode(i);

            String id = node.getTaxon().getId();
            Taxon taxon = null;

            try {
                Object obj = getStore().getObjectById(id);

                if (obj instanceof Taxon) {

                    taxon = (Taxon)obj;
                }
            } catch (ObjectNotFoundException e) { }

            if (taxon != null) {

                node.setTaxon(taxon);

            } else {
                throw new XMLParseException("unknown taxon, " + id + ", in newick tree");
            }
        }

        if (usingDates) {

            dr.evolution.util.Date mostRecent = null;
            for (int i = 0; i < tree.getTaxonCount(); i++) {

                dr.evolution.util.Date date = (dr.evolution.util.Date)tree.getTaxonAttribute(i, dr.evolution.util.Date.DATE);

                if (date == null) {
                    date = (dr.evolution.util.Date)tree.getNodeAttribute(tree.getExternalNode(i), dr.evolution.util.Date.DATE);
                }

                if (date != null && ((mostRecent == null) || date.after(mostRecent))) {
                    mostRecent = date;
                }
            }

            for (int i = 0; i < tree.getInternalNodeCount(); i++) {
                dr.evolution.util.Date date = (dr.evolution.util.Date)tree.getNodeAttribute(tree.getInternalNode(i), dr.evolution.util.Date.DATE);

                if (date != null && ((mostRecent == null) || date.after(mostRecent))) {
                    mostRecent = date;
                }
            }

            if (mostRecent == null) {
                mostRecent = dr.evolution.util.Date.createRelativeAge(0.0, units);
            }

            TimeScale timeScale = new TimeScale(mostRecent.getUnits(), true, mostRecent.getAbsoluteTimeValue());

            for (int i = 0; i < tree.getTaxonCount(); i++) {
                dr.evolution.util.Date date = (dr.evolution.util.Date)tree.getTaxonAttribute(i, dr.evolution.util.Date.DATE);

                if (date == null) {
                    date = (dr.evolution.util.Date)tree.getNodeAttribute(tree.getExternalNode(i), dr.evolution.util.Date.DATE);
                }

                if (date != null) {
                    double height = timeScale.convertTime(date.getTimeValue(), date);
                    tree.setNodeHeight(tree.getExternalNode(i), height);
                } else {
                    tree.setNodeHeight(tree.getExternalNode(i), 0.0);
                }
            }

            for (int i = 0; i < tree.getInternalNodeCount(); i++) {
                dr.evolution.util.Date date = (dr.evolution.util.Date)tree.getNodeAttribute(tree.getInternalNode(i), dr.evolution.util.Date.DATE);

                if (date != null) {
                    double height = timeScale.convertTime(date.getTimeValue(), date);
                    tree.setNodeHeight(tree.getInternalNode(i), height);
                }
            }



            MutableTree.Utils.correctHeightsForTips(tree);
        }

        if (xo.hasAttribute(RESCALE_HEIGHT)) {
            double rescaleHeight = xo.getDoubleAttribute(RESCALE_HEIGHT);
            double scale = rescaleHeight / tree.getNodeHeight(tree.getRoot());
            for (int i = 0; i < tree.getInternalNodeCount(); i++) {
                NodeRef n = tree.getInternalNode(i);
                tree.setNodeHeight(n, tree.getNodeHeight(n) * scale);
            }
        }

        //System.out.println("Constructed newick tree = " + Tree.Utils.uniqueNewick(tree, tree.getRoot()));
        return tree;
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "Constructs a tree from a NEWICK format tree description";
    }

    public String getExample() {
        return "<" + getParserName() + " " + UNITS + "=\"" + Units.Utils.getDefaultUnitName(Units.YEARS) + "\">" + " ((A:1.0, B:1.0):1.0,(C:2.0, D:2.0):1.0); </" + getParserName() + ">";
    }

    public XMLSyntaxRule[] getSyntaxRules() { return rules; }

    private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
            AttributeRule.newBooleanRule(SimpleTreeParser.USING_DATES, true),
            AttributeRule.newDoubleRule(RESCALE_HEIGHT, true, "Attempt to rescale the tree to the given root height"),
            new StringAttributeRule(UNITS, "The branch length units of this tree", Units.UNIT_NAMES, true),
            new ElementRule(String.class, "The NEWICK format tree. Tip labels are taken to be Taxon IDs")
    };

    public Class getReturnType() { return Tree.class; }
    

}
