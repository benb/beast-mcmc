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

import dr.app.beauti.enumTypes.FixRateType;

import java.util.List;


/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 * @version $Id$
 */
public class PriorOptions extends ModelOptions {

	// Instance variables
    private final BeautiOptions options;
   

    public PriorOptions(BeautiOptions options) {    	
    	this.options = options;
    }
       
    
    /**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectParameters(List<Parameter> params) {    	    	
        
        double growthRateMaximum = 1E6;
//        double birthRateMaximum = 1E6;
//        double substitutionRateMaximum = 100;
//        double logStdevMaximum = 10;
//        double substitutionParameterMaximum = 100;
                
        double[] rootAndRate = options.clockModelOptions.calculateInitialRootHeightAndRate(BeautiOptions.getNonTraitsDataList());
        double avgInitialRootHeight = rootAndRate[0];
        double avgInitialRate = rootAndRate[1];
        
        if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.FIX_MEAN
    			|| options.clockModelOptions.getRateOptionClockModel() == FixRateType.RELATIVE_TO) {
        	
            growthRateMaximum = 1E6 * avgInitialRate;
//            birthRateMaximum = 1E6 * avgInitialRate;
        }
        
//        if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.FIX_MEAN) {
//            double rate = options.clockModelOptions.getMeanRelativeRate();
//
//            growthRateMaximum = 1E6 * rate;
//            birthRateMaximum = 1E6 * rate;
//
//            if (options.hasData()) {
//                initialRootHeight = meanDistance / rate;
//
//                initialRootHeight = round(initialRootHeight, 2);
//            }
//
//        } else {
//            if (options.maximumTipHeight > 0) {
//                initialRootHeight = options.maximumTipHeight * 10.0;
//            }
//
//            initialRate = round((meanDistance * 0.2) / initialRootHeight, 2);
//        }

//        double timeScaleMaximum = MathUtils.round(avgInitialRootHeight * 1000.0, 2);

        for (Parameter param : params) {
            if (!options.hasData()) param.setPriorEdited(false);

            if (!param.isPriorEdited()) {
                switch (param.scaleType) {
                    case TIME_SCALE:
//                        param.lower = Math.max(0.0, param.lower);
                        //param.upper = Math.min(timeScaleMaximum, param.upper);
                        param.initial = avgInitialRootHeight;
                        break;
                        
                    case T50_SCALE:
//                        param.lower = Math.max(0.0, param.lower);
                        //param.upper = Math.min(timeScaleMaximum, param.upper);
                        param.initial = avgInitialRootHeight / 5.0;
                        break;
                        
                    case GROWTH_RATE_SCALE:
//                        param.lower = Math.max(-growthRateMaximum, param.lower);
                        //param.upper = Math.min(growthRateMaximum, param.upper);
                        break;
                        
                    case BIRTH_RATE_SCALE:
//                        param.lower = Math.max(0.0, param.lower);
                        //param.upper = Math.min(birthRateMaximum, param.upper);
                        break;
                        
                    case SUBSTITUTION_RATE_SCALE:
//                        param.lower = Math.max(0.0, param.lower);
                        //param.upper = Math.min(substitutionRateMaximum, param.upper);
                        param.initial = avgInitialRate;                                             
                        break;
                        
                    case LOG_STDEV_SCALE:
//                        param.lower = Math.max(0.0, param.lower);
                        //param.upper = Math.min(logStdevMaximum, param.upper);
                        break;
                        
                    case SUBSTITUTION_PARAMETER_SCALE:
//                        param.lower = Math.max(0.0, param.lower);
                        //param.upper = Math.min(substitutionParameterMaximum, param.upper);
                        break;

                    case UNITY_SCALE:
                        param.lower = 0.0;
                        param.upper = 1.0;
                        break;

                    case ROOT_RATE_SCALE:
                        param.initial = avgInitialRate;
                        param.shape = 0.5;
                        param.scale = param.initial / 0.5;
                        break;

                    case LOG_VAR_SCALE:
                        param.initial = avgInitialRate;
                        param.shape = 2.0;
                        param.scale = param.initial / 2.0;
                        break;

                }
                
                if (param.isNodeHeight) { //TODO only affecting "treeModel.rootHeight", need to review
                    param.lower = options.maximumTipHeight;
//                    param.upper = timeScaleMaximum;
//                    param.initial = avgInitialRootHeight;
                    if (param.getOptions() instanceof PartitionTreeModel) {
                        param.initial = ((PartitionTreeModel) param.getOptions()).getInitialRootHeight();
                    } else {
                        param.initial = avgInitialRootHeight;
                    }
                }
            }
        }

//        dataReset = false;
        
        
    }

}
