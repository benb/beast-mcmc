/*
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

package dr.app.beauti.options;


import dr.app.beauti.enumTypes.OperatorType;
import dr.app.beauti.enumTypes.PriorScaleType;
import dr.app.beauti.enumTypes.TreePriorType;
import dr.app.beauti.generator.Generator;
import dr.evomodelxml.operators.TreeNodeSlideParser;
import dr.evomodelxml.speciation.BirthDeathModelParser;
import dr.evomodelxml.speciation.SpeciesTreeModelParser;
import dr.evomodelxml.speciation.YuleModelParser;

import java.util.List;


/**
 * @author Walter Xie
 * @version $Id$
 */
public class STARBEASTOptions extends ModelOptions {

	public static final String TREE_FILE_NAME = "trees";

    public final String POP_MEAN = "popMean";
    public final String SPECIES_TREE_FILE_NAME = TraitData.TRAIT_SPECIES
    							+ "." + STARBEASTOptions.TREE_FILE_NAME; // species.trees

    public static final String CITATION = "<html>Joseph Heled and Alexei J. Drummond,<br>" +
                "Bayesian Inference of Species Trees from Multilocus Data,<br>" +
                "Molecular Biology and Evolution 2010 27(3):570-580</html>";

    public static final String EXAMPLE_FORMAT = "<html>A proper trait file is tab delimited.<br>" +
            "The first row is always <b>traits</b> followed by the keyword (e.g. <b>species</b> in *BEAST)<br> in the <br>" +
            "second column and separated by tab. The rest rows are mapping taxa to species, which list taxon <br>" +
            "name in the first column and species name in the second column separated by tab. For example: <br>" +
            "traits\tspecies<br>" +
            "taxon1\tspeciesA<br>" +
            "taxon2\tspeciesA<br>" +
            "taxon3\tspeciesB<br>" +
            "... ...<br>" +
            "Once mapping file is loaded, the trait named by keyword \"species\" is displayed in the main panel,<br>" +
            "and the message of using *BEAST is also displayed on the bottom of main frame.<br>" +
            "For multi-alignment, the default of *BEAST is unlinking all models: substitution model, clock model,<br>" +
            "and tree models.</html>";

    private final BeautiOptions options;

    public STARBEASTOptions(BeautiOptions options) {
        this.options = options;
        initTraitParametersAndOperators();
    }

    protected void initTraitParametersAndOperators() {
        double spWeights = 5.0;
        double spTuning = 0.9;

        createParameterJeffreysPrior(TraitData.TRAIT_SPECIES + "." + POP_MEAN, "Species tree: population hyper-parameter operator",
        		PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        // species tree Yule
        createParameterJeffreysPrior(TraitData.TRAIT_SPECIES + "." + YuleModelParser.YULE + "." + YuleModelParser.BIRTH_RATE,
                "Speices tree: Yule process birth rate", PriorScaleType.BIRTH_RATE_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);

        // species tree Birth Death
        createParameterJeffreysPrior(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME,
                "Speices tree: Birth Death model mean growth rate", PriorScaleType.BIRTH_RATE_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME,
                "Speices tree: Birth Death model relative death rate", PriorScaleType.BIRTH_RATE_SCALE, 0.5, 0.0, 1.0);

        createParameterJeffreysPrior(SpeciesTreeModelParser.SPECIES_TREE + "." + Generator.SPLIT_POPS, "Species tree: population size operator",
        		PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);

        createParameter(TraitData.TRAIT_SPECIES + "." + TreeNodeSlideParser.TREE_NODE_REHEIGHT, "Species tree: tree node operator");

        createScaleOperator(TraitData.TRAIT_SPECIES + "." + POP_MEAN, spTuning, spWeights);

        createScaleOperator(TraitData.TRAIT_SPECIES + "." + YuleModelParser.YULE + "." + YuleModelParser.BIRTH_RATE, demoTuning, demoWeights);
        createScaleOperator(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME, demoTuning, demoWeights);
        createScaleOperator(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME, demoTuning, demoWeights);

        createScaleOperator(SpeciesTreeModelParser.SPECIES_TREE + "." + Generator.SPLIT_POPS, 0.5, 94);

        createOperator(TraitData.TRAIT_SPECIES + "." + TreeNodeSlideParser.TREE_NODE_REHEIGHT, OperatorType.NODE_REHIGHT, demoTuning, 94);

        //TODO: more

        for (PartitionClockModel model : options.getPartitionClockModels()) {
            model.iniClockRateStarBEAST();
        }

    }

    /**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectParameters(List<Parameter> params) {

        params.add(getParameter(TraitData.TRAIT_SPECIES + "." + POP_MEAN));

        if (options.getPartitionTreePriors().get(0).getNodeHeightPrior() == TreePriorType.SPECIES_BIRTH_DEATH) {
            params.add(getParameter(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME));
            params.add(getParameter(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        } else if (options.getPartitionTreePriors().get(0).getNodeHeightPrior() == TreePriorType.SPECIES_YULE) {
            params.add(getParameter(TraitData.TRAIT_SPECIES + "." + YuleModelParser.YULE + "." + YuleModelParser.BIRTH_RATE));
        }

//    	params.add(getParameter(SpeciesTreeModel.SPECIES_TREE + "." + Generator.SPLIT_POPS));

        //TODO: more

    }

    /**
     * return a list of operators that are required
     *
     * @param ops the operator list
     */
    public void selectOperators(List<Operator> ops) {
        ops.add(getOperator(TraitData.TRAIT_SPECIES + "." + POP_MEAN));

        if (options.getPartitionTreePriors().get(0).getNodeHeightPrior() == TreePriorType.SPECIES_BIRTH_DEATH) {
            ops.add(getOperator(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME));
            ops.add(getOperator(TraitData.TRAIT_SPECIES + "." + BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
            
//            ops.add(getOperator("upDownBirthDeathSpeciesTree"));
//            ops.add(getOperator("upDownBirthDeathSTPop"));
//            
//            for (PartitionTreeModel tree : getPartitionTreeModels()) {
//            	ops.add(getOperator(tree.getPrefix() + "upDownBirthDeathGeneTree"));
//            }
        } else if (options.getPartitionTreePriors().get(0).getNodeHeightPrior() == TreePriorType.SPECIES_YULE) {
            ops.add(getOperator(TraitData.TRAIT_SPECIES + "." + YuleModelParser.YULE + "." + YuleModelParser.BIRTH_RATE));
            
//            ops.add(getOperator("upDownYuleSpeciesTree"));
//            ops.add(getOperator("upDownYuleSTPop"));
//            
//            for (PartitionTreeModel tree : getPartitionTreeModels()) {
//            	ops.add(getOperator(tree.getPrefix() + "upDownYuleGeneTree"));
//            }
        }

        ops.add(getOperator(SpeciesTreeModelParser.SPECIES_TREE + "." + Generator.SPLIT_POPS));

        ops.add(getOperator(TraitData.TRAIT_SPECIES + "." + TreeNodeSlideParser.TREE_NODE_REHEIGHT));
        //TODO: more
    }
    
    /////////////////////////////////////////////////////////////

    public List<String> getSpeciesList() {
        return TraitData.getStatesListOfTrait(options.taxonList, TraitData.TRAIT_SPECIES.toString());
    }

    public String getDescription() {
        return "Species definition: binds taxa, species and gene trees";
    }

}
