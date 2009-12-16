/*
 * BeastParserDoc.java
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

package dr.app.tools;

import dr.app.util.Arguments;
import dr.app.beast.BeastParser;
import dr.app.beast.BeastVersion;
import dr.xml.*;
import dr.util.Version;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;


public class BeastParserDoc {

    private final static Version version = new BeastVersion();

    public final static String INDEX_HTML = "index_" + version.getVersionString() + ".html";
    public final static String DETAIL_HTML = "detail_" + version.getVersionString() + ".html";
    public final static String INDEX_WIKI = "index_" + version.getVersionString() + ".wiki";
    public final static String DETAIL_WIKI = "detail_" + version.getVersionString() + ".wiki";


    public final static String TITTLE = "BEAST " + version.getVersionString() + " Parser Library ("
            + version.getDateString() + ")";
    public final static String AUTHORS = "Alexei Drummond, Andrew Rambaut, Walter Xie";
    public final static String LINK1 = "http://beast.bio.ed.ac.uk/";
    public final static String LINK2 = "http://code.google.com/p/beast-mcmc/";

    public BeastParserDoc(BeastParser parser, String directory, boolean wikiFormat) throws java.io.IOException {

        try {
            // Create multiple directories
            boolean success = (new File(directory)).mkdirs();
            if (success) {
                System.out.println("Directories: " + directory + " created");
            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

//
//        File file = new File(directory);
//
//        if (!file.exists()) {
//            file.mkdir();
//        }
//
//        if (!file.isDirectory()) {
//            throw new IllegalArgumentException(directory + " is not a directory!");
//        }
        PrintWriter writer;
        if (wikiFormat) {
            XMLDocumentationHandler handler = new WikiDocumentationHandler(parser);

//            writer = new PrintWriter(new FileWriter(new File(directory, INDEX_WIKI)));
//            System.out.println("Building types table in " + INDEX_WIKI + " ...");
//
//            handler.outputIndex(writer);
//            System.out.println("done.");
//            writer.flush();
//            writer.close();

            writer = new PrintWriter(new FileWriter(new File(directory, DETAIL_WIKI)));
            System.out.println("Building element descriptions in " + DETAIL_WIKI + " ...");

            handler.outputElements(writer);
            System.out.println("done.");
            writer.flush();
            writer.close();

        } else {
            XMLDocumentationHandler handler = new XMLDocumentationHandler(parser);

            System.out.println("Generate " + INDEX_HTML + " ...");
            writer = new PrintWriter(new FileWriter(new File(directory, INDEX_HTML)));

            handler.outputIndex(writer); // generate index.html
            System.out.println("done.");
            writer.flush();
            writer.close();

            System.out.println("Generate " + DETAIL_HTML + " ...");
            writer = new PrintWriter(new FileWriter(new File(directory, DETAIL_HTML)));

            handler.outputElements(writer);
            System.out.println("done.");

            writer.flush();
            writer.close();
        }
    }

//	private final void setup() throws XMLParseException {
    // add all the XMLObject parsers you need
//	}

    public static void printTitle() {

        System.out.println("+-----------------------------------------------\\");
        System.out.print("|");
        int n = 47 - TITTLE.length();
        int n1 = n / 2;
        int n2 = n1 + (n % 2);
        for (int i = 0; i < n1; i++) { System.out.print(" "); }
        System.out.print(TITTLE);
        for (int i = 0; i < n2; i++) { System.out.print(" "); }
        System.out.println("||");
        System.out.println("|   " + AUTHORS + " ||");
        System.out.println("|           " + LINK1 + "          ||");
        System.out.println("|      " + LINK2 + "     ||");
        System.out.println("\\-----------------------------------------------\\|");
        System.out.println(" \\-----------------------------------------------\\");
        System.out.println();
    }

    public static void printUsage(Arguments arguments) {

        arguments.printUsage("beastdoc", "<output-directory>");
        System.out.println();
        System.out.println("  Example: beastdoc ./doc");
        System.out.println();

    }

    //Main method
    public static void main(String[] args) throws java.io.IOException, XMLParseException {

        printTitle();

        Arguments arguments = new Arguments(
                new Arguments.Option[] {
                        new Arguments.Option("help", "option to print this message")
                });

        try {
            arguments.parseArguments(args);
        } catch (Arguments.ArgumentException ae) {
            System.out.println(ae);
            printUsage(arguments);
            return;
        }

        if (arguments.hasOption("help")) {
            printUsage(arguments);
            return;
        }

        String outputDirectory = null;

        String[] args2 = arguments.getLeftoverArguments();

        if (args2.length > 1) {
            System.err.println("Unknown option: " + args2[1]);
            System.err.println();
            printUsage(arguments);
            return;
        }

        if (args2.length > 0) {
            outputDirectory = args2[0];
        }

        if (outputDirectory == null) {
            // No input file name was given so throw up a dialog box...
            outputDirectory = System.getProperty("user.dir") + "\\release\\common\\doc\\BEASTParserLibrary";
        }

        // BeastParserDoc(BeastParser parser, String directory, boolean wikiFormat)
        new BeastParserDoc(new BeastParser(new String[] {}, null, false, false, false), outputDirectory, true);

        System.exit(0);
    }
}

