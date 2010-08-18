/*
 * LoggerParser.java
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

package dr.inferencexml.loggers;

import dr.app.beast.BeastVersion;
import dr.inference.loggers.*;
import dr.math.MathUtils;
import dr.util.FileHelpers;
import dr.util.Identifiable;
import dr.util.Property;
import dr.xml.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class LoggerParser extends AbstractXMLObjectParser {

    public static final String LOG = "log";
    public static final String ECHO = "echo";
    public static final String ECHO_EVERY = "echoEvery";
    public static final String TITLE = "title";
    public static final String FILE_NAME = FileHelpers.FILE_NAME;
    public static final String FORMAT = "format";
    public static final String TAB = "tab";
    public static final String HTML = "html";
    public static final String PRETTY = "pretty";
    public static final String LOG_EVERY = "logEvery";
    public static final String ALLOW_OVERWRITE_LOG = "allowOverwrite";

    public static final String COLUMNS = "columns";
    public static final String COLUMN = "column";
    public static final String LABEL = "label";
    public static final String SIGNIFICANT_FIGURES = "sf";
    public static final String DECIMAL_PLACES = "dp";
    public static final String WIDTH = "width";

    public String getParserName() {
        return LOG;
    }

    /**
     * @return an object based on the XML element it was passed.
     */
    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        // You must say how often you want to log
        final int logEvery = xo.getIntegerAttribute(LOG_EVERY);

        String fileName = null;
        if (xo.hasAttribute(FILE_NAME)) {
            fileName = xo.getStringAttribute(FILE_NAME);
        }

        boolean allowOverwrite = false; // default to not allow to overwrite
        if (xo.hasAttribute(ALLOW_OVERWRITE_LOG)) allowOverwrite = xo.getBooleanAttribute(ALLOW_OVERWRITE_LOG);

        if (fileName!= null && (!allowOverwrite)) {
             File f = new File(fileName);
            if (f.exists()) {
                throw new XMLParseException("\nThe log file " + fileName + " already exists in the working directory." +
                        "\nYou cannot overwrite it, unless adding an attribute " + ALLOW_OVERWRITE_LOG + "=\"true\" in "
                        + LOG + " element in xml.\nFor example: <" + LOG + " ... fileName=\"" + fileName + "\" "
                        + ALLOW_OVERWRITE_LOG + "=\"true\">");
            }
        }

        final PrintWriter pw = XMLParser.getFilePrintWriter(xo, getParserName());

        final LogFormatter formatter = new TabDelimitedFormatter(pw);

        boolean performanceReport = false;

        if (!xo.hasAttribute(FILE_NAME)) {
            // is a screen log
            performanceReport = true;
        }

        // added a performance measurement delay to avoid the full evaluation period.
        final MCLogger logger = new MCLogger(fileName, formatter, logEvery, performanceReport, 10000);

        if (xo.hasAttribute(TITLE)) {
            logger.setTitle(xo.getStringAttribute(TITLE));
        } else {

            final BeastVersion version = new BeastVersion();

            final String title = "BEAST " + version.getVersionString() +
                    ", " + version.getBuildString() + "\n" +

                    "Generated " + (new Date()).toString() + " [seed=" + MathUtils.getSeed() + "]";
            logger.setTitle(title);
        }

        for (int i = 0; i < xo.getChildCount(); i++) {

            final Object child = xo.getChild(i);

            if (child instanceof Columns) {

                logger.addColumns(((Columns) child).getColumns());

            } else if (child instanceof Loggable) {

                logger.add((Loggable) child);

            } else if (child instanceof Identifiable) {

                logger.addColumn(new LogColumn.Default(((Identifiable) child).getId(), child));

            } else if (child instanceof Property) {
                logger.addColumn(new LogColumn.Default(((Property) child).getAttributeName(), child));
            } else {

                logger.addColumn(new LogColumn.Default(child.getClass().toString(), child));
            }
        }

        return logger;
    }

    public static PrintWriter getLogFile(XMLObject xo, String parserName) throws XMLParseException {
        return XMLParser.getFilePrintWriter(xo, parserName);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            AttributeRule.newIntegerRule(LOG_EVERY),
            new StringAttributeRule(FILE_NAME,
                    "The name of the file to send log output to. " +
                            "If no file name is specified then log is sent to standard output", true),
            new StringAttributeRule(TITLE,
                    "The title of the log", true),
            new OrRule(
                    new XMLSyntaxRule[]{
                            new ElementRule(Columns.class, 1, Integer.MAX_VALUE),
                            new ElementRule(Loggable.class, 1, Integer.MAX_VALUE),
                            new ElementRule(Object.class, 1, Integer.MAX_VALUE)
                    }
            )
    };

    public String getParserDescription() {
        return "Logs one or more items at a given frequency to the screen or to a file";
    }

    public Class getReturnType() {
        return MLLogger.class;
    }
}

