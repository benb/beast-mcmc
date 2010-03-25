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

import dr.app.beauti.enumTypes.*;
import dr.evomodel.coalescent.VariableDemographicModel;
import dr.evomodelxml.speciation.BirthDeathModelParser;

import java.util.List;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 */
public class PartitionTreePrior extends PartitionOptions {

    // Instance variables

    private final BeautiOptions options;

    private PartitionTreeModel treeModel; // only used when not sharing same prior

    private TreePriorType nodeHeightPrior = TreePriorType.CONSTANT;
    private TreePriorParameterizationType parameterization = TreePriorParameterizationType.GROWTH_RATE;
    private int skylineGroupCount = 10;
    private TreePriorParameterizationType skylineModel = TreePriorParameterizationType.CONSTANT_SKYLINE;
    private TreePriorParameterizationType skyrideSmoothing = TreePriorParameterizationType.TIME_AWARE_SKYRIDE;
    // AR - this seems to be set to taxonCount - 1 so we don't need to
    // have a settable variable...
    // public int skyrideIntervalCount = 1;
    private VariableDemographicModel.Type extendedSkylineModel = VariableDemographicModel.Type.LINEAR;
    private double birthDeathSamplingProportion = 1.0;
    private PopulationSizeModelType populationSizeModel = PopulationSizeModelType.CONTINUOUS_CONSTANT;
    private boolean fixedTree = false;

    public PartitionTreePrior(BeautiOptions options, PartitionTreeModel treeModel) {
        this.options = options;
        this.partitionName = treeModel.getName();
        this.treeModel = treeModel;

        initTreePriorParaAndOpers();
    }

    /**
     * A copy constructor
     *
     * @param options the beauti options
     * @param name    the name of the new model
     * @param source  the source model
     */
    public PartitionTreePrior(BeautiOptions options, String name, PartitionTreePrior source) {
        this.options = options;
        this.partitionName = name;
        this.treeModel = source.treeModel;

        this.nodeHeightPrior = source.nodeHeightPrior;
        this.parameterization = source.parameterization;
        this.skylineGroupCount = source.skylineGroupCount;
        this.skylineModel = source.skylineModel;
        this.skyrideSmoothing = source.skyrideSmoothing;
        this.birthDeathSamplingProportion = source.birthDeathSamplingProportion;
        this.fixedTree = source.fixedTree;

        initTreePriorParaAndOpers();
    }

    private void initTreePriorParaAndOpers() {
       
        createParameterJeffreysPrior("constant.popSize", "coalescent population size parameter",
                PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);

        createParameterJeffreysPrior("exponential.popSize", "coalescent population size parameter",
                PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("exponential.growthRate", "coalescent growth rate parameter",
                PriorScaleType.GROWTH_RATE_SCALE, 0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("exponential.doublingTime", "coalescent doubling time parameter",
                PriorScaleType.TIME_SCALE, 0.5, 0.0, Double.POSITIVE_INFINITY);

        createParameterJeffreysPrior("logistic.popSize", "coalescent population size parameter",
                PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("logistic.growthRate", "coalescent logistic growth rate parameter",
                PriorScaleType.GROWTH_RATE_SCALE, 0.001, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("logistic.doublingTime", "coalescent doubling time parameter",
                PriorScaleType.TIME_SCALE, 0.5, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("logistic.t50", "logistic shape parameter",
                PriorScaleType.T50_SCALE, 0.1, 0.0, Double.POSITIVE_INFINITY);

        createParameterJeffreysPrior("expansion.popSize", "coalescent population size parameter",
                PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("expansion.growthRate", "coalescent logistic growth rate parameter",
                PriorScaleType.GROWTH_RATE_SCALE, 0.001, 0.0, Double.POSITIVE_INFINITY);
        createParameterJeffreysPrior("expansion.doublingTime", "coalescent doubling time parameter",
                PriorScaleType.TIME_SCALE, 0.5, 0.0001, Double.POSITIVE_INFINITY);
        createParameterUniformPrior("expansion.ancestralProportion", "ancestral population proportion",
                PriorScaleType.NONE, 0.1, 0.0, 1.0);

        createParameterUniformPrior("skyline.popSize", "Bayesian Skyline population sizes",
                PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("skyline.groupSize", "Bayesian Skyline group sizes");
        // skyride.logPopSize is log unit unlike other popSize
        createParameterUniformPrior("skyride.logPopSize", "GMRF Bayesian skyride population sizes (log unit)",
                PriorScaleType.TIME_SCALE, 1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        createParameter("skyride.groupSize", "GMRF Bayesian skyride group sizes (for backward compatibility)");
        createParameterGammaPrior("skyride.precision", "GMRF Bayesian skyride precision",
                PriorScaleType.NONE, 1.0, 0.001, 1000, 0.0, Double.POSITIVE_INFINITY, true);

        createParameterUniformPrior("demographic.popSize", "Extended Bayesian Skyline population sizes",
                PriorScaleType.TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("demographic.indicators", "Extended Bayesian Skyline population switch", 0.0);
        createParameterJeffreysPrior("demographic.populationMean", "Extended Bayesian Skyline population prior mean",
                PriorScaleType.TIME_SCALE, 1, 0, Double.POSITIVE_INFINITY);

        createDiscreteStatistic("demographic.populationSizeChanges", "Average number of population change points"); // POISSON_PRIOR

        createParameterUniformPrior("yule.birthRate", "Yule speciation process birth rate",
                PriorScaleType.BIRTH_RATE_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);

        createParameterUniformPrior(BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME, "Birth-Death speciation process rate",
                PriorScaleType.BIRTH_RATE_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameterUniformPrior(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME, "Death/Birth speciation process relative death rate",
                PriorScaleType.BIRTH_RATE_SCALE, 0.5, 0.0, Double.POSITIVE_INFINITY);

        createScaleOperator("constant.popSize", demoTuning, demoWeights);
        createScaleOperator("exponential.popSize", demoTuning, demoWeights);
        createOperator("exponential.growthRate", OperatorType.RANDOM_WALK, 1.0, demoWeights);
        createScaleOperator("exponential.doublingTime", demoTuning, demoWeights);
        createScaleOperator("logistic.popSize", demoTuning, demoWeights);
        createScaleOperator("logistic.growthRate", demoTuning, demoWeights);
        createScaleOperator("logistic.doublingTime", demoTuning, demoWeights);
        createScaleOperator("logistic.t50", demoTuning, demoWeights);
        createScaleOperator("expansion.popSize", demoTuning, demoWeights);
        createScaleOperator("expansion.growthRate", demoTuning, demoWeights);
        createScaleOperator("expansion.doublingTime", demoTuning, demoWeights);
        createScaleOperator("expansion.ancestralProportion", demoTuning, demoWeights);
        createScaleOperator("skyline.popSize", demoTuning, demoWeights * 5);
        createOperator("skyline.groupSize", OperatorType.INTEGER_DELTA_EXCHANGE, 1.0, demoWeights * 2);
        createOperator("demographic.populationMean", OperatorType.SCALE, 0.9, demoWeights);
        createOperator("demographic.indicators", OperatorType.BITFLIP, 1, 2 * treeWeights);

        // hack pass distribution in name
        createOperatorUsing2Parameters("demographic.popSize", "demographic.populationMeanDist", "", "demographic.popSize",
        		"demographic.indicators", OperatorType.SAMPLE_NONACTIVE, 1, 5 * demoWeights);
        createOperatorUsing2Parameters("demographic.scaleActive", "demographic.scaleActive", "", "demographic.popSize",
        		"demographic.indicators", OperatorType.SCALE_WITH_INDICATORS, 0.5, 2 * demoWeights);
        createOperatorUsing2Parameters("gmrfGibbsOperator", "gmrfGibbsOperator", "Gibbs sampler for GMRF", "skyride.logPopSize",
        		"skyride.precision", OperatorType.GMRF_GIBBS_OPERATOR, 2, 2);

        createScaleOperator("yule.birthRate", demoTuning, demoWeights);

        createScaleOperator(BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME, demoTuning, demoWeights);
        createScaleOperator(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME, demoTuning, demoWeights);

    }

    /**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectParameters(List<Parameter> params) {

        if (nodeHeightPrior == TreePriorType.CONSTANT) {
            params.add(getParameter("constant.popSize"));
        } else if (nodeHeightPrior == TreePriorType.EXPONENTIAL) {
            params.add(getParameter("exponential.popSize"));
            if (parameterization == TreePriorParameterizationType.GROWTH_RATE) {
                params.add(getParameter("exponential.growthRate"));
            } else {
                params.add(getParameter("exponential.doublingTime"));
            }
        } else if (nodeHeightPrior == TreePriorType.LOGISTIC) {
            params.add(getParameter("logistic.popSize"));
            if (parameterization == TreePriorParameterizationType.GROWTH_RATE) {
                params.add(getParameter("logistic.growthRate"));
            } else {
                params.add(getParameter("logistic.doublingTime"));
            }
            params.add(getParameter("logistic.t50"));
        } else if (nodeHeightPrior == TreePriorType.EXPANSION) {
            params.add(getParameter("expansion.popSize"));
            if (parameterization == TreePriorParameterizationType.GROWTH_RATE) {
                params.add(getParameter("expansion.growthRate"));
            } else {
                params.add(getParameter("expansion.doublingTime"));
            }
            params.add(getParameter("expansion.ancestralProportion"));
        } else if (nodeHeightPrior == TreePriorType.SKYLINE) {
            params.add(getParameter("skyline.popSize"));
        } else if (nodeHeightPrior == TreePriorType.EXTENDED_SKYLINE) {
            params.add(getParameter("demographic.populationSizeChanges"));
            params.add(getParameter("demographic.populationMean"));
        } else if (nodeHeightPrior == TreePriorType.GMRF_SKYRIDE) {
//            params.add(getParameter("skyride.popSize")); // force user to use GMRF, not allowed to change
            params.add(getParameter("skyride.precision"));
        } else if (nodeHeightPrior == TreePriorType.YULE) {
            params.add(getParameter("yule.birthRate"));
        } else if (nodeHeightPrior == TreePriorType.BIRTH_DEATH) {
            params.add(getParameter(BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME));
            params.add(getParameter(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        }

    }

    /**
     * return a list of operators that are required
     *
     * @param ops the operator list
     */
    public void selectOperators(List<Operator> ops) {

        if (nodeHeightPrior == TreePriorType.CONSTANT) {
            ops.add(getOperator("constant.popSize"));
        } else if (nodeHeightPrior == TreePriorType.EXPONENTIAL) {
            ops.add(getOperator("exponential.popSize"));
            if (parameterization == TreePriorParameterizationType.GROWTH_RATE) {
                ops.add(getOperator("exponential.growthRate"));
            } else {
                ops.add(getOperator("exponential.doublingTime"));
            }
        } else if (nodeHeightPrior == TreePriorType.LOGISTIC) {
            ops.add(getOperator("logistic.popSize"));
            if (parameterization == TreePriorParameterizationType.GROWTH_RATE) {
                ops.add(getOperator("logistic.growthRate"));
            } else {
                ops.add(getOperator("logistic.doublingTime"));
            }
            ops.add(getOperator("logistic.t50"));
        } else if (nodeHeightPrior == TreePriorType.EXPANSION) {
            ops.add(getOperator("expansion.popSize"));
            if (parameterization == TreePriorParameterizationType.GROWTH_RATE) {
                ops.add(getOperator("expansion.growthRate"));
            } else {
                ops.add(getOperator("expansion.doublingTime"));
            }
            ops.add(getOperator("expansion.ancestralProportion"));
        } else if (nodeHeightPrior == TreePriorType.SKYLINE) {
            ops.add(getOperator("skyline.popSize"));
            ops.add(getOperator("skyline.groupSize"));
        } else if (nodeHeightPrior == TreePriorType.GMRF_SKYRIDE) {
            ops.add(getOperator("gmrfGibbsOperator"));
        } else if (nodeHeightPrior == TreePriorType.EXTENDED_SKYLINE) {
            ops.add(getOperator("demographic.populationMean"));
            ops.add(getOperator("demographic.popSize"));
            ops.add(getOperator("demographic.indicators"));
            ops.add(getOperator("demographic.scaleActive"));
        } else if (nodeHeightPrior == TreePriorType.YULE) {
            ops.add(getOperator("yule.birthRate"));
        } else if (nodeHeightPrior == TreePriorType.BIRTH_DEATH) {
            ops.add(getOperator(BirthDeathModelParser.MEAN_GROWTH_RATE_PARAM_NAME));
            ops.add(getOperator(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        }
    }


    //////////////////////////////////////////////////////
    public String getPrefix() {
        String prefix = "";
        if (options.getPartitionTreePriors().size() > 1) {//|| options.isSpeciesAnalysis()
            // There is more than one active partition model, or doing species analysis
            prefix += getName() + ".";
        }
        return prefix;
    }

    /////////////////////////////////////////////////////////////////////////

    public PartitionTreeModel getTreeModel() {
        return treeModel;
    }

//    public void setTreeModel(PartitionTreeModel treeModel) {
//        this.treeModel = treeModel;
//    }

    public TreePriorType getNodeHeightPrior() {
        return nodeHeightPrior;
    }

    public void setNodeHeightPrior(TreePriorType nodeHeightPrior) {
        this.nodeHeightPrior = nodeHeightPrior;
    }

    public TreePriorParameterizationType getParameterization() {
        return parameterization;
    }

    public void setParameterization(TreePriorParameterizationType parameterization) {
        this.parameterization = parameterization;
    }

    public int getSkylineGroupCount() {
        return skylineGroupCount;
    }

    public void setSkylineGroupCount(int skylineGroupCount) {
        this.skylineGroupCount = skylineGroupCount;
    }

    public TreePriorParameterizationType getSkylineModel() {
        return skylineModel;
    }

    public void setSkylineModel(TreePriorParameterizationType skylineModel) {
        this.skylineModel = skylineModel;
    }

    public TreePriorParameterizationType getSkyrideSmoothing() {
        return skyrideSmoothing;
    }

    public void setSkyrideSmoothing(TreePriorParameterizationType skyrideSmoothing) {
        this.skyrideSmoothing = skyrideSmoothing;
    }

    public double getBirthDeathSamplingProportion() {
        return birthDeathSamplingProportion;
    }

    public void setBirthDeathSamplingProportion(double birthDeathSamplingProportion) {
        this.birthDeathSamplingProportion = birthDeathSamplingProportion;
    }

    public boolean isFixedTree() {
        return fixedTree;
    }

    public void setFixedTree(boolean fixedTree) {
        this.fixedTree = fixedTree;
    }

    public void setExtendedSkylineModel(VariableDemographicModel.Type extendedSkylineModel) {
        this.extendedSkylineModel = extendedSkylineModel;
    }

    public VariableDemographicModel.Type getExtendedSkylineModel() {
        return extendedSkylineModel;
    }

    public PopulationSizeModelType getPopulationSizeModel() {
        return populationSizeModel;
    }

    public void setPopulationSizeModel(PopulationSizeModelType populationSizeModel) {
        this.populationSizeModel = populationSizeModel;
    }

    public BeautiOptions getOptions() {
        return options;
    }

}