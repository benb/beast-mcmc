/*
 * NexusApplicationImporter.java
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

package dr.app.beauti;

import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.NucModelType;
import dr.app.beauti.options.PartitionModel;
import dr.evolution.datatype.Nucleotides;
import dr.evolution.io.NexusImporter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for importing PAUP, MrBayes and Rhino NEXUS file format
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: NexusApplicationImporter.java,v 1.4 2005/07/11 14:07:25 rambaut Exp $
 */
public class NexusApplicationImporter extends NexusImporter {

    public static final NexusBlock ASSUMPTIONS_BLOCK = new NexusBlock("ASSUMPTIONS");
    public static final NexusBlock PAUP_BLOCK = new NexusBlock("PAUP");
    public static final NexusBlock MRBAYES_BLOCK = new NexusBlock("MRBAYES");

    /**
     * Constructor
     */
    public NexusApplicationImporter(Reader reader) {
        super(reader);
        setCommentDelimiters('[', ']', '\0');
    }

    public NexusApplicationImporter(Reader reader, Writer commentWriter) {
        super(reader, commentWriter);
        setCommentDelimiters('[', ']', '\0');
    }

    /**
     * This function returns an enum class to specify what the
     * block given by blockName is.
     */
    public NexusBlock findBlockName(String blockName) {
        if (blockName.equalsIgnoreCase(ASSUMPTIONS_BLOCK.toString())) {
            return ASSUMPTIONS_BLOCK;
        } else if (blockName.equalsIgnoreCase(PAUP_BLOCK.toString())) {
            return PAUP_BLOCK;
        } else if (blockName.equalsIgnoreCase(MRBAYES_BLOCK.toString())) {
            return MRBAYES_BLOCK;
        } else {
            return super.findBlockName(blockName);
        }
    }

    /**
     * Parses a 'PAUP' block.
     */
    public List<CharSet> parseAssumptionsBlock() throws ImportException, IOException {
        // PAUP is largely a subset of BEAST block
        return readAssumptionsBlock();
    }

    /**
     * Parses a 'PAUP' block.
     */
    public PartitionModel parsePAUPBlock(BeautiOptions options) throws ImportException, IOException {
        PartitionModel model = new PartitionModel("nucs", Nucleotides.INSTANCE);

        readPAUPBlock(options, model);

        return model;
    }

    /**
     * Parses a 'MRBAYES' block.
     */
    public PartitionModel parseMrBayesBlock(BeautiOptions options) throws ImportException, IOException {
        return parseMrBayesBlock(options);
    }

    private List<CharSet> readAssumptionsBlock() throws ImportException, IOException {
        List<CharSet> charSets = new ArrayList<CharSet>();

        boolean done = false;
        while (!done) {
            String command = readToken(";");
            if (command.equalsIgnoreCase("ENDBLOCK") || command.equalsIgnoreCase("END")) {
                done = true;
            } else if (match("CHARSET", command, 5)) {
                if (getLastDelimiter() != ';') {
                    charSets.add(readCharSetCommand());
                }
            } else {
                System.err.println("The command, '" + command + "', is not used by BEAST and has been ignored");
            }
        }
        return charSets;
    }

    private CharSet readCharSetCommand() throws ImportException, IOException {

        String name = readToken("=;");

        String[] parts = readToken(";").split("-");

        int from;
        int to;

        try {
            if (parts.length == 2) {
                from = Integer.parseInt(parts[0]);
                to = Integer.parseInt(parts[1]);
            } else if (parts.length == 1) {
                from = Integer.parseInt(parts[0]);
                to = from;
            } else {
                throw new ImportException("CharSet, " + name + ", unable to be parsed");
            }
        } catch (NumberFormatException nfe) {
            throw new ImportException("CharSet, " + name + ", unable to be parsed");
        }
        return new CharSet(name, from, to);
    }

    private void readPAUPBlock(BeautiOptions options, PartitionModel model) throws ImportException, IOException {
        boolean done = false;

        while (!done) {
            String command = readToken(";");
            if (match("HSEARCH", command, 2)) {
                // Once we reach a search in PAUP then stop
                done = true;
            } else if (match("MCMC", command, 4)) {
                if (getLastDelimiter() != ';') {
                    readMCMCCommand(options);
                }
                done = true;
            } else if (match("MCMCP", command, 5)) {
                if (getLastDelimiter() != ';') {
                    readMCMCCommand(options);
                }
            } else if (match("LSET", command, 2)) {
                if (getLastDelimiter() != ';') {
                    readLSETCommand(model);
                }
            } else if (command.equalsIgnoreCase("ENDBLOCK") || command.equalsIgnoreCase("END")) {
                done = true;
            } else {

                System.err.println("The command, '" + command + "', is not used by BEAST and has been ignored");
            }
        }
    }

    private void readLSETCommand(PartitionModel model) throws ImportException, IOException {
        boolean done = false;

        while (!done) {
            String subcommand = readToken("=;");
            if (match("NST", subcommand, 2)) {
                int nst = readInteger(";");
                if (nst == 1) {
                    model.nucSubstitutionModel = NucModelType.JC;
                } else if (nst == 2) {
                    model.nucSubstitutionModel = NucModelType.HKY;
                } else if (nst == 6) {
                    model.nucSubstitutionModel = NucModelType.GTR;
                } else {
                    throw new BadFormatException("Bad value for NST subcommand of LSET command");
                }
            } else if (match("RATES", subcommand, 2)) {
                String token = readToken(";");

                if (match("EQUAL", token, 1)) {
                    model.gammaHetero = false;
                    model.invarHetero = false;
                } else if (match("GAMMA", token, 1)) {
                    model.gammaHetero = true;
                    model.invarHetero = false;
                } else if (match("PROPINV", token, 1)) {
                    model.gammaHetero = false;
                    model.invarHetero = true;
                } else if (match("INVGAMMA", token, 1)) {
                    model.gammaHetero = true;
                    model.invarHetero = true;
                } else if (match("ADGAMMA", token, 1)) {
                    System.err.println("The option, 'RATES=ADGAMMA', in the LSET command is not used by BEAST and has been ignored");
                } else if (match("SITESPEC", token, 1)) {
                    System.err.println("The option, 'RATES=SITESPEC', in the LSET command is not used by BEAST and has been ignored");
                } else {
                    throw new BadFormatException("Unknown value, '" + token + "'");
                }
            } else if (match("NGAMMACAT", subcommand, 2)) {

                model.gammaCategories = readInteger(";");
            } else {

                System.err.println("The option, '" + subcommand + "', in the LSET command is not used by BEAST and has been ignored");
            }

            if (getLastDelimiter() == ';') {
                done = true;
            }
        }
    }

    private void readMCMCCommand(BeautiOptions options) throws ImportException, IOException {
        boolean done = false;

        while (!done) {
            String subcommand = readToken("=;");
            if (match("NGEN", subcommand, 2)) {
                options.chainLength = readInteger(";");
            } else if (match("SAMPLEFREQ", subcommand, 2)) {
                options.logEvery = readInteger(";");
            } else if (match("PRINTFREQ", subcommand, 1)) {
                options.echoEvery = readInteger(";");
            } else if (match("FILENAME", subcommand, 1)) {
                options.fileName = readToken(";");
            } else if (match("BURNIN", subcommand, 1)) {
                options.burnIn = readInteger(";");
            } else if (match("STARTINGTREE", subcommand, 2)) {
                String token = readToken(";");
                if (match("USER", token, 1)) {
                    options.userTree = true;
                } else if (match("RANDOM", token, 1)) {
                    options.userTree = false;
                } else {
                    throw new BadFormatException("Unknown value, '" + token + "'");
                }
            } else {

                System.err.println("The option, '" + subcommand + "', in the MCMC command is not used by BEAST and has been ignored");
            }

            if (getLastDelimiter() == ';') {
                done = true;
            }
        }
    }

    private boolean match(String reference, String target, int min) throws ImportException {
        if (target.length() < min) {
            throw new BadFormatException("Ambiguous command or subcommand, '" + target + "'");
        }

        return reference.startsWith(target.toUpperCase());
    }

    public class CharSet {
        public CharSet(String name, int fromSite, int toSite) {
            this.name = name;
            this.fromSite = fromSite;
            this.toSite = toSite;
        }

        public String getName() {
            return name;
        }

        public int getFromSite() {
            return fromSite;
        }

        public int getToSite() {
            return toSite;
        }

        private final String name;
        private final int fromSite;
        private final int toSite;
    }
}
