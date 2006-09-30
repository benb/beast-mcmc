/*
 * MCMCOperator.java
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

package dr.inference.operators;

/**
 * An MCMC operator.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * @version $Id: MCMCOperator.java,v 1.6 2005/06/14 10:40:34 rambaut Exp $
 */
public interface MCMCOperator {

	public static final String WEIGHT = "weight";
	
// This attribute is now called AUTO_OPTIMIZE and is in CoercableMCMCOperator	
//	public static final String ADAPT = "adapt";
	
	/** 
	 * operates on the model.
	 * @return the hastings ratio of this operator.
	 */
	double operate() throws OperatorFailedException;
	
	/**
	 * Called to tell operator that operation was accepted
	 */
	void accept(double deviation);
	
	/**
	 * Called to tell operator that operation was rejected
	 */
	void reject();
	
	/**
	 * Reset operator acceptance records.
	 */
	void reset();
	
	/**
	 * @return the number of acceptances since last call to reset().
	 */
	int getAccepted();

    /**
     * Set the number of acceptances since last call to reset(). This is used
     * to restore the state of the operator
     */
    void setAccepted(int accepted);

	/**
	 * @return the number of rejections since last call to reset().
	 */
	int getRejected();
	
    /**
     * Set the number of rejections since last call to reset(). This is used
     * to restore the state of the operator
     */
    void setRejected(int rejected);
    
	/**
	 * @return the mean deviation in log posterior per accepted operations.
	 */
	double getMeanDeviation();
	
    double getSumDeviation();

    void setDumDeviation(double sumDeviation);
    
	/**
	 * @return the optimal acceptance probability
	 */
	double getTargetAcceptanceProbability();
	
	/**
	 * @return the minimum acceptable acceptance probability
	 */
	double getMinimumAcceptanceLevel();

	/**
	 * @return the maximum acceptable acceptance probability
	 */
	double getMaximumAcceptanceLevel();
	
	/**
	 * @return the minimum good acceptance probability
	 */
	double getMinimumGoodAcceptanceLevel();

	/**
	 * @return the maximum good acceptance probability
	 */
	double getMaximumGoodAcceptanceLevel();
	
	/**
	 * @return a short descriptive message of the performance of this operator.
	 */
	String getPerformanceSuggestion();

	/**
	 * @return the weight of this operator.
	 */
	int getWeight();

	/**
	 * sets the weight of this operator. The weight
	 * determines the proportion of time spent using
	 * this operator. This is relative to a 'standard'
	 * operator weight of 1. 
	 */
	void setWeight(int w);

	/**
	 * @return the name of this operator 
	 */
	String getOperatorName();

	class Utils {
	
		public static double getAcceptanceProbability(MCMCOperator op) {
			int accepted = op.getAccepted();
			int rejected = op.getRejected();
			return (double)accepted / (double)(accepted + rejected);
		}
		
		public static int getOperationCount(MCMCOperator op) {
			return op.getAccepted() + op.getRejected();
		}
	}

}
