/*
 * TraceCorrelation.java
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

package dr.app.tracer;



/**
 * A class that stores the correlation statistics for a trace
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: TraceCorrelation.java,v 1.5 2005/07/11 14:07:26 rambaut Exp $
 */
public class TraceCorrelation extends TraceDistribution {

	public TraceCorrelation(double[] values, int stepSize) {
		super(values, stepSize);
		
		if (isValid) {
			analyseCorrelation(values, stepSize);
		}
	}
	
	public double getStdErrorOfMean() { return stdErrorOfMean; }
	public double getACT() { return ACT; }
	
	private final static int MAX_OFFSET = 1000;
	
	/**
	 * Analyze trace
	 */
	private void analyseCorrelation(double[] values, int stepSize) {
	
		int maxLag = MAX_OFFSET;
		int samples = values.length;
		if ((samples/3) < maxLag) {
			maxLag = (samples/3);
		}	
			
		double[] gammaStat = new double[maxLag];
		double[] varGammaStat = new double[maxLag];
  		double varStat;
		double varVarStat;
		double assVarCor;
		double del1, del2;
			
		for (int lag=0; lag < maxLag; lag++) {
			for (int j = 0; j < samples-lag; j++) {
    			del1=values[j] - mean;
				del2=values[j + lag] - mean;
				gammaStat[lag] += ( del1*del2 ); 
				varGammaStat[lag] += (del1*del1*del2*del2);
			}
				
			gammaStat[lag] /= ((double)samples);
			varGammaStat[lag] /= ((double) samples-lag);
			varGammaStat[lag] -= (gammaStat[0] * gammaStat[0]);
		}
					
		varStat = gammaStat[0];
		varVarStat = varGammaStat[0];
		assVarCor = 1.0;
		
		int lag=1;
		while ((lag < maxLag-3) && (gammaStat[lag] + gammaStat[lag+1] > 0)) {
			varStat += (2.0*(gammaStat[lag]+gammaStat[lag+1]));
			varVarStat += (2.0*(varGammaStat[lag] + varGammaStat[lag+1]));
			assVarCor += (2.0*((gammaStat[lag] * gammaStat[lag]) + (gammaStat[lag+1] * gammaStat[lag+1])) / (gammaStat[0] * gammaStat[0]));
			if (gammaStat[lag]+gammaStat[lag+1] < gammaStat[lag+2]+gammaStat[lag+3] ) break;
			lag += 2;
		}
			
		// standard error of mean
		stdErrorOfMean = Math.sqrt(varStat/samples);
		
		// auto correlation time
		ACT = stepSize * varStat / gammaStat[0];
		
		// effective sample size
		ESS = (stepSize * samples) / ACT;
		
		// standard deviation of autocorrelation time
		stdErrOfACT = (2.0* Math.sqrt(2.0*(2.0*(double) lag+1)/samples)*(varStat/gammaStat[0])*stepSize);
	
		isValid = true;
	}
		
	//************************************************************************
	// private methods
	//************************************************************************
	
	protected double stdErrorOfMean;
	protected double stdErrorOfVariance;
	protected double ACT;
	protected double stdErrOfACT;
}