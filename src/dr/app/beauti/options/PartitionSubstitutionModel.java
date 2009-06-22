package dr.app.beauti.options;

import dr.app.beauti.priorsPanel.PriorType;
import dr.evolution.datatype.DataType;
import dr.evolution.util.Taxa;
import dr.evomodel.tree.RateStatistic;
import dr.evomodelxml.BirthDeathModelParser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class PartitionSubstitutionModel extends ModelOptions {
    
    public PartitionSubstitutionModel(BeautiOptions options, PartitionData partition) {
        this(options, partition.getName(), partition.getAlignment().getDataType());
    }

    /**
     * A copy constructor
     *
     * @param options the beauti options
     * @param name    the name of the new model
     * @param source  the source model
     */
    public PartitionSubstitutionModel(BeautiOptions options, String name, PartitionSubstitutionModel source) {
        this(options, name, source.dataType);

        nucSubstitutionModel = source.nucSubstitutionModel;
        aaSubstitutionModel = source.aaSubstitutionModel;
        binarySubstitutionModel = source.binarySubstitutionModel;

        frequencyPolicy = source.frequencyPolicy;
        gammaHetero = source.gammaHetero;
        gammaCategories = source.gammaCategories;
        invarHetero = source.invarHetero;
        codonHeteroPattern = source.codonHeteroPattern;
        unlinkedSubstitutionModel = source.unlinkedSubstitutionModel;
        unlinkedHeterogeneityModel = source.unlinkedHeterogeneityModel;
        unlinkedFrequencyModel = source.unlinkedFrequencyModel;
    }

    public PartitionSubstitutionModel(BeautiOptions options, String name, DataType dataType) {

        this.options = options;
        this.name = name;
        this.dataType = dataType;

        initSubstModelParaAndOpers();
    }    
        
    // use override method getParameter(String name) in PartitionModel containing prefix	
    public void selectParameters(List<Parameter> params) {
        if (options.hasData()) {

            // if not fixed then do mutation rate move and up/down move
            boolean fixed = options.isFixedSubstitutionRate();
            Parameter rateParam;

            switch (options.clockType) {
                case STRICT_CLOCK:
                    rateParam = getParameter("clock.rate");
                    if (!fixed) params.add(rateParam);
                    break;

                case UNCORRELATED_EXPONENTIAL:
                    rateParam = getParameter(ClockType.UCED_MEAN);
                    if (!fixed) params.add(rateParam);
                    break;

                case UNCORRELATED_LOGNORMAL:
                    rateParam = getParameter(ClockType.UCLD_MEAN);
                    if (!fixed) params.add(rateParam);
                    params.add(getParameter(ClockType.UCLD_STDEV));
                    break;

                case AUTOCORRELATED_LOGNORMAL:
                    rateParam = getParameter("treeModel.rootRate");
                    if (!fixed) params.add(rateParam);
                    params.add(getParameter("branchRates.var"));
                    break;

                case RANDOM_LOCAL_CLOCK:
                    rateParam = getParameter("clock.rate");
                    if (!fixed) params.add(rateParam);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown clock model");
            }

            /*if (clockType == ClockType.STRICT_CLOCK || clockType == ClockType.RANDOM_LOCAL_CLOCK) {
				rateParam = getParameter("clock.rate");
				if (!fixed) params.add(rateParam);
			} else {
				if (clockType == ClockType.UNCORRELATED_EXPONENTIAL) {
					rateParam = getParameter("uced.mean");
					if (!fixed) params.add(rateParam);
				} else if (clockType == ClockType.UNCORRELATED_LOGNORMAL) {
					rateParam = getParameter("ucld.mean");
					if (!fixed) params.add(rateParam);
					params.add(getParameter("ucld.stdev"));
				} else {
					throw new IllegalArgumentException("Unknown clock model");
				}
			}*/

            rateParam.isFixed = fixed;

        }

        if (options.nodeHeightPrior == TreePrior.CONSTANT) {
            params.add(getParameter("constant.popSize"));
        } else if (options.nodeHeightPrior == TreePrior.EXPONENTIAL) {
            params.add(getParameter("exponential.popSize"));
            if (options.parameterization == GROWTH_RATE) {
                params.add(getParameter("exponential.growthRate"));
            } else {
                params.add(getParameter("exponential.doublingTime"));
            }
        } else if (options.nodeHeightPrior == TreePrior.LOGISTIC) {
            params.add(getParameter("logistic.popSize"));
            if (options.parameterization == GROWTH_RATE) {
                params.add(getParameter("logistic.growthRate"));
            } else {
                params.add(getParameter("logistic.doublingTime"));
            }
            params.add(getParameter("logistic.t50"));
        } else if (options.nodeHeightPrior == TreePrior.EXPANSION) {
            params.add(getParameter("expansion.popSize"));
            if (options.parameterization == GROWTH_RATE) {
                params.add(getParameter("expansion.growthRate"));
            } else {
                params.add(getParameter("expansion.doublingTime"));
            }
            params.add(getParameter("expansion.ancestralProportion"));
        } else if (options.nodeHeightPrior == TreePrior.SKYLINE) {
            params.add(getParameter("skyline.popSize"));
        } else if (options.nodeHeightPrior == TreePrior.EXTENDED_SKYLINE) {
            params.add(getParameter("demographic.populationSizeChanges"));
            params.add(getParameter("demographic.populationMean"));
        } else if (options.nodeHeightPrior == TreePrior.GMRF_SKYRIDE) {
//            params.add(getParameter("skyride.popSize"));
            params.add(getParameter("skyride.precision"));
        } else if (options.nodeHeightPrior == TreePrior.YULE) {
            params.add(getParameter("yule.birthRate"));
        } else if (options.nodeHeightPrior == TreePrior.BIRTH_DEATH) {
            params.add(getParameter(BirthDeathModelParser.BIRTHDIFF_RATE_PARAM_NAME));
            params.add(getParameter(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        }

        params.add(getParameter("treeModel.rootHeight"));
        
    }
    
    // use override method getParameter(String name) and getOperator(String name) in PartitionModel containing prefix	
    public void selectOperators(List<Operator> ops) { 
        if (options.hasData()) {

            if (!options.isFixedSubstitutionRate()) {
                switch (options.clockType) {
                    case STRICT_CLOCK:
                        ops.add(getOperator("clock.rate"));
                        ops.add(getOperator("upDownRateHeights"));
                        break;

                    case UNCORRELATED_EXPONENTIAL:
                        ops.add(getOperator(ClockType.UCED_MEAN));
                        ops.add(getOperator("upDownUCEDMeanHeights"));
                        ops.add(getOperator("swapBranchRateCategories"));
                        ops.add(getOperator("randomWalkBranchRateCategories"));
                        ops.add(getOperator("unformBranchRateCategories"));
                        break;

                    case UNCORRELATED_LOGNORMAL:
                        ops.add(getOperator(ClockType.UCLD_MEAN));
                        ops.add(getOperator(ClockType.UCLD_STDEV));
                        ops.add(getOperator("upDownUCLDMeanHeights"));
                        ops.add(getOperator("swapBranchRateCategories"));
                        ops.add(getOperator("randomWalkBranchRateCategories"));
                        ops.add(getOperator("unformBranchRateCategories"));
                        break;

                    case AUTOCORRELATED_LOGNORMAL:
                        ops.add(getOperator("scaleRootRate"));
                        ops.add(getOperator("scaleOneRate"));
                        ops.add(getOperator("scaleAllRates"));
                        ops.add(getOperator("scaleAllRatesIndependently"));
                        ops.add(getOperator("upDownAllRatesHeights"));
                        ops.add(getOperator("branchRates.var"));
                        break;

                    case RANDOM_LOCAL_CLOCK:
                        ops.add(getOperator("clock.rate"));
                        ops.add(getOperator("upDownRateHeights"));
                        ops.add(getOperator(ClockType.LOCAL_CLOCK + "." + "rates"));
                        ops.add(getOperator(ClockType.LOCAL_CLOCK + "." + "changes"));
                        ops.add(getOperator("treeBitMove"));
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown clock model");
                }
            } else {
                switch (options.clockType) {
                    case STRICT_CLOCK:
                        // no parameter to operator on
                        break;

                    case UNCORRELATED_EXPONENTIAL:
                        ops.add(getOperator("swapBranchRateCategories"));
                        ops.add(getOperator("randomWalkBranchRateCategories"));
                        ops.add(getOperator("unformBranchRateCategories"));
                        break;

                    case UNCORRELATED_LOGNORMAL:
                        ops.add(getOperator(ClockType.UCLD_STDEV));
                        ops.add(getOperator("swapBranchRateCategories"));
                        ops.add(getOperator("randomWalkBranchRateCategories"));
                        ops.add(getOperator("unformBranchRateCategories"));
                        break;

                    case AUTOCORRELATED_LOGNORMAL:
                        ops.add(getOperator("scaleOneRate"));
                        ops.add(getOperator("scaleAllRatesIndependently"));
                        ops.add(getOperator("branchRates.var"));
                        break;

                    case RANDOM_LOCAL_CLOCK:
                        ops.add(getOperator(ClockType.LOCAL_CLOCK + "." + "rates"));
                        ops.add(getOperator(ClockType.LOCAL_CLOCK + "." + "changes"));
                        ops.add(getOperator("treeBitMove"));
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown clock model");
                }
            }
        }

        if (options.nodeHeightPrior == TreePrior.CONSTANT) {
            ops.add(getOperator("constant.popSize"));
        } else if (options.nodeHeightPrior == TreePrior.EXPONENTIAL) {
            ops.add(getOperator("exponential.popSize"));
            if (options.parameterization == GROWTH_RATE) {
                ops.add(getOperator("exponential.growthRate"));
            } else {
                ops.add(getOperator("exponential.doublingTime"));
            }
        } else if (options.nodeHeightPrior == TreePrior.LOGISTIC) {
            ops.add(getOperator("logistic.popSize"));
            if (options.parameterization == GROWTH_RATE) {
                ops.add(getOperator("logistic.growthRate"));
            } else {
                ops.add(getOperator("logistic.doublingTime"));
            }
            ops.add(getOperator("logistic.t50"));
        } else if (options.nodeHeightPrior == TreePrior.EXPANSION) {
            ops.add(getOperator("expansion.popSize"));
            if (options.parameterization == GROWTH_RATE) {
                ops.add(getOperator("expansion.growthRate"));
            } else {
                ops.add(getOperator("expansion.doublingTime"));
            }
            ops.add(getOperator("expansion.ancestralProportion"));
        } else if (options.nodeHeightPrior == TreePrior.SKYLINE) {
            ops.add(getOperator("skyline.popSize"));
            ops.add(getOperator("skyline.groupSize"));
        } else if (options.nodeHeightPrior == TreePrior.GMRF_SKYRIDE) {
            ops.add(getOperator("gmrfGibbsOperator"));
        } else if (options.nodeHeightPrior == TreePrior.EXTENDED_SKYLINE) {
            ops.add(getOperator("demographic.populationMean"));
            ops.add(getOperator("demographic.popSize"));
            ops.add(getOperator("demographic.indicators"));
            ops.add(getOperator("demographic.scaleActive"));
        } else if (options.nodeHeightPrior == TreePrior.YULE) {
            ops.add(getOperator("yule.birthRate"));
        } else if (options.nodeHeightPrior == TreePrior.BIRTH_DEATH) {
            ops.add(getOperator(BirthDeathModelParser.BIRTHDIFF_RATE_PARAM_NAME));
            ops.add(getOperator(BirthDeathModelParser.RELATIVE_DEATH_RATE_PARAM_NAME));
        }

        ops.add(getOperator("treeModel.rootHeight"));
        ops.add(getOperator("uniformHeights"));

        // if not a fixed tree then sample tree space
        if (!options.fixedTree) {
            ops.add(getOperator("subtreeSlide"));
            ops.add(getOperator("narrowExchange"));
            ops.add(getOperator("wideExchange"));
            ops.add(getOperator("wilsonBalding"));
        }

    }
    
    // use override method getParameter(String name) and getOperator(String name) in PartitionModel containing prefix
    public void selectStatistics(List<Parameter> params) {

//        if (options.taxonSets != null) {
//            for (Taxa taxonSet : options.taxonSets) {
//                Parameter statistic = statistics.get(taxonSet);
//                if (statistic == null) {
//                    statistic = new Parameter(taxonSet, "tMRCA for taxon set ");
//                    statistics.put(taxonSet, statistic);
//                }
//                params.add(statistic);
//            }
//        } else {
//            System.err.println("TaxonSets are null");
//        }

        if (options.clockType == ClockType.RANDOM_LOCAL_CLOCK) {
            if (this.localClockRateChangesStatistic == null) {
            	this.localClockRateChangesStatistic = new Parameter("rateChanges", "number of random local clocks", true);
            	this.localClockRateChangesStatistic.priorType = PriorType.POISSON_PRIOR;
            	this.localClockRateChangesStatistic.poissonMean = 1.0;
            	this.localClockRateChangesStatistic.poissonOffset = 0.0;
            }
            if (this.localClockRatesStatistic == null) {
            	this.localClockRatesStatistic = new Parameter(ClockType.LOCAL_CLOCK + "." + "rates", "random local clock rates", false);

            	this.localClockRatesStatistic.priorType = PriorType.GAMMA_PRIOR;
            	this.localClockRatesStatistic.gammaAlpha = 0.5;
            	this.localClockRatesStatistic.gammaBeta = 2.0;
            }
            
            this.localClockRateChangesStatistic.setPrefix(getPrefix());
            params.add(this.localClockRateChangesStatistic);
            this.localClockRatesStatistic.setPrefix(getPrefix());
            params.add(this.localClockRatesStatistic);
        }

        if (options.clockType != ClockType.STRICT_CLOCK) {
            params.add(getParameter("meanRate"));
            params.add(getParameter(RateStatistic.COEFFICIENT_OF_VARIATION));
            params.add(getParameter("covariance"));
        }

    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<Operator>();

        switch (dataType.getType()) {
            case DataType.NUCLEOTIDES:

                if (getCodonPartitionCount() > 1 && unlinkedSubstitutionModel) {
                    if (codonHeteroPattern.equals("123")) {
                        switch (nucSubstitutionModel) {
                            case HKY:
                                operators.add(getOperator("CP1.kappa"));
                                operators.add(getOperator("CP2.kappa"));
                                operators.add(getOperator("CP3.kappa"));
                                break;

                            case GTR:
                                for (int i = 1; i <= 3; i++) {
                                    for (String rateName : GTR_RATE_NAMES) {
                                        operators.add(getOperator("CP" + i + "." + rateName));
                                    }
                                }
                                break;

                            default:
                                throw new IllegalArgumentException("Unknown nucleotides substitution model");
                        }

                        if (frequencyPolicy == FrequencyPolicy.ESTIMATED) {
                            if (getCodonPartitionCount() > 1 && unlinkedSubstitutionModel) {
                                operators.add(getOperator("CP1.frequencies"));
                                operators.add(getOperator("CP2.frequencies"));
                                operators.add(getOperator("CP3.frequencies"));
                            } else {
                                operators.add(getOperator("frequencies"));
                            }
                        }
                    } else if (codonHeteroPattern.equals("112")) {
                        switch (nucSubstitutionModel) {
                            case HKY:
                                operators.add(getOperator("CP1+2.kappa"));
                                operators.add(getOperator("CP3.kappa"));
                                break;

                            case GTR:
                                for (String rateName : GTR_RATE_NAMES) {
                                    operators.add(getOperator("CP1+2." + rateName));
                                }
                                for (String rateName : GTR_RATE_NAMES) {
                                    operators.add(getOperator("CP3." + rateName));
                                }
                                break;

                            default:
                                throw new IllegalArgumentException("Unknown nucleotides substitution model");
                        }
                        if (frequencyPolicy == FrequencyPolicy.ESTIMATED) {
                            if (getCodonPartitionCount() > 1 && unlinkedSubstitutionModel) {
                                operators.add(getOperator("CP1+2.frequencies"));
                                operators.add(getOperator("CP3.frequencies"));
                            } else {
                                operators.add(getOperator("frequencies"));
                            }
                        }

                    } else {
                        throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                    }
                } else { // no codon partitioning
                    switch (nucSubstitutionModel) {
                        case HKY:
                            operators.add(getOperator("kappa"));
                            break;

                        case GTR:
                            for (String rateName : GTR_RATE_NAMES) {
                                operators.add(getOperator(rateName));
                            }
                            break;

                        default:
                            throw new IllegalArgumentException("Unknown nucleotides substitution model");
                    }
                    if (frequencyPolicy == FrequencyPolicy.ESTIMATED) {
                        operators.add(getOperator("frequencies"));
                    }
                }
                break;

            case DataType.AMINO_ACIDS:
                break;

            case DataType.TWO_STATES:
            case DataType.COVARION:
                switch (binarySubstitutionModel) {
                    case BIN_SIMPLE:
                        break;

                    case BIN_COVARION:
                        operators.add(getOperator("bcov.alpha"));
                        operators.add(getOperator("bcov.s"));
                        operators.add(getOperator("bcov.frequencies"));
                        operators.add(getOperator("bcov.hfrequencies"));
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown binary substitution model");
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown data type");
        }

        // if gamma do shape move
        if (gammaHetero) {
            if (getCodonPartitionCount() > 1 && unlinkedHeterogeneityModel) {
                if (codonHeteroPattern.equals("123")) {
                    operators.add(getOperator("CP1.alpha"));
                    operators.add(getOperator("CP2.alpha"));
                    operators.add(getOperator("CP3.alpha"));
                } else if (codonHeteroPattern.equals("112")) {
                    operators.add(getOperator("CP1+2.alpha"));
                    operators.add(getOperator("CP3.alpha"));
                } else {
                    throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                }
            } else {
                operators.add(getOperator("alpha"));
            }
        }
        // if pinv do pinv move
        if (invarHetero) {
            if (getCodonPartitionCount() > 1 && unlinkedHeterogeneityModel) {
                if (codonHeteroPattern.equals("123")) {
                    operators.add(getOperator("CP1.pInv"));
                    operators.add(getOperator("CP2.alpha"));
                    operators.add(getOperator("CP3.pInv"));
                } else if (codonHeteroPattern.equals("112")) {
                    operators.add(getOperator("CP1+2.pInv"));
                    operators.add(getOperator("CP3.pInv"));
                } else {
                    throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                }
            } else {
                operators.add(getOperator("pInv"));
            }
        }

        return operators;
    }

    /**
     * @param includeRelativeRates true if relative rate parameters should be added
     * @return a list of parameters that are required
     */
    List<Parameter> getParameters(boolean includeRelativeRates) {

        List<Parameter> params = new ArrayList<Parameter>();

        switch (dataType.getType()) {
            case DataType.NUCLEOTIDES:
                if (getCodonPartitionCount() > 1 && unlinkedSubstitutionModel) {
                    if (codonHeteroPattern.equals("123")) {
                        switch (nucSubstitutionModel) {
                            case HKY:
                                params.add(getParameter("CP1.kappa"));
                                params.add(getParameter("CP2.kappa"));
                                params.add(getParameter("CP3.kappa"));
                                break;
                            case GTR:
                                for (int i = 1; i <= getCodonPartitionCount(); i++) {
                                    for (String rateName : GTR_RATE_NAMES) {
                                        params.add(getParameter("CP" + i + "." + rateName));
                                    }
                                }
                                break;

                            default:
                                throw new IllegalArgumentException("Unknown nucleotides substitution model");
                        }
                        params.add(getParameter("CP1.mu"));
                        params.add(getParameter("CP2.mu"));
                        params.add(getParameter("CP3.mu"));
                    } else if (codonHeteroPattern.equals("112")) {
                        switch (nucSubstitutionModel) {
                            case HKY:
                                params.add(getParameter("CP1+2.kappa"));
                                params.add(getParameter("CP3.kappa"));
                                break;
                            case GTR:
                                for (String rateName : GTR_RATE_NAMES) {
                                    params.add(getParameter("CP1+2." + rateName));
                                }
                                for (String rateName : GTR_RATE_NAMES) {
                                    params.add(getParameter("CP3." + rateName));
                                }
                                break;

                            default:
                                throw new IllegalArgumentException("Unknown nucleotides substitution model");
                        }
                        params.add(getParameter("CP1+2.mu"));
                        params.add(getParameter("CP3.mu"));
                    } else {
                        throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                    }
                } else { // no codon partitioning
                    switch (nucSubstitutionModel) {
                        case HKY:
                            params.add(getParameter("kappa"));
                            break;
                        case GTR:
                            for (String rateName : GTR_RATE_NAMES) {
                                params.add(getParameter(rateName));
                            }
                            break;

                        default:
                            throw new IllegalArgumentException("Unknown nucleotides substitution model");
                    }
                    if (includeRelativeRates) {
                        params.add(getParameter("mu"));
                    }
                }
                break;

            case DataType.AMINO_ACIDS:
                if (includeRelativeRates) {
                    params.add(getParameter("mu"));
                }
                break;

            case DataType.TWO_STATES:
            case DataType.COVARION:
                switch (binarySubstitutionModel) {
                    case BIN_SIMPLE:
                        break;

                    case BIN_COVARION:
                        params.add(getParameter("bcov.alpha"));
                        params.add(getParameter("bcov.s"));
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown binary substitution model");
                }
                if (includeRelativeRates) {
                    params.add(getParameter("mu"));
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown data type");
        }

        // if gamma do shape move
        if (gammaHetero) {
            if (getCodonPartitionCount() > 1 && unlinkedHeterogeneityModel) {
                if (codonHeteroPattern.equals("123")) {
                    params.add(getParameter("CP1.alpha"));
                    params.add(getParameter("CP2.alpha"));
                    params.add(getParameter("CP3.alpha"));
                } else if (codonHeteroPattern.equals("112")) {
                    params.add(getParameter("CP1+2.alpha"));
                    params.add(getParameter("CP3.alpha"));
                } else {
                    throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                }
            } else {
                params.add(getParameter("alpha"));
            }
        }
        // if pinv do pinv move
        if (invarHetero) {
            if (getCodonPartitionCount() > 1 && unlinkedHeterogeneityModel) {
                if (codonHeteroPattern.equals("123")) {
                    params.add(getParameter("CP1.pInv"));
                    params.add(getParameter("CP2.pInv"));
                    params.add(getParameter("CP3.pInv"));
                } else if (codonHeteroPattern.equals("112")) {
                    params.add(getParameter("CP1+2.pInv"));
                    params.add(getParameter("CP3.pInv"));
                } else {
                    throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                }
            } else {
                params.add(getParameter("pInv"));
            }
        }
        if (frequencyPolicy == FrequencyPolicy.ESTIMATED) {
            if (getCodonPartitionCount() > 1 && unlinkedHeterogeneityModel) {
                if (codonHeteroPattern.equals("123")) {
                    params.add(getParameter("CP1.frequencies"));
                    params.add(getParameter("CP2.frequencies"));
                    params.add(getParameter("CP3.frequencies"));
                } else if (codonHeteroPattern.equals("112")) {
                    params.add(getParameter("CP1+2.frequencies"));
                    params.add(getParameter("CP3.frequencies"));
                } else {
                    throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
                }
            } else {
                params.add(getParameter("frequencies"));
            }

        }

        return params;
    }

    public Parameter getParameter(String name) {

        if (name.startsWith(getName())) {
            name = name.substring(getName().length() + 1);
        }
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

        operator.setPrefix(getName());

        return operator;
    }

    public int getCodonPartitionCount() {
        if (codonHeteroPattern == null || codonHeteroPattern.equals("111")) {
            return 1;
        }
        if (codonHeteroPattern.equals("123")) {
            return 3;
        }
        if (codonHeteroPattern.equals("112")) {
            return 2;
        }
        throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
    }

    public void addWeightsForPartition(PartitionData partition, int[] weights, int offset) {
        int n = partition.getSiteCount();
        int codonCount = n / 3;
        int remainder = n % 3;
        if (codonHeteroPattern == null || codonHeteroPattern.equals("111")) {
            weights[offset] += n;
            return;
        }
        if (codonHeteroPattern.equals("123")) {
            weights[offset] += codonCount + (remainder > 0 ? 1 : 0);
            weights[offset + 1] += codonCount + (remainder > 1 ? 1 : 0);
            weights[offset + 2] += codonCount;
            return;
        }
        if (codonHeteroPattern.equals("112")) {
            weights[offset] += codonCount * 2 + remainder; // positions 1 + 2
            weights[offset + 1] += codonCount; // position 3
            return;
        }
        throw new IllegalArgumentException("codonHeteroPattern must be one of '111', '112' or '123'");
    }

    public String toString() {
        return getName();
    }

    public NucModelType getNucSubstitutionModel() {
        return nucSubstitutionModel;
    }

    public void setNucSubstitutionModel(NucModelType nucSubstitutionModel) {
        this.nucSubstitutionModel = nucSubstitutionModel;
    }

    public AminoAcidModelType getAaSubstitutionModel() {
        return aaSubstitutionModel;
    }

    public void setAaSubstitutionModel(AminoAcidModelType aaSubstitutionModel) {
        this.aaSubstitutionModel = aaSubstitutionModel;
    }

    public int getBinarySubstitutionModel() {
        return binarySubstitutionModel;
    }

    public void setBinarySubstitutionModel(int binarySubstitutionModel) {
        this.binarySubstitutionModel = binarySubstitutionModel;
    }

    public FrequencyPolicy getFrequencyPolicy() {
        return frequencyPolicy;
    }

    public void setFrequencyPolicy(FrequencyPolicy frequencyPolicy) {
        this.frequencyPolicy = frequencyPolicy;
    }

    public boolean isGammaHetero() {
        return gammaHetero;
    }

    public void setGammaHetero(boolean gammaHetero) {
        this.gammaHetero = gammaHetero;
    }

    public int getGammaCategories() {
        return gammaCategories;
    }

    public void setGammaCategories(int gammaCategories) {
        this.gammaCategories = gammaCategories;
    }

    public boolean isInvarHetero() {
        return invarHetero;
    }

    public void setInvarHetero(boolean invarHetero) {
        this.invarHetero = invarHetero;
    }

    public String getCodonHeteroPattern() {
        return codonHeteroPattern;
    }

    public void setCodonHeteroPattern(String codonHeteroPattern) {
        this.codonHeteroPattern = codonHeteroPattern;
    }

    /**
     * @return true if the rate matrix parameters are unlinked across codon positions
     */
    public boolean isUnlinkedSubstitutionModel() {
        return unlinkedSubstitutionModel;
    }

    public void setUnlinkedSubstitutionModel(boolean unlinkedSubstitutionModel) {
        this.unlinkedSubstitutionModel = unlinkedSubstitutionModel;
    }

    public boolean isUnlinkedHeterogeneityModel() {
        return unlinkedHeterogeneityModel;
    }

    public void setUnlinkedHeterogeneityModel(boolean unlinkedHeterogeneityModel) {
        this.unlinkedHeterogeneityModel = unlinkedHeterogeneityModel;
    }

    public boolean isUnlinkedFrequencyModel() {
        return unlinkedFrequencyModel;
    }

    public void setUnlinkedFrequencyModel(boolean unlinkedFrequencyModel) {
        this.unlinkedFrequencyModel = unlinkedFrequencyModel;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public boolean isDolloModel() {
        return dolloModel;
    }

    public void setDolloModel(boolean dolloModel) {
        this.dolloModel = dolloModel;
    }

    public String getPrefix() {
        String prefix = "";
        if (options.getActivePartitionSubstitutionModels().size() > 1 || options.isSpeciesAnalysis()) {
            // There is more than one active partition model, or doing species analysis
            prefix += getName() + ".";
        }
        return prefix;
    }

    public String getPrefix(int codonPartitionNumber) {
        String prefix = "";
        if (options.getActivePartitionSubstitutionModels().size() > 1 || options.isSpeciesAnalysis()) {
            // There is more than one active partition model, or doing species analysis
            prefix += getName() + ".";
        }
        if (getCodonPartitionCount() > 1 && codonPartitionNumber > 0) {
            if (getCodonHeteroPattern().equals("123")) {
                prefix += "CP" + codonPartitionNumber + ".";
            } else if (getCodonHeteroPattern().equals("112")) {
                if (codonPartitionNumber == 1) {
                    prefix += "CP1+2.";
                } else {
                    prefix += "CP3.";
                }
            } else {
                throw new IllegalArgumentException("unsupported codon hetero pattern");
            }

        }
        return prefix;
    }

    // Instance variables

    private final BeautiOptions options;

    private NucModelType nucSubstitutionModel = NucModelType.HKY;
    private AminoAcidModelType aaSubstitutionModel = AminoAcidModelType.BLOSUM_62;
    private int binarySubstitutionModel = BeautiOptions.BIN_SIMPLE;

    private FrequencyPolicy frequencyPolicy = FrequencyPolicy.ESTIMATED;
    private boolean gammaHetero = false;
    private int gammaCategories = 4;
    private boolean invarHetero = false;
    private String codonHeteroPattern = null;
    private boolean unlinkedSubstitutionModel = false;
    private boolean unlinkedHeterogeneityModel = false;

    private boolean unlinkedFrequencyModel = false;

    private boolean dolloModel = false;

    public DataType dataType;
    public String name;
    
    public Parameter localClockRateChangesStatistic = null;
    public Parameter localClockRatesStatistic = null;
}