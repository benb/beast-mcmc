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

package dr.app.beauti.generator;

import java.util.List;

import dr.app.beauti.components.ComponentFactory;
import dr.app.beauti.options.*;
import dr.app.beauti.util.XMLWriter;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.branchratemodel.RandomLocalClockModel;
import dr.evomodel.branchratemodel.StrictClockBranchRates;
import dr.evomodel.clock.ACLikelihood;
import dr.evomodel.clock.RateEvolutionLikelihood;
import dr.evomodel.tree.RateCovarianceStatistic;
import dr.evomodel.tree.RateStatistic;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.DiscretizedBranchRatesParser;
import dr.evomodelxml.TreeModelParser;
import dr.inference.distribution.ExponentialDistributionModel;
import dr.inference.distribution.LogNormalDistributionModel;
import dr.inference.model.CompoundParameter;
import dr.inference.model.ParameterParser;
import dr.inference.model.SumStatistic;
import dr.util.Attribute;
import dr.xml.XMLParser;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class BranchRatesModelGenerator extends Generator {

    public BranchRatesModelGenerator(BeautiOptions options, ComponentFactory[] components) {
        super(options, components);
    }

    /**
     * Write the relaxed clock branch rates block.
     *
     * @param model
     * @param tree
     * @param writer the writer
     */
    public void writeBranchRatesModel(PartitionClockModel model, XMLWriter writer) {//PartitionTreeModel tree, 

        setModelPrefix(model.getPrefix());        
        
        Attribute[] attributes;
        int categoryCount = 0;
        String treePrefix = "";
        switch (model.getClockType()) {
            case STRICT_CLOCK:
                writer.writeComment("The strict clock (Uniform rates across branches)");
                
                List<PartitionTreeModel> activeTrees = options.getPartitionTreeModels(model.getAllPartitionData());
                for (PartitionTreeModel tree : activeTrees) {
                	treePrefix = tree.getPrefix();
                	
                	writer.writeOpenTag(
		                        StrictClockBranchRates.STRICT_CLOCK_BRANCH_RATES,
		                        new Attribute[]{new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES)}
		                );
                	
                	if (activeTrees.indexOf(tree) < 1) {
	                	if (options.isFixedSubstitutionRate()) {
		                    fixParameter(model.getParameter("clock.rate"), options.getMeanSubstitutionRate());
		                }		
		                
		                writeParameter("rate", "clock.rate", model, writer);
		                
                	} else {                		
                		writeParameterRef("rate", modelPrefix + "clock.rate", writer);		                
                	}
                	
                	writer.writeCloseTag(StrictClockBranchRates.STRICT_CLOCK_BRANCH_RATES);
                }
                
                break;

            case UNCORRELATED_EXPONENTIAL:
            case UNCORRELATED_LOGNORMAL:
                writer.writeComment("The uncorrelated relaxed clock (Drummond, Ho, Phillips & Rambaut, 2006)");
                
                activeTrees = options.getPartitionTreeModels(model.getAllPartitionData());
                for (PartitionTreeModel tree : activeTrees) {
                	treePrefix = tree.getPrefix();
                	
                    PartitionClockModelTreeModelLink clockTree = options.getPartitionClockTreeLink(model, tree);
                    if (clockTree == null) {
                    	throw new IllegalArgumentException("Cannot find PartitionClockTreeLink, given clock model = " + model.getName() 
                    			+ ", tree model = " + tree.getName());
                    }        
                	
	                //if (options.isFixedSubstitutionRate()) {
	                //    attributes = new Attribute[]{
	                //            new Attribute.Default<String>(XMLParser.ID, BranchRateModel.BRANCH_RATES),
	                //            new Attribute.Default<Double>(DiscretizedBranchRatesParser.NORMALIZED_MEAN, options.getMeanSubstitutionRate())
	                //    };
	                //} else {
	                attributes = new Attribute[]{new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix 
	                		+ BranchRateModel.BRANCH_RATES)};
	                //}
	                writer.writeOpenTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, attributes);
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeOpenTag("distribution");
	                
					if (model.getClockType() == ClockType.UNCORRELATED_EXPONENTIAL) {
	
						writer.writeOpenTag(ExponentialDistributionModel.EXPONENTIAL_DISTRIBUTION_MODEL);
	
						if (activeTrees.indexOf(tree) < 1) {
							if (options.isFixedSubstitutionRate()) {
								fixParameter(model.getParameter(ClockType.UCED_MEAN), options.getMeanSubstitutionRate());
							}
							writeParameter("mean", ClockType.UCED_MEAN, model, writer);
						} else {
							writeParameterRef("mean", modelPrefix + ClockType.UCED_MEAN, writer);
						}
	
						writer.writeCloseTag(ExponentialDistributionModel.EXPONENTIAL_DISTRIBUTION_MODEL);
	
					} else if (model.getClockType() == ClockType.UNCORRELATED_LOGNORMAL) {
	
						writer.writeOpenTag("logNormalDistributionModel",
										new Attribute.Default<String>(LogNormalDistributionModel.MEAN_IN_REAL_SPACE, "true"));
	
						if (activeTrees.indexOf(tree) < 1) {
							if (options.isFixedSubstitutionRate()) {
								fixParameter(model.getParameter(ClockType.UCLD_MEAN), options.getMeanSubstitutionRate());
							}
							writeParameter("mean", ClockType.UCLD_MEAN, model, writer);
							writeParameter("stdev", ClockType.UCLD_STDEV, model, writer);
						} else {
							writeParameterRef("mean", modelPrefix + ClockType.UCLD_MEAN, writer);
							writeParameterRef("stdev", modelPrefix + ClockType.UCLD_STDEV, writer);
						}
	
						writer.writeCloseTag("logNormalDistributionModel");
	
					} else {
						throw new RuntimeException(
								"Unrecognised relaxed clock model");
					}
	                
	                writer.writeCloseTag("distribution");
	                
	                writer.writeOpenTag(DiscretizedBranchRatesParser.RATE_CATEGORIES);
	                if (options.allowDifferentTaxa) {
	                    for (PartitionData dataPartition : options.dataPartitions) {
	                        if (dataPartition.getPartitionClockModel().equals(model)) { // TODO check this with Joseph
	                            categoryCount = (dataPartition.getTaxaCount() - 1) * 2;
	                        }
	                    }
	                } else {
	                    categoryCount = (options.taxonList.getTaxonCount() - 1) * 2;
	                }
	                writeParameter(clockTree.getParameter("branchRates.categories"), categoryCount, writer);
	                writer.writeCloseTag(DiscretizedBranchRatesParser.RATE_CATEGORIES);
	                writer.writeCloseTag(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateStatistic.RATE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + "meanRate"),
	                                new Attribute.Default<String>("name", "meanRate"),
	                                new Attribute.Default<String>("mode", "mean"),
	                                new Attribute.Default<String>("internal", "true"),
	                                new Attribute.Default<String>("external", "true")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, modelPrefix + treePrefix 
	                		+ BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateStatistic.RATE_STATISTIC);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateStatistic.RATE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("name", RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("mode", RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("internal", "true"),
	                                new Attribute.Default<String>("external", "true")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, modelPrefix + treePrefix 
	                		+ BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateStatistic.RATE_STATISTIC);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + "covariance"),
	                                new Attribute.Default<String>("name", "covariance")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(DiscretizedBranchRatesParser.DISCRETIZED_BRANCH_RATES, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC);
                }
	                
                break;

            case AUTOCORRELATED_LOGNORMAL:
                writer.writeComment("The autocorrelated relaxed clock (Rannala & Yang, 2007)");

                activeTrees = options.getPartitionTreeModels(model.getAllPartitionData());
                for (PartitionTreeModel tree : activeTrees) {
                	treePrefix = tree.getPrefix();
                	
                    PartitionClockModelTreeModelLink clockTree = options.getPartitionClockTreeLink(model, tree);
                    if (clockTree == null) {
                    	throw new IllegalArgumentException("Cannot find PartitionClockTreeLink, given clock model = " + model.getName() 
                    			+ ", tree model = " + tree.getName());
                    }        
                
	                attributes = new Attribute[]{
	                        new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES),
	                        new Attribute.Default<String>("episodic", "false"),
	                        new Attribute.Default<String>("logspace", "true"),
	                };
	
	                writer.writeOpenTag(ACLikelihood.AC_LIKELIHOOD, attributes);
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	
	                if (options.isFixedSubstitutionRate()) {
	                    fixParameter(tree.getParameter(TreeModel.TREE_MODEL + "." + RateEvolutionLikelihood.ROOTRATE), options.getMeanSubstitutionRate());//"treeModel.rootRate"
	                }
	
	                writer.writeOpenTag(RateEvolutionLikelihood.RATES,
	                      new Attribute[]{
	                              new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "false"),
	                              new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "true"),
	                              new Attribute.Default<String>(TreeModelParser.LEAF_NODES, "true")
	                      });
	                writer.writeTag(ParameterParser.PARAMETER,
	                      new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + TreeModel.TREE_MODEL + "." 
	                    		  + TreeModelParser.NODE_RATES), true);
	                writer.writeCloseTag(RateEvolutionLikelihood.RATES);
	
	                writer.writeOpenTag(RateEvolutionLikelihood.ROOTRATE,
	                      new Attribute[]{
	                              new Attribute.Default<String>(TreeModelParser.ROOT_NODE, "true"),
	                              new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES, "false"),
	                              new Attribute.Default<String>(TreeModelParser.LEAF_NODES, "false")
	                      });
	                writer.writeTag(ParameterParser.PARAMETER,
	                      new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + TreeModel.TREE_MODEL + "." 
	                    		  + RateEvolutionLikelihood.ROOTRATE), true);
	                writer.writeCloseTag(RateEvolutionLikelihood.ROOTRATE);
	//                writeParameterRef("rates", treePrefix + "treeModel.nodeRates", writer);
	//                writeParameterRef(RateEvolutionLikelihood.ROOTRATE, treePrefix + "treeModel.rootRate", writer);
	                writeParameter("variance", "branchRates.var", clockTree, writer);
	
	                writer.writeCloseTag(ACLikelihood.AC_LIKELIHOOD);
	                
	                if (!options.isFixedSubstitutionRate()) {
		              	writer.writeText("");
			            writer.writeOpenTag(CompoundParameter.COMPOUND_PARAMETER,
			                      new Attribute[]{new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + TreeModel.TREE_MODEL 
			                    		  + "." + "allRates")});
			            writer.writeTag(ParameterParser.PARAMETER,
			                      new Attribute.Default<String>(XMLParser.IDREF, modelPrefix + treePrefix + TreeModel.TREE_MODEL + "." 
			                    		  + TreeModelParser.NODE_RATES), true);
			            writer.writeTag(ParameterParser.PARAMETER,
			                      new Attribute.Default<String>(XMLParser.IDREF, modelPrefix + treePrefix + TreeModel.TREE_MODEL + "." 
			                    		  + RateEvolutionLikelihood.ROOTRATE), true);
			            writer.writeCloseTag(CompoundParameter.COMPOUND_PARAMETER);
	                }
	                
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateStatistic.RATE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + "meanRate"),
	                                new Attribute.Default<String>("name", "meanRate"),
	                                new Attribute.Default<String>("mode", "mean"),
	                                new Attribute.Default<String>("internal", "true"),
	                                new Attribute.Default<String>("external", "true")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(ACLikelihood.AC_LIKELIHOOD, modelPrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateStatistic.RATE_STATISTIC);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateStatistic.RATE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("name", RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("mode", RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("internal", "true"),
	                                new Attribute.Default<String>("external", "true")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(ACLikelihood.AC_LIKELIHOOD, modelPrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateStatistic.RATE_STATISTIC);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + "covariance"),
	                                new Attribute.Default<String>("name", "covariance")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(ACLikelihood.AC_LIKELIHOOD, modelPrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC);
                }
                
                break;

            case RANDOM_LOCAL_CLOCK:
                writer.writeComment("The random local clock model (Drummond & Suchard, 2007)");
                
                activeTrees = options.getPartitionTreeModels(model.getAllPartitionData());
                for (PartitionTreeModel tree : activeTrees) {
                	treePrefix = tree.getPrefix();
                	
                    PartitionClockModelTreeModelLink clockTree = options.getPartitionClockTreeLink(model, tree);
                    if (clockTree == null) {
                    	throw new IllegalArgumentException("Cannot find PartitionClockTreeLink, given clock model = " + model.getName() 
                    			+ ", tree model = " + tree.getName());
                    }        
                
	                writer.writeOpenTag(
	                        RandomLocalClockModel.LOCAL_BRANCH_RATES,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES),
	                                new Attribute.Default<String>("ratesAreMultipliers", "false")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	
	                writeParameterRef("rates", modelPrefix + treePrefix + ClockType.LOCAL_CLOCK + ".rates", writer);
	                writeParameterRef("rateIndicator", modelPrefix + treePrefix + ClockType.LOCAL_CLOCK + ".changes", writer);
	                
	                if (activeTrees.indexOf(tree) < 1) {
		                if (options.isFixedSubstitutionRate()) {
		                    fixParameter(model.getParameter("clock.rate"), options.getMeanSubstitutionRate());
		                }
		                writeParameter("clockRate", "clock.rate", model, writer);
	                } else {
	                	writeParameterRef("clockRate", modelPrefix + "clock.rate", writer);
	                }
	                
	                writer.writeCloseTag(RandomLocalClockModel.LOCAL_BRANCH_RATES);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        SumStatistic.SUM_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + "rateChanges"),
	                                new Attribute.Default<String>("name", "rateChangeCount"),
	                                new Attribute.Default<String>("elementwise", "true"),
	                        }
	                );
	                writer.writeIDref(ParameterParser.PARAMETER, modelPrefix + treePrefix + ClockType.LOCAL_CLOCK + ".changes");
	                writer.writeCloseTag(SumStatistic.SUM_STATISTIC);
	
	                writer.writeText("");
	
	                writer.writeOpenTag(
	                        RateStatistic.RATE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + "meanRate"),
	                                new Attribute.Default<String>("name", "meanRate"),
	                                new Attribute.Default<String>("mode", "mean"),
	                                new Attribute.Default<String>("internal", "true"),
	                                new Attribute.Default<String>("external", "true")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(RandomLocalClockModel.LOCAL_BRANCH_RATES, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateStatistic.RATE_STATISTIC);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateStatistic.RATE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("name", RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("mode", RateStatistic.COEFFICIENT_OF_VARIATION),
	                                new Attribute.Default<String>("internal", "true"),
	                                new Attribute.Default<String>("external", "true")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(RandomLocalClockModel.LOCAL_BRANCH_RATES, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateStatistic.RATE_STATISTIC);
	
	                writer.writeText("");
	                writer.writeOpenTag(
	                        RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC,
	                        new Attribute[]{
	                                new Attribute.Default<String>(XMLParser.ID, modelPrefix + treePrefix + "covariance"),
	                                new Attribute.Default<String>("name", "covariance")
	                        }
	                );
	                writer.writeIDref(TreeModel.TREE_MODEL, treePrefix + TreeModel.TREE_MODEL);
	                writer.writeIDref(RandomLocalClockModel.LOCAL_BRANCH_RATES, modelPrefix + treePrefix + BranchRateModel.BRANCH_RATES);
	                writer.writeCloseTag(RateCovarianceStatistic.RATE_COVARIANCE_STATISTIC);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown clock model");
        }

    }

}
