/*
 * BeastGenerator.java
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

package dr.app.beauti.generator;

import dr.app.beast.BeastVersion;
import dr.app.beauti.XMLWriter;
import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.options.*;
import dr.app.beauti.priorsPanel.PriorType;
import dr.evolution.alignment.Alignment;
import dr.evolution.alignment.SitePatterns;
import dr.evolution.datatype.DataType;
import dr.evolution.datatype.Nucleotides;
import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;
import dr.evolution.util.TaxonList;
import dr.evolution.util.Units;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.StrictClockBranchRates;
import dr.evomodel.clock.ACLikelihood;
import dr.evomodel.coalescent.BayesianSkylineLikelihood;
import dr.evomodel.coalescent.CoalescentLikelihood;
import dr.evomodel.coalescent.GMRFSkyrideLikelihood;
import dr.evomodel.coalescent.operators.GMRFSkyrideBlockUpdateOperator;
import dr.evomodel.coalescent.operators.SampleNonActiveGibbsOperator;
import dr.evomodel.operators.ExchangeOperator;
import dr.evomodel.operators.SubtreeSlideOperator;
import dr.evomodel.operators.TreeBitMoveOperator;
import dr.evomodel.operators.WilsonBalding;
import dr.evomodel.speciation.SpeciationLikelihood;
import dr.evomodel.tree.*;
import dr.evomodelxml.DiscretizedBranchRatesParser;
import dr.evomodelxml.LoggerParser;
import dr.evomodelxml.TreeLoggerParser;
import dr.evoxml.AlignmentParser;
import dr.evoxml.MergePatternsParser;
import dr.evoxml.SitePatternsParser;
import dr.evoxml.TaxaParser;
import dr.inference.distribution.DistributionLikelihood;
import dr.inference.distribution.ExponentialDistributionModel;
import dr.inference.distribution.ExponentialMarkovModel;
import dr.inference.distribution.MixedDistributionLikelihood;
import dr.inference.loggers.Columns;
import dr.inference.model.*;
import dr.inference.operators.*;
import dr.util.Attribute;
import dr.util.Version;
import dr.xml.XMLParser;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class holds all the data for the current BEAUti Document
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: BeastGenerator.java,v 1.4 2006/09/05 13:29:34 rambaut Exp $
 */
public class BeastGenerator extends Generator {

    private final static Version version = new BeastVersion();
//    private final String mulitTaxaTagName = "gene";

    private final TreePriorGenerator treePriorGenerator;
    private final TreeLikelihoodGenerator treeLikelihoodGenerator;
    private final PartitionModelGenerator partitionModelGenerator;
    private final InitialTreeGenerator initialTreeGenerator;
    private final TreeModelGenerator treeModelGenerator;
    private final BranchRatesModelGenerator branchRatesModelGenerator;

    public BeastGenerator(BeautiOptions options, ComponentFactory[] components) {
        super(options, components);

        partitionModelGenerator = new PartitionModelGenerator(options, components);
        treePriorGenerator = new TreePriorGenerator(options, components);
        treeLikelihoodGenerator = new TreeLikelihoodGenerator(options, components);

        initialTreeGenerator = new InitialTreeGenerator(options, components);
        treeModelGenerator = new TreeModelGenerator(options, components);
        branchRatesModelGenerator = new BranchRatesModelGenerator(options, components);
    }

    /**
     * Checks various options to check they are valid. Throws IllegalArgumentExceptions with
     * descriptions of the problems.
     *
     * @throws IllegalArgumentException if there is a problem with the current settings
     */
    public void checkOptions() throws IllegalArgumentException {

        TaxonList taxonList = options.taxonList;
        Set<String> ids = new HashSet<String>();

        ids.add("taxa");
        ids.add(AlignmentParser.ALIGNMENT);

        if (taxonList != null) {
            if (taxonList.getTaxonCount() < 2) {
                throw new IllegalArgumentException("BEAST requires at least two taxa to run.");
            }

            for (int i = 0; i < taxonList.getTaxonCount(); i++) {
                Taxon taxon = taxonList.getTaxon(i);
                if (ids.contains(taxon.getId())) {
                    throw new IllegalArgumentException("A taxon has the same id," + taxon.getId() +
                            "\nas another element (taxon, sequence, taxon set etc.):\nAll ids should be unique.");
                }
                ids.add(taxon.getId());
            }
        }

        for (Taxa taxa : options.taxonSets) {
            if (taxa.getTaxonCount() < 2) {
                throw new IllegalArgumentException("Taxon set, " + taxa.getId() + ", should contain\n" +
                        "at least two taxa.");
            }
            if (ids.contains(taxa.getId())) {
                throw new IllegalArgumentException("A taxon sets has the same id," + taxa.getId() +
                        "\nas another element (taxon, sequence, taxon set etc.):\nAll ids should be unique.");
            }
            ids.add(taxa.getId());
        }

        // add other tests and warnings here
        // Speciation model with dated tips
        // Sampling rates without dated tips or priors on rate or nodes

    }

    /**
     * Generate a beast xml file from these beast options
     *
     * @param w the writer
     */
    public void generateXML(Writer w) {

        XMLWriter writer = new XMLWriter(w);

        writer.writeText("<?xml version=\"1.0\" standalone=\"yes\"?>");
        writer.writeComment("Generated by BEAUTi " + version.getVersionString());
        writer.writeComment("      by Alexei J. Drummond and Andrew Rambaut");
        writer.writeComment("      Department of Computer Science, University of Auckland and");
        writer.writeComment("      Institute of Evolutionary Biology, University of Edinburgh");
        writer.writeComment("      http://beast.bio.ed.ac.uk/");
        writer.writeOpenTag("beast");
        writer.writeText("");

        // this gives any added implementations of the 'Component' interface a
        // chance to generate XML at this point in the BEAST file.
        generateInsertionPoint(ComponentGenerator.InsertionPoint.BEFORE_TAXA, writer);

        writeTaxa(writer, options.taxonList);

        List<Taxa> taxonSets = options.taxonSets;
        if (taxonSets != null && taxonSets.size() > 0) {
            writeTaxonSets(writer, taxonSets);
        }

        if (options.traits.contains(options.TRAIT_SPECIES)) { // species
        	writer.writeText("");
        	writer.writeComment("List all taxons regarding each gene (file) for Multispecies Coalescent function");
        	// write all taxa in each gene tree regarding each data partition,
        	for (DataPartition partition : options.dataPartitions) {
        		// do I need if (!alignments.contains(alignment)) {alignments.add(alignment);} ?
        		writeAllTaxaForMultiGene(partition, writer);
        	}
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_TAXA, writer);

        List<Alignment> alignments = new ArrayList<Alignment>();

        for (DataPartition partition : options.dataPartitions) {
            Alignment alignment = partition.getAlignment();
            if (!alignments.contains(alignment)) {
                alignments.add(alignment);
            }
        }

        if (!options.samplePriorOnly) {
            int index = 1;
            for (Alignment alignment : alignments) {
                if (alignments.size() > 1) {
                	 //if (!options.allowDifferentTaxa) {
                		 alignment.setId(AlignmentParser.ALIGNMENT + index);
                     //} else { // e.g. alignment_gene1
                    	// alignment.setId("alignment_" + mulitTaxaTagName + index);
                     //}
                } else {
                	alignment.setId(AlignmentParser.ALIGNMENT);
                }
                writeAlignment(alignment, writer);
                index += 1;
                writer.writeText("");
            }

            generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_SEQUENCES, writer);

            for (PartitionModel model : options.getActivePartitionModels()) {
                writePatternList(model, writer);
                writer.writeText("");
            }

            generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_PATTERNS, writer);
        } else {
            Alignment alignment = alignments.get(0);
            alignment.setId(AlignmentParser.ALIGNMENT);
            writeAlignment(alignment, writer);
            writer.writeText("");

            generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_SEQUENCES, writer);
            generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_PATTERNS, writer);
        }

        treePriorGenerator.writeTreePriorModel(writer);
        writer.writeText("");

        if (options.traits.contains(options.TRAIT_SPECIES)) { // species
        	for (PartitionModel model : options.getActivePartitionModels()) {
	        	initialTreeGenerator.setPrefix(model.getName() + "."); // partitionName.startingTree
	        	initialTreeGenerator.writeStartingTree(writer);
        	}
        } else { // no species
        	initialTreeGenerator.writeStartingTree(writer);
        }
        writer.writeText("");

//        treeModelGenerator = new TreeModelGenerator(options);
        if (options.traits.contains(options.TRAIT_SPECIES)) { // species
	        // generate gene trees regarding each data partition, if no species, only create 1 tree
	    	for (PartitionModel model : options.getActivePartitionModels()) {
	    		treeModelGenerator.setPrefix(model.getName() + "."); // partitionName.treeModel
	    		treeModelGenerator.writeTreeModel(writer);
	        }
        } else { // no species
        	treeModelGenerator.setPrefix("");
        	treeModelGenerator.writeTreeModel(writer);
	    }
        writer.writeText("");

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_TREE_MODEL, writer);

        if (options.traits.contains(options.TRAIT_SPECIES)) { // species
	        for (PartitionModel model : options.getActivePartitionModels()) {
	        	treePriorGenerator.setPrefix(model.getName() + "."); // partitionName.treeModel
	        	treePriorGenerator.writeTreePrior(writer);
	        }
        } else { // no species
        	treePriorGenerator.setPrefix("");
        	treePriorGenerator.writeTreePrior(writer);
	    }
        writer.writeText("");

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_TREE_PRIOR, writer);

        branchRatesModelGenerator.writeBranchRatesModel(writer);
        writer.writeText("");

        for (PartitionModel partitionModel : options.getActivePartitionModels()) {
            partitionModelGenerator.writeSubstitutionModel(partitionModel, writer);
            writer.writeText("");
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_SUBSTITUTION_MODEL, writer);

        boolean writeMuParameters = options.getTotalActivePartitionModelCount() > 1;

        for (PartitionModel model : options.getActivePartitionModels()) {
            partitionModelGenerator.writeSiteModel(model, writeMuParameters, writer);
            writer.writeText("");
        }

        if (writeMuParameters) {
            writer.writeOpenTag(CompoundParameter.COMPOUND_PARAMETER, new Attribute[]{new Attribute.Default<String>(XMLParser.ID, "allMus")});
            for (PartitionModel model : options.getActivePartitionModels()) {
                partitionModelGenerator.writeMuParameterRefs(model, writer);
            }
            writer.writeCloseTag(CompoundParameter.COMPOUND_PARAMETER);
            writer.writeText("");
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_SITE_MODEL, writer);

        for (PartitionModel model : options.getActivePartitionModels()) {
        	if (options.traits.contains(options.TRAIT_SPECIES)) { // species
        		treeLikelihoodGenerator.setGenePrefix(model.getName() + ".");
        	} else {
        		treeLikelihoodGenerator.setGenePrefix("");
        	}
            //TODO: need merge genePrifx and prefix
        	treeLikelihoodGenerator.writeTreeLikelihood(model, writer);
            writer.writeText("");
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_TREE_LIKELIHOOD, writer);

        // traits tag
        if (options.traits.size() > 0) {
        	for (String trait : options.traits) {
        		BeautiOptions.TraitType traiType = options.traitTypes.get(trait);

        		writeTraits(writer, trait, traiType.toString(), options.taxonList);
        	}
            generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_TRAITS, writer);
        }

        if (taxonSets != null && taxonSets.size() > 0) {
            writeTMRCAStatistics(writer);
        }

        List<Operator> operators = options.selectOperators();
        writeOperatorSchedule(operators, writer);
        writer.writeText("");

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_OPERATORS, writer);

        // XMLWriter writer, List<PartitionModel> models, boolean traitsContainSpecies
        writeMCMC(options.getActivePartitionModels(), (options.traits.contains(options.TRAIT_SPECIES)), writer);
        writer.writeText("");

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_MCMC, writer);

        writeTimerReport(writer);
        writer.writeText("");
        if (options.performTraceAnalysis) {
            writeTraceAnalysis(writer);
        }
        if (options.generateCSV) {
            treePriorGenerator.writeAnalysisToCSVfile(writer);
        }

        writer.writeCloseTag("beast");
        writer.flush();
    }

    /**
     * Generate a taxa block from these beast options
     *
     * @param writer    the writer
     * @param taxonList the taxon list to write
     */
    private void writeTaxa(XMLWriter writer, TaxonList taxonList) {
    	// -1 (single taxa), 0 (1st gene of multi-taxa)

    	writer.writeComment("The list of taxa analyse (can also include dates/ages).");
    	writer.writeComment("ntax=" + taxonList.getTaxonCount());
	    writer.writeOpenTag("taxa", new Attribute[]{new Attribute.Default<String>(XMLParser.ID, "taxa")});


        boolean firstDate = true;
        for (int i = 0; i < taxonList.getTaxonCount(); i++) {
            Taxon taxon = taxonList.getTaxon(i);

            boolean hasDate = false;

            if (options.maximumTipHeight > 0.0) {
                hasDate = TaxonList.Utils.hasAttribute(taxonList, i, dr.evolution.util.Date.DATE);
            }


            writer.writeTag("taxon", new Attribute[]{new Attribute.Default<String>(XMLParser.ID, taxon.getId())}, !hasDate);


            if (hasDate) {
                dr.evolution.util.Date date = (dr.evolution.util.Date) taxon.getAttribute(dr.evolution.util.Date.DATE);

                if (firstDate) {
                    options.units = date.getUnits();
                    firstDate = false;
                } else {
                    if (options.units != date.getUnits()) {
                        System.err.println("Error: Units in dates do not match.");
                    }
                }

                Attribute[] attributes = new Attribute[]{
                        new Attribute.Default<Double>("value", date.getTimeValue()),
                        new Attribute.Default<String>("direction", date.isBackwards() ? "backwards" : "forwards"),
                        new Attribute.Default<String>("units", Units.Utils.getDefaultUnitName(options.units))
                        /*,
                                                    new Attribute.Default("origin", date.getOrigin()+"")*/
                };

                writer.writeTag(dr.evolution.util.Date.DATE, attributes, true);
                writer.writeCloseTag("taxon");
            }

            generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_TAXON, taxon, writer);
        }

        writer.writeCloseTag("taxa");
    }

    /**
     * Generate additional taxon sets
     *
     * @param writer    the writer
     * @param taxonSets a list of taxa to write
     */
    private void writeTaxonSets(XMLWriter writer, List<Taxa> taxonSets) {

        writer.writeText("");
        for (Taxa taxa : taxonSets) {
            writer.writeOpenTag(
                    "taxa",
                    new Attribute[]{
                            new Attribute.Default<String>(XMLParser.ID, taxa.getId())
                    }
            );

            for (int j = 0; j < taxa.getTaxonCount(); j++) {
                Taxon taxon = taxa.getTaxon(j);

                writer.writeTag("taxon", new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, taxon.getId())}, true);
            }
            writer.writeCloseTag("taxa");
        }
    }


    /**
     * Determine and return the datatype description for these beast options
     * note that the datatype in XML may differ from the actual datatype
     *
     * @param alignment the alignment to get data type description of
     * @return description
     */

    private String getAlignmentDataTypeDescription(Alignment alignment) {
        String description;

        switch (alignment.getDataType().getType()) {
            case DataType.TWO_STATES:
            case DataType.COVARION:

                // TODO make this work
                throw new RuntimeException("TO DO!");

                //switch (partition.getPartitionModel().binarySubstitutionModel) {
                //    case ModelOptions.BIN_COVARION:
                //        description = TwoStateCovarion.DESCRIPTION;
                //        break;
                //
                //    default:
                //        description = alignment.getDataType().getDescription();
                //}
                //break;

            default:
                description = alignment.getDataType().getDescription();
        }

        return description;
    }


    public void writeAllTaxaForMultiGene(DataPartition dataPartition, XMLWriter writer) {
    	String gene = dataPartition.getName();
    	Alignment alignment = dataPartition.getAlignment();

    	writer.writeComment("gene name = " + gene + ", ntax= " + alignment.getTaxonCount());
    	writer.writeOpenTag("taxa", new Attribute[]{new Attribute.Default<String>(XMLParser.ID, gene + ".taxa")});

    	for (int i = 0; i < alignment.getTaxonCount(); i++) {
            Taxon taxon = alignment.getTaxon(i);
            writer.writeTag("taxon", new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, taxon.getId())}, true);
    	}

    	writer.writeCloseTag("taxa");
    }


    /**
     * Generate an alignment block from these beast options
     *
     * @param alignment the alignment to write
     * @param writer    the writer
     */
    public void writeAlignment(Alignment alignment, XMLWriter writer) {

        writer.writeText("");
        writer.writeComment("The sequence alignment (each sequence refers to a taxon above).");
        writer.writeComment("ntax=" + alignment.getTaxonCount() + " nchar=" + alignment.getSiteCount());
        if (options.samplePriorOnly) {
            writer.writeComment("Null sequences generated in order to sample from the prior only.");
        }

        writer.writeOpenTag(
                AlignmentParser.ALIGNMENT,
                new Attribute[]{
                        new Attribute.Default<String>(XMLParser.ID, alignment.getId()),
                        new Attribute.Default<String>("dataType", getAlignmentDataTypeDescription(alignment))
                }
        );

        for (int i = 0; i < alignment.getTaxonCount(); i++) {
            Taxon taxon = alignment.getTaxon(i);

            writer.writeOpenTag("sequence");
            writer.writeTag("taxon", new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, taxon.getId())}, true);
            if (!options.samplePriorOnly) {
                writer.writeText(alignment.getAlignedSequenceString(i));
            } else {
                writer.writeText("N");
            }
            writer.writeCloseTag("sequence");
        }
        writer.writeCloseTag(AlignmentParser.ALIGNMENT);
    }

    /**
     * Generate traits block regarding specific trait name (e.g. <species>) from these beast options
     * @param writer
     * @param trait
     * @param traitType
     * @param taxonList
     */
    private void writeTraits(XMLWriter writer, String trait, String traitType, TaxonList taxonList) {

    	writer.writeText("");
        if (trait.equals(options.TRAIT_SPECIES)) { // hard code
        	writer.writeComment("Species definition: binds taxa, species and gene trees");
        }
        writer.writeComment("trait = " + trait + " trait_type = " + traitType);

        writer.writeOpenTag(trait, new Attribute[]{new Attribute.Default<String>(XMLParser.ID, trait)});
        		//new Attribute.Default<String>("traitType", traitType)});

        // write sub-tags for species
        if (trait.equals(options.TRAIT_SPECIES)) { // hard code
        	List<String> species = new ArrayList<String>(); // hard code
        	String sp;

        	for (int i = 0; i < taxonList.getTaxonCount(); i++) {
        		Taxon taxon = taxonList.getTaxon(i);
	        	sp = taxon.getAttribute(options.TRAIT_SPECIES).toString();

	        	if (!species.contains(sp)) {
	        		species.add(sp);
	        	}
        	}

        	for (String eachSp : species) {
        		writer.writeOpenTag("sp", new Attribute[]{new Attribute.Default<String>(XMLParser.ID, eachSp)});

        		for (int i = 0; i < taxonList.getTaxonCount(); i++) {
        			Taxon taxon = taxonList.getTaxon(i);
        			sp = taxon.getAttribute(options.TRAIT_SPECIES).toString();

        			if (sp.equals(eachSp)) {
        				writer.writeTag("taxon", new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, taxon.getId())}, true);
        			}

        		}
        		writer.writeCloseTag("sp");
        	}

        	writeGeneTrees (writer);
        } // end write sub-tags for species

        writer.writeCloseTag(trait);

        if (trait.equals(options.TRAIT_SPECIES)) { // hard code
        	writeSpeciesTree (writer);

        }

    }


    private void writeGeneTrees(XMLWriter writer) {
    	writer.writeComment("Collection of Gene Trees");

        writer.writeOpenTag("geneTrees", new Attribute[]{new Attribute.Default<String>(XMLParser.ID, "geneTrees")});

    	// generate gene trees regarding each data partition
    	for (PartitionModel partitionModel : options.getActivePartitionModels()) {
    		writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF,
    				TreeModel.TREE_MODEL + "_" + partitionModel.getName())}, true);
        }

        writer.writeCloseTag("geneTrees");
    }


    private void writeSpeciesTree(XMLWriter writer) {
    	writer.writeComment("Species Tree: Provides Per branch demographic function");

//        writer.writeOpenTag("speciesTree", new Attribute[]{new Attribute.Default<String>(XMLParser.ID, ),
//				new Attribute.Default<String>("bmPrior", "true")});
//        writer.writeTag(options.TRAIT_SPECIES, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, options.TRAIT_SPECIES)}, true);


//        writer.writeCloseTag("speciesTree");

    }


    /**
     * Writes the pattern lists
     *
     * @param model  the partition model to write the pattern lists for
     * @param writer the writer
     */
    public void writePatternList(PartitionModel model, XMLWriter writer) {
        writer.writeText("");

        String codonHeteroPattern = model.getCodonHeteroPattern();
        int partitionCount = model.getCodonPartitionCount();

        if (model.dataType == Nucleotides.INSTANCE && codonHeteroPattern != null && partitionCount > 1) {

            if (codonHeteroPattern.equals("112")) {
                writer.writeComment("The unique patterns for codon positions 1 & 2");
                writer.writeOpenTag(MergePatternsParser.MERGE_PATTERNS,
                        new Attribute[]{
                                new Attribute.Default<String>(XMLParser.ID, model.getPrefix(1) + SitePatternsParser.PATTERNS),
                        }
                );
                for (DataPartition partition : options.dataPartitions) {
                    if (partition.getPartitionModel() == model) {
                        writePatternList(partition, 0, 3, writer);
                        writePatternList(partition, 1, 3, writer);
                    }
                }
                writer.writeCloseTag(MergePatternsParser.MERGE_PATTERNS);

                writer.writeComment("The unique patterns for codon positions 3");
                writer.writeOpenTag(MergePatternsParser.MERGE_PATTERNS,
                        new Attribute[]{
                                new Attribute.Default<String>(XMLParser.ID, model.getPrefix(2) + SitePatternsParser.PATTERNS),
                        }
                );

                for (DataPartition partition : options.dataPartitions) {
                    if (partition.getPartitionModel() == model) {
                        writePatternList(partition, 2, 3, writer);
                    }
                }

                writer.writeCloseTag(MergePatternsParser.MERGE_PATTERNS);

            } else {
                // pattern is 123
                // write pattern lists for all three codon positions
                for (int i = 1; i <= 3; i++) {
                    writer.writeComment("The unique patterns for codon positions " + i);
                    writer.writeOpenTag(MergePatternsParser.MERGE_PATTERNS,
                            new Attribute[]{
                                    new Attribute.Default<String>(XMLParser.ID, model.getPrefix(i) + SitePatternsParser.PATTERNS),
                            }
                    );

                    for (DataPartition partition : options.dataPartitions) {
                        if (partition.getPartitionModel() == model) {
                            writePatternList(partition, i - 1, 3, writer);
                        }
                    }

                    writer.writeCloseTag(MergePatternsParser.MERGE_PATTERNS);
                }

            }
        } else {
            //partitionCount = 1;
            writer.writeComment("The unique patterns site patterns");
            writer.writeOpenTag(MergePatternsParser.MERGE_PATTERNS,
                    new Attribute[]{
                            new Attribute.Default<String>(XMLParser.ID, model.getPrefix() + SitePatternsParser.PATTERNS),
                    }
            );

            for (DataPartition partition : options.dataPartitions) {
                if (partition.getPartitionModel() == model) {
                    writePatternList(partition, 0, 1, writer);
                }
            }

            writer.writeCloseTag(MergePatternsParser.MERGE_PATTERNS);
        }
    }

    /**
     * Write a single pattern list
     *
     * @param partition the partition to write a pattern list for
     * @param offset    offset by
     * @param every     skip every
     * @param writer    the writer
     */
    private void writePatternList(DataPartition partition, int offset, int every, XMLWriter writer) {

        Alignment alignment = partition.getAlignment();
        int from = partition.getFromSite();
        int to = partition.getToSite();
        int partEvery = partition.getEvery();
        if (partEvery > 1 && every > 1) throw new IllegalArgumentException();

        if (from < 1) from = 1;
        every = Math.max(partEvery, every);

        from += offset;

        writer.writeComment("The unique patterns from " + from + " to " + (to > 0 ? to : "end") + ((every > 1) ? " every " + every : ""));

        // this object is created solely to calculate the number of patterns in the alignment
        SitePatterns patterns = new SitePatterns(alignment, from - 1, to - 1, every);
        writer.writeComment("npatterns=" + patterns.getPatternCount());

        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute.Default<String>("from", "" + from));
        if (to >= 0) attributes.add(new Attribute.Default<String>("to", "" + to));

        if (every > 1) {
            attributes.add(new Attribute.Default<String>("every", "" + every));
        }
        writer.writeOpenTag(SitePatternsParser.PATTERNS, attributes);

        writer.writeTag(AlignmentParser.ALIGNMENT, new Attribute.Default<String>(XMLParser.IDREF, alignment.getId()), true);
        writer.writeCloseTag(SitePatternsParser.PATTERNS);
    }

    /**
     * Generate tmrca statistics
     *
     * @param writer the writer
     */
    public void writeTMRCAStatistics(XMLWriter writer) {

        writer.writeText("");
        for (Taxa taxa : options.taxonSets) {
            writer.writeOpenTag(
                    TMRCAStatistic.TMRCA_STATISTIC,
                    new Attribute[]{
                            new Attribute.Default<String>(XMLParser.ID, "tmrca(" + taxa.getId() + ")"),
                    }
            );
            writer.writeOpenTag(TMRCAStatistic.MRCA);
            writer.writeTag(TaxaParser.TAXA, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, taxa.getId())}, true);
            writer.writeCloseTag(TMRCAStatistic.MRCA);
            writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
            writer.writeCloseTag(TMRCAStatistic.TMRCA_STATISTIC);

            if (options.taxonSetsMono.get(taxa)) {
                writer.writeOpenTag(
                        MonophylyStatistic.MONOPHYLY_STATISTIC,
                        new Attribute[]{
                                new Attribute.Default<String>(XMLParser.ID, "monophyly(" + taxa.getId() + ")"),
                        });
                writer.writeOpenTag(MonophylyStatistic.MRCA);
                writer.writeTag(TaxaParser.TAXA, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, taxa.getId())}, true);
                writer.writeCloseTag(MonophylyStatistic.MRCA);
                writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
                writer.writeCloseTag(MonophylyStatistic.MONOPHYLY_STATISTIC);
            }
        }
    }

    /**
     * Write the operator schedule XML block.
     *
     * @param operators the list of operators
     * @param writer    the writer
     */
    public void writeOperatorSchedule(List<Operator> operators, XMLWriter writer) {
        Attribute[] operatorAttributes;
//		switch (options.coolingSchedule) {
//			case SimpleOperatorSchedule.LOG_SCHEDULE:
        if (options.nodeHeightPrior == TreePrior.GMRF_SKYRIDE) {
            operatorAttributes = new Attribute[2];
            operatorAttributes[1] = new Attribute.Default<String>(SimpleOperatorSchedule.OPTIMIZATION_SCHEDULE, SimpleOperatorSchedule.LOG_STRING);
        } else {
//				break;
//			default:
            operatorAttributes = new Attribute[1];
        }
        operatorAttributes[0] = new Attribute.Default<String>(XMLParser.ID, "operators");

        writer.writeOpenTag(
                SimpleOperatorSchedule.OPERATOR_SCHEDULE,
                operatorAttributes
//				new Attribute[]{new Attribute.Default<String>(XMLParser.ID, "operators")}
        );

        for (Operator operator : operators) {
            if (operator.weight > 0. && operator.inUse)
                writeOperator(operator, writer);
        }

        writer.writeCloseTag(SimpleOperatorSchedule.OPERATOR_SCHEDULE);
    }

    private void writeOperator(Operator operator, XMLWriter writer) {

        switch (operator.type) {

            case SCALE:
                writeScaleOperator(operator, writer);
                break;
            case RANDOM_WALK:
                writeRandomWalkOperator(operator, writer);
            case RANDOM_WALK_ABSORBING:
                writeRandomWalkOperator(operator, false, writer);
                break;
            case RANDOM_WALK_REFLECTING:
                writeRandomWalkOperator(operator, true, writer);
                break;
            case INTEGER_RANDOM_WALK:
                writeIntegerRandomWalkOperator(operator, writer);
                break;
            case UP_DOWN:
                writeUpDownOperator(operator, writer);
                break;
            case SCALE_ALL:
                writeScaleAllOperator(operator, writer);
                break;
            case SCALE_INDEPENDENTLY:
                writeScaleOperator(operator, writer, true);
                break;
            case CENTERED_SCALE:
                writeCenteredOperator(operator, writer);
                break;
            case DELTA_EXCHANGE:
                writeDeltaOperator(operator, writer);
                break;
            case INTEGER_DELTA_EXCHANGE:
                writeIntegerDeltaOperator(operator, writer);
                break;
            case SWAP:
                writeSwapOperator(operator, writer);
                break;
            case BITFLIP:
                writeBitFlipOperator(operator, writer);
                break;
            case TREE_BIT_MOVE:
                writeTreeBitMoveOperator(operator, writer);
                break;
            case UNIFORM:
                writeUniformOperator(operator, writer);
                break;
            case INTEGER_UNIFORM:
                writeIntegerUniformOperator(operator, writer);
                break;
            case SUBTREE_SLIDE:
                writeSubtreeSlideOperator(operator, writer);
                break;
            case NARROW_EXCHANGE:
                writeNarrowExchangeOperator(operator, writer);
                break;
            case WIDE_EXCHANGE:
                writeWideExchangeOperator(operator, writer);
                break;
            case WILSON_BALDING:
                writeWilsonBaldingOperator(operator, writer);
                break;
            case SAMPLE_NONACTIVE:
                writeSampleNonActiveOperator(operator, writer);
                break;
            case SCALE_WITH_INDICATORS:
                writeScaleWithIndicatorsOperator(operator, writer);
                break;
            case GMRF_GIBBS_OPERATOR:
                writeGMRFGibbsOperator(operator, writer);
                break;
            default:
                throw new IllegalArgumentException("Unknown operator type");
        }
    }

    private Attribute getRef(String name) {
        return new Attribute.Default<String>(XMLParser.IDREF, name);
    }

    private void writeParameterRefByName(XMLWriter writer, String name) {
        writer.writeTag(ParameterParser.PARAMETER, getRef(name), true);
    }

    private void writeParameter1Ref(XMLWriter writer, Operator operator) {
        writeParameterRefByName(writer, operator.parameter1.getName());
    }

    private void writeScaleOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(
                ScaleOperator.SCALE_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<Double>("scaleFactor", operator.tuning),
                        getWeightAttribute(operator.weight)
                });
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(ScaleOperator.SCALE_OPERATOR);
    }

    private void writeScaleOperator(Operator operator, XMLWriter writer, boolean indepedently) {
        writer.writeOpenTag(
                ScaleOperator.SCALE_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<Double>("scaleFactor", operator.tuning),
                        getWeightAttribute(operator.weight),
                        new Attribute.Default<String>("scaleAllIndependently", indepedently ? "true" : "false")
                });
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(ScaleOperator.SCALE_OPERATOR);
    }

    private void writeRandomWalkOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(
                "randomWalkOperator",
                new Attribute[]{
                        new Attribute.Default<Double>("windowSize", operator.tuning),
                        getWeightAttribute(operator.weight)
                });
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag("randomWalkOperator");
    }

    private void writeRandomWalkOperator(Operator operator, boolean reflecting, XMLWriter writer) {
        writer.writeOpenTag(
                "randomWalkOperator",
                new Attribute[]{
                        new Attribute.Default<Double>("windowSize", operator.tuning),
                        getWeightAttribute(operator.weight),
                        new Attribute.Default<String>("boundaryCondition",
                                (reflecting ? "reflecting" : "absorbing"))
                });
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag("randomWalkOperator");
    }

    private void writeIntegerRandomWalkOperator(Operator operator, XMLWriter writer) {

        int windowSize = (int) Math.round(operator.tuning);
        if (windowSize < 1) windowSize = 1;

        writer.writeOpenTag(
                "randomWalkIntegerOperator",
                new Attribute[]{
                        new Attribute.Default<Integer>("windowSize", windowSize),
                        getWeightAttribute(operator.weight)
                });
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag("randomWalkIntegerOperator");
    }

    private void writeScaleAllOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(
                ScaleOperator.SCALE_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<Double>("scaleFactor", operator.tuning),
                        new Attribute.Default<String>("scaleAll", "true"),
                        getWeightAttribute(operator.weight)
                });

        if (operator.parameter2 == null) {
            writeParameter1Ref(writer, operator);
        } else {
            writer.writeOpenTag(CompoundParameter.COMPOUND_PARAMETER);
            writeParameter1Ref(writer, operator);
            writer.writeTag(ParameterParser.PARAMETER, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, operator.parameter2.getName())}, true);
            writer.writeCloseTag(CompoundParameter.COMPOUND_PARAMETER);
        }

        writer.writeCloseTag(ScaleOperator.SCALE_OPERATOR);
    }

    private void writeUpDownOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(UpDownOperator.UP_DOWN_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<Double>("scaleFactor", operator.tuning),
                        getWeightAttribute(operator.weight)
                }
        );

        writer.writeOpenTag(UpDownOperator.UP);
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(UpDownOperator.UP);

        writer.writeOpenTag(UpDownOperator.DOWN);
        writer.writeTag(ParameterParser.PARAMETER, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, operator.parameter2.getName())}, true);
        writer.writeCloseTag(UpDownOperator.DOWN);

        writer.writeCloseTag(UpDownOperator.UP_DOWN_OPERATOR);
    }

    private void writeCenteredOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(CenteredScaleOperator.CENTERED_SCALE,
                new Attribute[]{
                        new Attribute.Default<Double>(CenteredScaleOperator.SCALE_FACTOR, operator.tuning),
                        getWeightAttribute(operator.weight)
                }
        );
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(CenteredScaleOperator.CENTERED_SCALE);
    }

    private void writeDeltaOperator(Operator operator, XMLWriter writer) {


        if (operator.getName().equals("Relative rates")) {

            int[] parameterWeights = options.getPartitionWeights();

            if (parameterWeights != null && parameterWeights.length > 1) {
                String pw = "" + parameterWeights[0];
                for (int i = 1; i < parameterWeights.length; i++) {
                    pw += " " + parameterWeights[i];
                }
                writer.writeOpenTag(DeltaExchangeOperator.DELTA_EXCHANGE,
                        new Attribute[]{
                                new Attribute.Default<Double>(DeltaExchangeOperator.DELTA, operator.tuning),
                                new Attribute.Default<String>(DeltaExchangeOperator.PARAMETER_WEIGHTS, pw),
                                getWeightAttribute(operator.weight)
                        }
                );
            }
        } else {
            writer.writeOpenTag(DeltaExchangeOperator.DELTA_EXCHANGE,
                    new Attribute[]{
                            new Attribute.Default<Double>(DeltaExchangeOperator.DELTA, operator.tuning),
                            getWeightAttribute(operator.weight)
                    }
            );
        }

        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(DeltaExchangeOperator.DELTA_EXCHANGE);
    }

    private void writeIntegerDeltaOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(DeltaExchangeOperator.DELTA_EXCHANGE,
                new Attribute[]{
                        new Attribute.Default<String>(DeltaExchangeOperator.DELTA, Integer.toString((int) operator.tuning)),
                        new Attribute.Default<String>("integer", "true"),
                        getWeightAttribute(operator.weight),
                        new Attribute.Default<String>("autoOptimize", "false")
                }
        );
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(DeltaExchangeOperator.DELTA_EXCHANGE);
    }

    private void writeSwapOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(SwapOperator.SWAP_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<String>("size", Integer.toString((int) operator.tuning)),
                        getWeightAttribute(operator.weight),
                        new Attribute.Default<String>("autoOptimize", "false")
                }
        );
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(SwapOperator.SWAP_OPERATOR);
    }

    private void writeBitFlipOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(BitFlipOperator.BIT_FLIP_OPERATOR,
                getWeightAttribute(operator.weight));
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(BitFlipOperator.BIT_FLIP_OPERATOR);
    }

    private void writeTreeBitMoveOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(TreeBitMoveOperator.BIT_MOVE_OPERATOR,
                        getWeightAttribute(operator.weight));
        writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
        writer.writeCloseTag(TreeBitMoveOperator.BIT_MOVE_OPERATOR);
    }

    private void writeUniformOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag("uniformOperator",
                getWeightAttribute(operator.weight));
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag("uniformOperator");
    }

    private void writeIntegerUniformOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag("uniformIntegerOperator",
                getWeightAttribute(operator.weight));
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag("uniformIntegerOperator");
    }

    private void writeNarrowExchangeOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(ExchangeOperator.NARROW_EXCHANGE,
                getWeightAttribute(operator.weight));
        writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
        writer.writeCloseTag(ExchangeOperator.NARROW_EXCHANGE);
    }

    private void writeWideExchangeOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(ExchangeOperator.WIDE_EXCHANGE,
                getWeightAttribute(operator.weight));
        writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
        writer.writeCloseTag(ExchangeOperator.WIDE_EXCHANGE);
    }

    private void writeWilsonBaldingOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(WilsonBalding.WILSON_BALDING,
                getWeightAttribute(operator.weight));
        writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
        // not supported anymore. probably never worked. (todo) get it out of GUI too
//        if (options.nodeHeightPrior == TreePrior.CONSTANT) {
//            treePriorGenerator.writeNodeHeightPriorModelRef(writer);
//        }
        writer.writeCloseTag(WilsonBalding.WILSON_BALDING);
    }

    private void writeSampleNonActiveOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(SampleNonActiveGibbsOperator.SAMPLE_NONACTIVE_GIBBS_OPERATOR,
                getWeightAttribute(operator.weight));

        writer.writeOpenTag(SampleNonActiveGibbsOperator.DISTRIBUTION);
        writeParameterRefByName(writer, operator.getName());
        writer.writeCloseTag(SampleNonActiveGibbsOperator.DISTRIBUTION);

        writer.writeOpenTag(SampleNonActiveGibbsOperator.DATA_PARAMETER);
        writeParameter1Ref(writer, operator);
        writer.writeCloseTag(SampleNonActiveGibbsOperator.DATA_PARAMETER);

        writer.writeOpenTag(SampleNonActiveGibbsOperator.INDICATOR_PARAMETER);
        writeParameterRefByName(writer, operator.parameter2.getName());
        writer.writeCloseTag(SampleNonActiveGibbsOperator.INDICATOR_PARAMETER);

        writer.writeCloseTag(SampleNonActiveGibbsOperator.SAMPLE_NONACTIVE_GIBBS_OPERATOR);
    }

    private void writeGMRFGibbsOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(
                GMRFSkyrideBlockUpdateOperator.BLOCK_UPDATE_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<Double>(GMRFSkyrideBlockUpdateOperator.SCALE_FACTOR, operator.tuning),
                        getWeightAttribute(operator.weight)
                }
        );
        writer.writeTag(GMRFSkyrideLikelihood.SKYLINE_LIKELIHOOD,
                new Attribute.Default<String>(XMLParser.IDREF, "skyride"), true);
        writer.writeCloseTag(GMRFSkyrideBlockUpdateOperator.BLOCK_UPDATE_OPERATOR);
    }

    private void writeScaleWithIndicatorsOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(
                ScaleOperator.SCALE_OPERATOR,
                new Attribute[]{
                        new Attribute.Default<Double>("scaleFactor", operator.tuning),
                        getWeightAttribute(operator.weight)
                });
        writeParameter1Ref(writer, operator);
        writer.writeOpenTag(ScaleOperator.INDICATORS, new Attribute.Default<String>(ScaleOperator.PICKONEPROB, "1.0"));
        writeParameterRefByName(writer, operator.parameter2.getName());
        writer.writeCloseTag(ScaleOperator.INDICATORS);
        writer.writeCloseTag(ScaleOperator.SCALE_OPERATOR);
    }

    private void writeSubtreeSlideOperator(Operator operator, XMLWriter writer) {
        writer.writeOpenTag(SubtreeSlideOperator.SUBTREE_SLIDE,
                new Attribute[]{
                        new Attribute.Default<Double>("size", operator.tuning),
                        new Attribute.Default<String>("gaussian", "true"),
                        getWeightAttribute(operator.weight)
                }
        );
        writer.writeTag(TreeModel.TREE_MODEL, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "treeModel")}, true);
        writer.writeCloseTag(SubtreeSlideOperator.SUBTREE_SLIDE);
    }

    private Attribute getWeightAttribute(double weight) {
        if (weight == (int)weight) {
            return new Attribute.Default<Integer>("weight", (int)weight);
        } else {
            return new Attribute.Default<Double>("weight", weight);
        }
    }
    /**
     * Write the timer report block.
     *
     * @param writer the writer
     */
    public void writeTimerReport(XMLWriter writer) {
        writer.writeOpenTag("report");
        writer.writeOpenTag("property", new Attribute.Default<String>("name", "timer"));
        writer.writeTag("object", new Attribute.Default<String>(XMLParser.IDREF, "mcmc"), true);
        writer.writeCloseTag("property");
        writer.writeCloseTag("report");
    }

    /**
     * Write the trace analysis block.
     *
     * @param writer the writer
     */
    public void writeTraceAnalysis(XMLWriter writer) {
        writer.writeTag(
                "traceAnalysis",
                new Attribute[]{
                        new Attribute.Default<String>("fileName", options.logFileName)
                },
                true
        );
    }

    /**
     * Write the MCMC block.
     *
     * @param writer the writer
     */
    public void writeMCMC(List<PartitionModel> models, boolean traitsContainSpecies, XMLWriter writer) {
    	writer.writeOpenTag(
                "mcmc",
                new Attribute[]{
                        new Attribute.Default<String>(XMLParser.ID, "mcmc"),
                        new Attribute.Default<Integer>("chainLength", options.chainLength),
                        new Attribute.Default<String>("autoOptimize", options.autoOptimize ? "true" : "false")
                });

        if (options.hasData()) {
            writer.writeOpenTag(CompoundLikelihood.POSTERIOR, new Attribute.Default<String>(XMLParser.ID, "posterior"));
        }

        // write prior block
        writer.writeOpenTag(CompoundLikelihood.PRIOR, new Attribute.Default<String>(XMLParser.ID, "prior"));

        writeParameterPriors(writer);

        switch (options.nodeHeightPrior) {

            case YULE:
            case BIRTH_DEATH:
                writer.writeTag(SpeciationLikelihood.SPECIATION_LIKELIHOOD,
                        new Attribute.Default<String>(XMLParser.IDREF, "speciation"), true);
                break;
            case SKYLINE:
                writer.writeTag(BayesianSkylineLikelihood.SKYLINE_LIKELIHOOD,
                        new Attribute.Default<String>(XMLParser.IDREF, "skyline"), true);
                writer.writeTag(ExponentialMarkovModel.EXPONENTIAL_MARKOV_MODEL, new Attribute.Default<String>(XMLParser.IDREF, "eml1"), true);
                break;
            case GMRF_SKYRIDE:
                writer.writeTag(GMRFSkyrideLikelihood.SKYLINE_LIKELIHOOD,
                        new Attribute.Default<String>(XMLParser.IDREF, "skyride"), true);
                break;
            case LOGISTIC:
                writer.writeTag(BooleanLikelihood.BOOLEAN_LIKELIHOOD,
                        new Attribute.Default<String>(XMLParser.IDREF, "booleanLikelihood1"), true);
            default:
            	if (traitsContainSpecies) { // species
            		for (PartitionModel model : models) {
            			writer.writeTag(CoalescentLikelihood.COALESCENT_LIKELIHOOD, new Attribute.Default<String>
            						(XMLParser.IDREF, model.getName() + "." + "coalescent"), true);
            		}
            	} else { // no species
            		writer.writeTag(CoalescentLikelihood.COALESCENT_LIKELIHOOD, new Attribute.Default<String>
            						(XMLParser.IDREF, "coalescent"), true);
            	}
        }

        if (options.nodeHeightPrior == TreePrior.EXTENDED_SKYLINE) {
            writer.writeOpenTag(MixedDistributionLikelihood.DISTRIBUTION_LIKELIHOOD);

            writer.writeOpenTag(MixedDistributionLikelihood.DISTRIBUTION0);
            writer.writeTag(ExponentialDistributionModel.EXPONENTIAL_DISTRIBUTION_MODEL,
                    new Attribute.Default<String>(XMLParser.IDREF, "demographic.populationMeanDist"), true);
            writer.writeCloseTag(MixedDistributionLikelihood.DISTRIBUTION0);

            writer.writeOpenTag(MixedDistributionLikelihood.DISTRIBUTION1);
            writer.writeTag(ExponentialDistributionModel.EXPONENTIAL_DISTRIBUTION_MODEL,
                    new Attribute.Default<String>(XMLParser.IDREF, "demographic.populationMeanDist"), true);
            writer.writeCloseTag(MixedDistributionLikelihood.DISTRIBUTION1);

            writer.writeOpenTag(MixedDistributionLikelihood.DATA);
            writer.writeTag(ParameterParser.PARAMETER,
                    new Attribute.Default<String>(XMLParser.IDREF, "demographic.popSize"), true);
            writer.writeCloseTag(MixedDistributionLikelihood.DATA);

            writer.writeOpenTag(MixedDistributionLikelihood.INDICATORS);
            writer.writeTag(ParameterParser.PARAMETER,
                    new Attribute.Default<String>(XMLParser.IDREF, "demographic.indicators"), true);
            writer.writeCloseTag(MixedDistributionLikelihood.INDICATORS);

            writer.writeCloseTag(MixedDistributionLikelihood.DISTRIBUTION_LIKELIHOOD);
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_MCMC_PRIOR, writer);

        writer.writeCloseTag(CompoundLikelihood.PRIOR);

        if (options.hasData()) {
            // write likelihood block
            writer.writeOpenTag(CompoundLikelihood.LIKELIHOOD, new Attribute.Default<String>(XMLParser.ID, "likelihood"));

            treeLikelihoodGenerator.writeTreeLikelihoodReferences(writer);

            if (options.clockType == ClockType.AUTOCORRELATED_LOGNORMAL) {
                writer.writeTag(ACLikelihood.AC_LIKELIHOOD,
                        new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES), true);
            }

            generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_MCMC_LIKELIHOOD, writer);

            writer.writeCloseTag(CompoundLikelihood.LIKELIHOOD);


            writer.writeCloseTag(CompoundLikelihood.POSTERIOR);
        }

        writer.writeTag(SimpleOperatorSchedule.OPERATOR_SCHEDULE, new Attribute.Default<String>(XMLParser.IDREF, "operators"), true);

        // write log to screen
        writer.writeOpenTag(LoggerParser.LOG,
                new Attribute[]{
                        new Attribute.Default<String>(XMLParser.ID, "screenLog"),
                        new Attribute.Default<String>(LoggerParser.LOG_EVERY, options.echoEvery + "")
                });
        writeScreenLog(writer);

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_SCREEN_LOG, writer);

        writer.writeCloseTag(LoggerParser.LOG);

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_SCREEN_LOG, writer);

        // write log to file
        if (options.logFileName == null) {
            options.logFileName = options.fileNameStem + ".log";
        }
        writer.writeOpenTag(LoggerParser.LOG,
                new Attribute[]{
                        new Attribute.Default<String>(XMLParser.ID, "fileLog"),
                        new Attribute.Default<String>(LoggerParser.LOG_EVERY, options.logEvery + ""),
                        new Attribute.Default<String>(LoggerParser.FILE_NAME, options.logFileName)
                });
        writeLog(writer);

        writer.writeCloseTag(LoggerParser.LOG);

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_FILE_LOG, writer);

        // write tree log to file
        if (options.treeFileName == null) {
            if (options.substTreeLog) {
                options.treeFileName = options.fileNameStem + "(time).trees";
            } else {
                options.treeFileName = options.fileNameStem + ".trees";
            }
        }
        writer.writeOpenTag(TreeLoggerParser.LOG_TREE,
                new Attribute[]{
                        new Attribute.Default<String>(XMLParser.ID, "treeFileLog"),
                        new Attribute.Default<String>(TreeLoggerParser.LOG_EVERY, options.logEvery + ""),
                        new Attribute.Default<String>(TreeLoggerParser.NEXUS_FORMAT, "true"),
                        new Attribute.Default<String>(TreeLoggerParser.FILE_NAME, options.treeFileName),
                        new Attribute.Default<String>(TreeLoggerParser.SORT_TRANSLATION_TABLE, "true")
                });

    	if (traitsContainSpecies) { // species
    		for (PartitionModel model : models) {
    			writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>
    						(XMLParser.IDREF, model.getName() + "." + TreeModel.TREE_MODEL), true);

    		}
    	} else { // no species
			writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>
						(XMLParser.IDREF, TreeModel.TREE_MODEL), true);
    	}


        switch (options.clockType) {
            case STRICT_CLOCK:
                break;

            case UNCORRELATED_EXPONENTIAL:
            case UNCORRELATED_LOGNORMAL:
            case RANDOM_LOCAL_CLOCK:
                writer.writeTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
                break;

            case AUTOCORRELATED_LOGNORMAL:
                writer.writeTag(ACLikelihood.AC_LIKELIHOOD, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
                break;

            default:
                throw new IllegalArgumentException("Unknown clock model");
        }

        /*if (options.clockType != ClockType.STRICT_CLOCK) {
            writer.writeTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
        }*/

        if (options.hasData()) {
            // we have data...
            writer.writeTag("posterior", new Attribute.Default<String>(XMLParser.IDREF, "posterior"), true);
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_TREES_LOG, writer);

        writer.writeCloseTag(TreeLoggerParser.LOG_TREE);

//        if (mapTreeLog) {
//            // write tree log to file
//            if (mapTreeFileName == null) {
//                mapTreeFileName = fileNameStem + ".MAP.tree";
//            }
//            writer.writeOpenTag("logML",
//                    new Attribute[] {
//                        new Attribute.Default<String>(TreeLogger.FILE_NAME, mapTreeFileName)
//                    });
//            writer.writeOpenTag("ml");
//            writer.writeTag(CompoundLikelihood.POSTERIOR, new Attribute.Default<String>(XMLParser.IDREF, "posterior"), true);
//            writer.writeCloseTag("ml");
//            writer.writeOpenTag("column", new Attribute[] {
//                        new Attribute.Default<String>("label", "MAP tree")
//                    });
//            writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>(XMLParser.IDREF, "treeModel"), true);
//            writer.writeCloseTag("column");
//            writer.writeCloseTag("logML");
//        }

        if (options.substTreeLog) {
            // write tree log to file
            if (options.substTreeFileName == null) {
                options.substTreeFileName = options.fileNameStem + "(subst).trees";
            }
            writer.writeOpenTag(TreeLoggerParser.LOG_TREE,
                    new Attribute[]{
                            new Attribute.Default<String>(XMLParser.ID, "substTreeFileLog"),
                            new Attribute.Default<String>(TreeLoggerParser.LOG_EVERY, options.logEvery + ""),
                            new Attribute.Default<String>(TreeLoggerParser.NEXUS_FORMAT, "true"),
                            new Attribute.Default<String>(TreeLoggerParser.FILE_NAME, options.substTreeFileName),
                            new Attribute.Default<String>(TreeLoggerParser.BRANCH_LENGTHS, TreeLoggerParser.SUBSTITUTIONS)
                    });
            writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>(XMLParser.IDREF, "treeModel"), true);

            switch (options.clockType) {
                case STRICT_CLOCK:
                    writer.writeTag(StrictClockBranchRates.STRICT_CLOCK_BRANCH_RATES, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
                    break;

                case UNCORRELATED_EXPONENTIAL:
                case UNCORRELATED_LOGNORMAL:
                case RANDOM_LOCAL_CLOCK:
                    writer.writeTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
                    break;

                case AUTOCORRELATED_LOGNORMAL:
                    writer.writeTag(ACLikelihood.AC_LIKELIHOOD, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, BranchRateModel.BRANCH_RATES)}, true);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown clock model");
            }


            writer.writeCloseTag(TreeLoggerParser.LOG_TREE);
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.AFTER_TREES_LOG, writer);

        writer.writeCloseTag("mcmc");
    }

    /**
     * Write the priors for each parameter
     *
     * @param writer the writer
     */
    private void writeParameterPriors(XMLWriter writer) {
        boolean first = true;
        for (Taxa taxa : options.taxonSetsMono.keySet()) {
            if (options.taxonSetsMono.get(taxa)) {
                if (first) {
                    writer.writeOpenTag(BooleanLikelihood.BOOLEAN_LIKELIHOOD);
                    first = false;
                }
                final String taxaRef = "monophyly(" + taxa.getId() + ")";
                final Attribute.Default attr = new Attribute.Default<String>(XMLParser.IDREF, taxaRef);
                writer.writeTag(MonophylyStatistic.MONOPHYLY_STATISTIC, new Attribute[]{attr}, true);
            }
        }
        if (!first) {
            writer.writeCloseTag(BooleanLikelihood.BOOLEAN_LIKELIHOOD);
        }

        ArrayList<dr.app.beauti.options.Parameter> parameters = options.selectParameters();
        for (dr.app.beauti.options.Parameter parameter : parameters) {
            if (parameter.priorType != PriorType.NONE) {
                if (parameter.priorType != PriorType.UNIFORM_PRIOR || parameter.isNodeHeight) {
                    writeParameterPrior(parameter, writer);
                }
            }
        }

    }

    /**
     * Write the priors for each parameter
     *
     * @param parameter the parameter
     * @param writer    the writer
     */
    private void writeParameterPrior(dr.app.beauti.options.Parameter parameter, XMLWriter writer) {
        switch (parameter.priorType) {
            case UNIFORM_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.UNIFORM_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.LOWER, "" + parameter.uniformLower),
                                new Attribute.Default<String>(DistributionLikelihood.UPPER, "" + parameter.uniformUpper)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.UNIFORM_PRIOR);
                break;
            case EXPONENTIAL_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.EXPONENTIAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.MEAN, "" + parameter.exponentialMean),
                                new Attribute.Default<String>(DistributionLikelihood.OFFSET, "" + parameter.exponentialOffset)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.EXPONENTIAL_PRIOR);
                break;
            case NORMAL_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.NORMAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.MEAN, "" + parameter.normalMean),
                                new Attribute.Default<String>(DistributionLikelihood.STDEV, "" + parameter.normalStdev)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.NORMAL_PRIOR);
                break;
            case LOGNORMAL_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.LOG_NORMAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.MEAN, "" + parameter.logNormalMean),
                                new Attribute.Default<String>(DistributionLikelihood.STDEV, "" + parameter.logNormalStdev),
                                new Attribute.Default<String>(DistributionLikelihood.OFFSET, "" + parameter.logNormalOffset),

                                // this is to be implemented...
                                new Attribute.Default<String>(DistributionLikelihood.MEAN_IN_REAL_SPACE, "false")
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.LOG_NORMAL_PRIOR);
                break;
            case GAMMA_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.GAMMA_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.SHAPE, "" + parameter.gammaAlpha),
                                new Attribute.Default<String>(DistributionLikelihood.SCALE, "" + parameter.gammaBeta),
                                new Attribute.Default<String>(DistributionLikelihood.OFFSET, "" + parameter.gammaOffset)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.GAMMA_PRIOR);
                break;
            case JEFFREYS_PRIOR:
                writer.writeOpenTag(JeffreysPriorLikelihood.JEFFREYS_PRIOR);
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(JeffreysPriorLikelihood.JEFFREYS_PRIOR);
                break;
            case POISSON_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.POISSON_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.MEAN, "" + parameter.poissonMean),
                                new Attribute.Default<String>(DistributionLikelihood.OFFSET, "" + parameter.poissonOffset)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.POISSON_PRIOR);
                break;
            case TRUNC_NORMAL_PRIOR:
                writer.writeOpenTag(DistributionLikelihood.UNIFORM_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.LOWER, "" + parameter.uniformLower),
                                new Attribute.Default<String>(DistributionLikelihood.UPPER, "" + parameter.uniformUpper)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.UNIFORM_PRIOR);
                writer.writeOpenTag(DistributionLikelihood.NORMAL_PRIOR,
                        new Attribute[]{
                                new Attribute.Default<String>(DistributionLikelihood.MEAN, "" + parameter.normalMean),
                                new Attribute.Default<String>(DistributionLikelihood.STDEV, "" + parameter.normalStdev)
                        });
                writeParameterIdref(writer, parameter);
                writer.writeCloseTag(DistributionLikelihood.NORMAL_PRIOR);
                break;
            default:
                throw new IllegalArgumentException("Unknown priorType");
        }
    }

    private void writeParameterIdref(XMLWriter writer, dr.app.beauti.options.Parameter parameter) {
        if (parameter.isStatistic) {
            writer.writeTag("statistic", new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, parameter.getName())}, true);
        } else {
            writer.writeTag(ParameterParser.PARAMETER, new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, parameter.getName())}, true);
        }
    }

    /**
     * Write the log
     *
     * @param writer the writer
     */
    private void writeScreenLog(XMLWriter writer) {
        if (options.hasData()) {
            writer.writeOpenTag(Columns.COLUMN,
                    new Attribute[]{
                            new Attribute.Default<String>(Columns.LABEL, "Posterior"),
                            new Attribute.Default<String>(Columns.DECIMAL_PLACES, "4"),
                            new Attribute.Default<String>(Columns.WIDTH, "12")
                    }
            );
            writer.writeTag(CompoundLikelihood.POSTERIOR, new Attribute.Default<String>(XMLParser.IDREF, "posterior"), true);
            writer.writeCloseTag(Columns.COLUMN);
        }

        writer.writeOpenTag(Columns.COLUMN,
                new Attribute[]{
                        new Attribute.Default<String>(Columns.LABEL, "Prior"),
                        new Attribute.Default<String>(Columns.DECIMAL_PLACES, "4"),
                        new Attribute.Default<String>(Columns.WIDTH, "12")
                }
        );
        writer.writeTag(CompoundLikelihood.PRIOR, new Attribute.Default<String>(XMLParser.IDREF, "prior"), true);
        writer.writeCloseTag(Columns.COLUMN);

        if (options.hasData()) {
            writer.writeOpenTag(Columns.COLUMN,
                    new Attribute[]{
                            new Attribute.Default<String>(Columns.LABEL, "Likelihood"),
                            new Attribute.Default<String>(Columns.DECIMAL_PLACES, "4"),
                            new Attribute.Default<String>(Columns.WIDTH, "12")
                    }
            );
            writer.writeTag(CompoundLikelihood.LIKELIHOOD, new Attribute.Default<String>(XMLParser.IDREF, "likelihood"), true);
            writer.writeCloseTag(Columns.COLUMN);
        }

        writer.writeOpenTag(Columns.COLUMN,
                new Attribute[]{
                        new Attribute.Default<String>(Columns.LABEL, "Root Height"),
                        new Attribute.Default<String>(Columns.SIGNIFICANT_FIGURES, "6"),
                        new Attribute.Default<String>(Columns.WIDTH, "12")
                }
        );
        writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "treeModel.rootHeight"), true);
        writer.writeCloseTag(Columns.COLUMN);

        writer.writeOpenTag(Columns.COLUMN,
                new Attribute[]{
                        new Attribute.Default<String>(Columns.LABEL, "Rate"),
                        new Attribute.Default<String>(Columns.SIGNIFICANT_FIGURES, "6"),
                        new Attribute.Default<String>(Columns.WIDTH, "12")
                }
        );
        if (options.clockType == ClockType.STRICT_CLOCK) {
            writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "clock.rate"), true);
        } else {
            writer.writeTag(RateStatistic.RATE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "meanRate"), true);
        }


        writer.writeCloseTag(Columns.COLUMN);

        if (options.clockType == ClockType.RANDOM_LOCAL_CLOCK) {
            writeSumStatisticColumn(writer, "rateChanges", "Rate Changes");
        }
    }

    /**
     * Write the log
     *
     * @param writer the writer
     */
    private void writeLog(XMLWriter writer) {
        if (options.hasData()) {
            writer.writeTag(CompoundLikelihood.POSTERIOR, new Attribute.Default<String>(XMLParser.IDREF, "posterior"), true);
        }
        writer.writeTag(CompoundLikelihood.PRIOR, new Attribute.Default<String>(XMLParser.IDREF, "prior"), true);
        if (options.hasData()) {
            writer.writeTag(CompoundLikelihood.LIKELIHOOD, new Attribute.Default<String>(XMLParser.IDREF, "likelihood"), true);
        }

        if (options.clockType == ClockType.STRICT_CLOCK) {
            writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "clock.rate"), true);
        } else {
            writer.writeTag(RateStatistic.RATE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "meanRate"), true);
        }

        writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "treeModel.rootHeight"), true);

        for (Taxa taxa : options.taxonSets) {
            writer.writeTag("tmrcaStatistic", new Attribute[]{new Attribute.Default<String>(XMLParser.IDREF, "tmrca(" + taxa.getId() + ")")}, true);
        }

        treePriorGenerator.writeParameterLog(writer);

        for (PartitionModel model : options.getActivePartitionModels()) {
            partitionModelGenerator.writeLog(writer, model);
        }
        if (hasCodonOrUserPartitions()) {
            writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "allMus"), true);
        }

        switch (options.clockType) {
            case STRICT_CLOCK:
                break;

            case UNCORRELATED_EXPONENTIAL:
                writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, ClockType.UCED_MEAN), true);
                writer.writeTag(RateStatistic.RATE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "coefficientOfVariation"), true);
                writer.writeTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "covariance"), true);
                break;
            case UNCORRELATED_LOGNORMAL:
                writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, ClockType.UCLD_MEAN), true);
                writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, ClockType.UCLD_STDEV), true);
                writer.writeTag(RateStatistic.RATE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "coefficientOfVariation"), true);
                writer.writeTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "covariance"), true);
                break;

            case AUTOCORRELATED_LOGNORMAL:
                writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "treeModel.rootRate"), true);
                writer.writeTag(ParameterParser.PARAMETER, new Attribute.Default<String>(XMLParser.IDREF, "branchRates.var"), true);
                writer.writeTag(RateStatistic.RATE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "coefficientOfVariation"), true);
                writer.writeTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "covariance"), true);
                break;

            case RANDOM_LOCAL_CLOCK:
                writer.writeTag(RateStatistic.RATE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "coefficientOfVariation"), true);
                writer.writeTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC, new Attribute.Default<String>(XMLParser.IDREF, "covariance"), true);
                writer.writeTag("sumStatistic", new Attribute.Default<String>(XMLParser.IDREF, "rateChanges"), true);
                break;

            default:
                throw new IllegalArgumentException("Unknown clock model");
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_FILE_LOG_PARAMETERS, writer);

        if (options.hasData()) {
            treeLikelihoodGenerator.writeTreeLikelihoodReferences(writer);
        }

        generateInsertionPoint(ComponentGenerator.InsertionPoint.IN_FILE_LOG_LIKELIHOODS, writer);

        treePriorGenerator.writeLikelihoodLog(writer);
    }

    /**
     * @return true either if the options have more than one partition or any partition is
     *         broken into codon positions.
     */
    private boolean hasCodonOrUserPartitions() {
        return (options.getActivePartitionModels().size() > 1 || options.getActivePartitionModels().get(0).getCodonPartitionCount() > 1);
    }

}