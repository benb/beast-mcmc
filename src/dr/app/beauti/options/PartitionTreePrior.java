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

import dr.app.beauti.priorsPanel.PriorType;
import dr.evomodel.coalescent.VariableDemographicModel;
import dr.evomodelxml.BirthDeathModelParser;

import java.util.List;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 */
public class PartitionTreePrior extends ModelOptions {

    // Instance variables

    private final BeautiOptions options;

    private String name;

    private PartitionTreeModel treeModel;

    private TreePrior nodeHeightPrior = TreePrior.CONSTANT;
    private int parameterization = GROWTH_RATE;
    private int skylineGroupCount = 10;
    private int skylineModel = CONSTANT_SKYLINE;
    private int skyrideSmoothing = SKYRIDE_TIME_AWARE_SMOOTHING;
    // AR - this seems to be set to taxonCount - 1 so we don't need to
    // have a settable variable...
    // public int skyrideIntervalCount = 1;
    private String extendedSkylineModel = VariableDemographicModel.LINEAR;
    private boolean multiLoci = false;
    private double birthDeathSamplingProportion = 1.0;
    private boolean fixedTree = false;

    public PartitionTreePrior(BeautiOptions options, PartitionTreeModel treeModel) {
        this.options = options;
        this.name = treeModel.getName();
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
        this.name = name;
        this.treeModel = source.treeModel;

        this.nodeHeightPrior = source.nodeHeightPrior;
        this.parameterization = source.parameterization;
        this.skylineGroupCount = source.skylineGroupCount;
        this.skylineModel = source.skylineModel;
        this.skyrideSmoothing = source.skyrideSmoothing;
        this.multiLoci = source.multiLoci;
        this.birthDeathSamplingProportion = source.birthDeathSamplingProportion;
        this.fixedTree = source.fixedTree;

        initTreePriorParaAndOpers();
    }

//    public PartitionTreePrior(BeautiOptions options, String name) {
//        this.options = options;
//        this.name = name;
//    }    

    private void initTreePriorParaAndOpers() {
        double treeWeights = 15.0;

        createScaleParameter("constant.popSize", "coalescent population size parameter", TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);

        createScaleParameter("exponential.popSize", "coalescent population size parameter", TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("exponential.growthRate", "coalescent growth rate parameter", GROWTH_RATE_SCALE, 0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        createParameter("exponential.doublingTime", "coalescent doubling time parameter", TIME_SCALE, 0.5, 0.0, Double.POSITIVE_INFINITY);
        createScaleParameter("logistic.popSize", "coalescent population size parameter", TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("logistic.growthRate", "coalescent logistic growth rate parameter", GROWTH_RATE_SCALE, 0.001, 0.0, Double.POSITIVE_INFINITY);
        createParameter("logistic.doublingTime", "coalescent doubling time parameter", TIME_SCALE, 0.5, 0.0, Double.POSITIVE_INFINITY);
        createParameter("logistic.t50", "logistic shape parameter", T50_SCALE, 0.1, 0.0, Double.POSITIVE_INFINITY);
        createScaleParameter("expansion.popSize", "coalescent population size parameter", TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("expansion.growthRate", "coalescent logistic growth rate parameter", GROWTH_RATE_SCALE, 0.001, 0.0, Double.POSITIVE_INFINITY);
        createParameter("expansion.doublingTime", "coalescent doubling time parameter", TIME_SCALE, 0.5, 0.0, Double.POSITIVE_INFINITY);
        createParameter("expansion.ancestralProportion", "ancestral population proportion", NONE, 0.1, 0.0, 1.0);
        createParameter("skyline.popSize", "Bayesian Skyline population sizes", TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("skyline.groupSize", "Bayesian Skyline group sizes");

        createParameter("skyride.popSize", "GMRF Bayesian skyride population sizes", TIME_SCALE, 1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        createParameter("skyride.groupSize", "GMRF Bayesian skyride group sizes (for backward compatibility)");
        {
            final Parameter p = createParameter("skyride.precision", "GMRF Bayesian skyride precision", NONE, 1.0, 0.0, Double.POSITIVE_INFINITY);
            p.priorType = PriorType.GAMMA_PRIOR;
            p.gammaAlpha = 0.001;
            p.gammaBeta = 1000;
            p.priorFixed = true;
        }

        createParameter("demographic.popSize", "Extended Bayesian Skyline population sizes", TIME_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter("demographic.indicators", "Extended Bayesian Skyline population switch");
        createScaleParameter("demographic.populationMean", "Extended Bayesian Skyline population prior mean", TIME_SCALE, 1, 0, Double.POSITIVE_INFINITY);
        {
            final Parameter p = createStatistic("demographic.populationSizeChanges", "Average number of population change points", true);
            p.priorType = PriorType.POISSON_PRIOR;
            p.poissonMean = Math.log(2);
        }
        createParameter("yule.birthRate", "Yule speciation process birth rate", BIRTH_RATE_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);

        createParameter(BirthDeathModelParser.BIRTHDIFF_RATE_PARAM_NAME, "Birth-Death speciation process rate", BIRTH_RATE_SCALE, 1.0, 0.0, Double.POSITIVE_INFINITY);
        createParameter(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME, "Death/Birth speciation process relative death rate", BIRTH_RATE_SCALE, 0.5, 0.0, 1.0);

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
        createOperator("demographic.popSize", "demographic.populationMeanDist", "", super.getParameter("demographic.popSize"),
                super.getParameter("demographic.indicators"), OperatorType.SAMPLE_NONACTIVE, 1, 5 * demoWeights);
        createOperator("demographic.scaleActive", "demographic.scaleActive", "", super.getParameter("demographic.popSize"),
                super.getParameter("demographic.indicators"), OperatorType.SCALE_WITH_INDICATORS, 0.5, 2 * demoWeights);

        createOperator("gmrfGibbsOperator", "gmrfGibbsOperator", "Gibbs sampler for GMRF", super.getParameter("skyride.popSize"),
                super.getParameter("skyride.precision"), OperatorType.GMRF_GIBBS_OPERATOR, 2, 2);

        createScaleOperator("yule.birthRate", demoTuning, demoWeights);

        createScaleOperator(BirthDeathModelParser.BIRTHDIFF_RATE_PARAM_NAME, demoTuning, demoWeights);
        createScaleOperator(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME, demoTuning, demoWeights);

    }

    /**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectParameters(List<Parameter> params) {

        if (nodeHeightPrior == TreePrior.CONSTANT) {
            params.add(getParameter("constant.popSize"));
        } else if (nodeHeightPrior == TreePrior.EXPONENTIAL) {
            params.add(getParameter("exponential.popSize"));
            if (parameterization == GROWTH_RATE) {
                params.add(getParameter("exponential.growthRate"));
            } else {
                params.add(getParameter("exponential.doublingTime"));
            }
        } else if (nodeHeightPrior == TreePrior.LOGISTIC) {
            params.add(getParameter("logistic.popSize"));
            if (parameterization == GROWTH_RATE) {
                params.add(getParameter("logistic.growthRate"));
            } else {
                params.add(getParameter("logistic.doublingTime"));
            }
            params.add(getParameter("logistic.t50"));
        } else if (nodeHeightPrior == TreePrior.EXPANSION) {
            params.add(getParameter("expansion.popSize"));
            if (parameterization == GROWTH_RATE) {
                params.add(getParameter("expansion.growthRate"));
            } else {
                params.add(getParameter("expansion.doublingTime"));
            }
            params.add(getParameter("expansion.ancestralProportion"));
        } else if (nodeHeightPrior == TreePrior.SKYLINE) {
            params.add(getParameter("skyline.popSize"));
        } else if (nodeHeightPrior == TreePrior.EXTENDED_SKYLINE) {
            params.add(getParameter("demographic.populationSizeChanges"));
            params.add(getParameter("demographic.populationMean"));
        } else if (nodeHeightPrior == TreePrior.GMRF_SKYRIDE) {
//            params.add(getParameter("skyride.popSize"));
            params.add(getParameter("skyride.precision"));
        } else if (nodeHeightPrior == TreePrior.YULE) {
            params.add(getParameter("yule.birthRate"));
        } else if (nodeHeightPrior == TreePrior.BIRTH_DEATH) {
            params.add(getParameter(BirthDeathModelParser.BIRTHDIFF_RATE_PARAM_NAME));
            params.add(getParameter(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        }

    }

    /**
     * return a list of operators that are required
     *
     * @param ops the operator list
     */
    public void selectOperators(List<Operator> ops) {

        if (nodeHeightPrior == TreePrior.CONSTANT) {
            ops.add(getOperator("constant.popSize"));
        } else if (nodeHeightPrior == TreePrior.EXPONENTIAL) {
            ops.add(getOperator("exponential.popSize"));
            if (parameterization == GROWTH_RATE) {
                ops.add(getOperator("exponential.growthRate"));
            } else {
                ops.add(getOperator("exponential.doublingTime"));
            }
        } else if (nodeHeightPrior == TreePrior.LOGISTIC) {
            ops.add(getOperator("logistic.popSize"));
            if (parameterization == GROWTH_RATE) {
                ops.add(getOperator("logistic.growthRate"));
            } else {
                ops.add(getOperator("logistic.doublingTime"));
            }
            ops.add(getOperator("logistic.t50"));
        } else if (nodeHeightPrior == TreePrior.EXPANSION) {
            ops.add(getOperator("expansion.popSize"));
            if (parameterization == GROWTH_RATE) {
                ops.add(getOperator("expansion.growthRate"));
            } else {
                ops.add(getOperator("expansion.doublingTime"));
            }
            ops.add(getOperator("expansion.ancestralProportion"));
        } else if (nodeHeightPrior == TreePrior.SKYLINE) {
            ops.add(getOperator("skyline.popSize"));
            ops.add(getOperator("skyline.groupSize"));
        } else if (nodeHeightPrior == TreePrior.GMRF_SKYRIDE) {
            ops.add(getOperator("gmrfGibbsOperator"));
        } else if (nodeHeightPrior == TreePrior.EXTENDED_SKYLINE) {
            ops.add(getOperator("demographic.populationMean"));
            ops.add(getOperator("demographic.popSize"));
            ops.add(getOperator("demographic.indicators"));
            ops.add(getOperator("demographic.scaleActive"));
        } else if (nodeHeightPrior == TreePrior.YULE) {
            ops.add(getOperator("yule.birthRate"));
        } else if (nodeHeightPrior == TreePrior.BIRTH_DEATH) {
            ops.add(getOperator(BirthDeathModelParser.BIRTHDIFF_RATE_PARAM_NAME));
            ops.add(getOperator(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        }
    }


    //////////////////////////////////////////////////////

    public Parameter getParameter(String name) {

        Parameter parameter = parameters.get(name);

        if (parameter == null) {
            throw new IllegalArgumentException("Parameter with name, " + name + ", is unknown");
        }

        parameter.setPrefix(getPrefix());

        return parameter;
    }

    public Operator getOperator(String name) {

        Operator operator = operators.get(name);

        if (operator == null) throw new IllegalArgumentException("Operator with name, " + name + ", is unknown");

        operator.setPrefix(getPrefix());

        return operator;
    }

    public String getPrefix() {
        String prefix = "";
        if (options.getPartitionTreePriors().size() > 1) {//|| options.isSpeciesAnalysis()
            // There is more than one active partition model, or doing species analysis
            prefix += getName() + ".";
        }
        return prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return getName();
    }

    /////////////////////////////////////////////////////////////////////////

    public PartitionTreeModel getTreeModel() {
        return treeModel;
    }

    public void setTreeModel(PartitionTreeModel treeModel) {
        this.treeModel = treeModel;
    }

    public TreePrior getNodeHeightPrior() {
        return nodeHeightPrior;
    }

    public void setNodeHeightPrior(TreePrior nodeHeightPrior) {
        this.nodeHeightPrior = nodeHeightPrior;
    }

    public int getParameterization() {
        return parameterization;
    }

    public void setParameterization(int parameterization) {
        this.parameterization = parameterization;
    }

    public int getSkylineGroupCount() {
        return skylineGroupCount;
    }

    public void setSkylineGroupCount(int skylineGroupCount) {
        this.skylineGroupCount = skylineGroupCount;
    }

    public int getSkylineModel() {
        return skylineModel;
    }

    public void setSkylineModel(int skylineModel) {
        this.skylineModel = skylineModel;
    }

    public int getSkyrideSmoothing() {
        return skyrideSmoothing;
    }

    public void setSkyrideSmoothing(int skyrideSmoothing) {
        this.skyrideSmoothing = skyrideSmoothing;
    }

    public boolean isMultiLoci() {
        return multiLoci;
    }

    public void setMultiLoci(boolean multiLoci) {
        this.multiLoci = multiLoci;
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

    public void setExtendedSkylineModel(String extendedSkylineModel) {
        this.extendedSkylineModel = extendedSkylineModel;
    }

    public String getExtendedSkylineModel() {
        return extendedSkylineModel;
    }

    public BeautiOptions getOptions() {
        return options;
    }

}