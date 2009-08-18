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

import java.util.ArrayList;
import java.util.List;

import dr.app.beauti.components.ComponentFactory;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Walter Xie
 * @version $Id$
 */
public class ClockModelOptions extends ModelOptions {

	// Instance variables
    private final BeautiOptions options;
   
    private FixRateType rateOptionClockModel = FixRateType.ESTIMATE; 
    private double meanRelativeRate = 1.0;

    public ClockModelOptions(BeautiOptions options) {    	
    	this.options = options;
               
        initGlobalClockModelParaAndOpers();
        
        fixRateOfFirstClockPartition(); //TODO correct?
    }
    
    private void initGlobalClockModelParaAndOpers() {
    	
        createParameter("allClockRates", "All the relative rates regarding clock models");
    	
        createOperator("deltaAllClockRates", RelativeRatesType.CLOCK_RELATIVE_RATES.toString(),
        		"Delta exchange operator for all the relative rates regarding clock models", "allClockRates",      		 
        		OperatorType.DELTA_EXCHANGE, 0.75, rateWeights);
    	
    }

    /**
     * return a list of parameters that are required
     *
     * @param params the parameter list
     */
    public void selectParameters(List<Parameter> params) {    	    	
//    	if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.FIX_MEAN) {
// TODO       	
//        }
    }

    /**
     * return a list of operators that are required
     *
     * @param ops the operator list
     */
    public void selectOperators(List<Operator> ops) {
    	if (options.clockModelOptions.getRateOptionClockModel() == FixRateType.FIX_MEAN) {
    		Operator deltaOperator = getOperator("deltaAllClockRates");
    		
            // update delta clock operator weight
    		deltaOperator.weight = options.getPartitionClockModels().size();
            
    		ops.add(deltaOperator);
    	}
    }

	
    
    /////////////////////////////////////////////////////////////
    public FixRateType getRateOptionClockModel() {
		return rateOptionClockModel;
	}

	public void setRateOptionClockModel(FixRateType rateOptionClockModel) {
		this.rateOptionClockModel = rateOptionClockModel;
	}

	public void setMeanRelativeRate(double meanRelativeRate) {
		this.meanRelativeRate = meanRelativeRate;
	}

	public double getMeanRelativeRate() {
		return meanRelativeRate;
	}

	public int[] getPartitionClockWeights() {
		int[] weights = new int[options.getPartitionClockModels().size()]; // use List?

		int k = 0;
		for (PartitionClockModel model : options.getPartitionClockModels()) {
			for (PartitionData partition : model.getAllPartitionData()) {
				int n = partition.getSiteCount();
				weights[k] += n;
			}
			k += 1;
		}

		assert (k == weights.length);

		return weights;
	}	
	
	public void fixRateOfFirstClockPartition() {
		this.rateOptionClockModel = FixRateType.ESTIMATE;
		// fix rate of 1st partition
		int i = 0;
		for (PartitionClockModel model : options.getPartitionClockModels()) {
			if (i < 1) {
				model.setEstimatedRate(false);
			} else {
				model.setEstimatedRate(true);
			}
			i = i + 1;
        }
	}
	
	public void estimateAllRates() {
		this.rateOptionClockModel = FixRateType.ESTIMATE;
		
		for (PartitionClockModel model : options.getPartitionClockModels()) {
			model.setEstimatedRate(true);
        }
	}
    
	@Override
	public String getPrefix() {
		// TODO Auto-generated method stub
		return null;
	}


}
