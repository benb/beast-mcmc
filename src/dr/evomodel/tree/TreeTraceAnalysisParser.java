/*
 * TreeTraceAnalysisParser.java
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

package dr.evomodel.tree;

import dr.evolution.io.Importer;
import dr.evolution.io.NewickImporter;
import dr.evolution.tree.Tree;
import dr.xml.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

/**
 * @author Alexei Drummond
 * @version $Id: TreeTraceAnalysisParser.java,v 1.6 2005/05/24 20:25:58 rambaut Exp $
 */
public class TreeTraceAnalysisParser extends AbstractXMLObjectParser {

    public final static String TREE_TRACE_ANALYSIS = "treeTraceAnalysis";
    public final static String BURN_IN = "burnIn";
    public final static String MIN_CLADE_PROBABILITY = "minCladeProbability";
    public final static String CRED_SET_PROBABILITY = "credSetProbability";
    public static final String FILE_NAME = "fileName";

    public final static String REFERENCE_TREE = "referenceTree";
    public final static String SHORT_REPORT = "shortReport";

    public String getParserName() {
        return TREE_TRACE_ANALYSIS;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        try {
            Reader reader;

            String fileName = xo.getStringAttribute(FILE_NAME);
            String name;
            try {
                File file = new File(fileName);
                name = file.getName();
                String parent = file.getParent();

                if (!file.isAbsolute()) {
                    parent = System.getProperty("user.dir");
                }

//					System.out.println("Writing log file to "+parent+System.getProperty("path.separator")+name);
                reader = new FileReader(new File(parent, name));
            } catch (FileNotFoundException fnfe) {
                throw new XMLParseException("File '" + fileName + "' can not be opened for " + getParserName() + " element.");
            }

            int burnin = -1;
            if (xo.hasAttribute(BURN_IN)) {
                // leaving the burnin attribute off will result in 10% being used
                burnin = xo.getIntegerAttribute(BURN_IN);
            }

            double minCladeProbability = 0.5;
            if (xo.hasAttribute(MIN_CLADE_PROBABILITY)) {
                // leaving the burnin attribute off will result in 10% being used
                minCladeProbability = xo.getDoubleAttribute(MIN_CLADE_PROBABILITY);
            }

            double credSetProbability = 0.95;
            if (xo.hasAttribute(CRED_SET_PROBABILITY)) {
                // leaving the burnin attribute off will result in 10% being used
                credSetProbability = xo.getDoubleAttribute(CRED_SET_PROBABILITY);
            }


            Tree referenceTree = null;
            Reader refReader;
            if (xo.hasAttribute(REFERENCE_TREE)) {
                String referenceName = xo.getStringAttribute(REFERENCE_TREE);

                try {
                    File refFile = new File(referenceName);
                    String refName = refFile.getName();
                    String parent = refFile.getParent();

                    if (!refFile.isAbsolute()) {
                        parent = System.getProperty("user.dir");
                    }
                    refReader = new FileReader(new File(parent, refName));
                } catch (FileNotFoundException fnfe) {
                    throw new XMLParseException("File '" + fileName + "' can not be opened for " + getParserName() + " element.");
                }

                try {
                    NewickImporter importTree = new NewickImporter(refReader);
                    if (importTree.hasTree()) {
                        referenceTree = importTree.importNextTree();
                    }
                } catch (Importer.ImportException iee) {
                    throw new XMLParseException("Reference file '" + referenceName + "' is empty.");
                }
            }

            boolean shortReport = xo.getAttribute(SHORT_REPORT, false);

            TreeTraceAnalysis analysis = TreeTraceAnalysis.analyzeLogFile(new Reader[]{reader}, burnin, true);

            if (shortReport) {
                analysis.shortReport(name, referenceTree, true, minCladeProbability, credSetProbability);
            } else {
                analysis.report(minCladeProbability, credSetProbability);
            }

            System.out.println();
            System.out.flush();

            return analysis;
        } catch (java.io.IOException ioe) {
            throw new XMLParseException(ioe.getMessage());
        }
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "Analyses and reports on a trace consisting of trees.";
    }

    public Class getReturnType() {
        return TreeTraceAnalysis.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
            new StringAttributeRule(FILE_NAME, "name of a tree log file", "trees.log"),
            AttributeRule.newIntegerRule(BURN_IN, true),
            AttributeRule.newDoubleRule(MIN_CLADE_PROBABILITY, true),
            AttributeRule.newDoubleRule(CRED_SET_PROBABILITY, true),
            AttributeRule.newBooleanRule(SHORT_REPORT, true),
            AttributeRule.newStringRule(REFERENCE_TREE, true)
    };
}