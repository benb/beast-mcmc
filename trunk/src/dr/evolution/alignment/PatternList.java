/*
 * PatternList.java
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

package dr.evolution.alignment;

import dr.evolution.datatype.DataType;
import dr.evolution.util.TaxonList;
import dr.util.Identifiable;

/**
 * interface for any list of patterns with weights.
 *
 * @version $Id: PatternList.java,v 1.12 2005/05/24 20:25:55 rambaut Exp $
 *
 * @author Andrew Rambaut
 */
public interface PatternList extends TaxonList, Identifiable
{
	/**
	 * @return number of patterns
	 */
	int getPatternCount();
	
	/**
	 * @return number of states for this PatternList
	 */
	int getStateCount();

	/**
	 * Gets the length of the pattern strings which will usually be the
	 * same as the number of taxa
	 * @return the length of patterns
	 */
	int getPatternLength();

	/** 
	 * Gets the pattern as an array of state numbers (one per sequence) 
	 * @return the site pattern at patternIndex
	 */
	int[] getPattern(int patternIndex);

	/** 
	 * @return state at (taxonIndex, patternIndex) 
	 */
	int getPatternState(int taxonIndex, int patternIndex);

	/** 
	 * Gets the weight of a site pattern
	 */
	double getPatternWeight(int patternIndex);

	/**
	 * @return the array of pattern weights
	 */
	double[] getPatternWeights();

	/**
	 * @return the DataType of this PatternList
	 */
	DataType getDataType();

	/**
	 * @return the frequency of each state
	 */
	double[] getStateFrequencies();

	/**
	 * Helper routines for pattern lists.
	 */
	public static class Utils {
		/**
		 * Returns a double array containing the empirically estimated frequencies
		 * for the states of patternList. This function deals correctly with any
		 * state ambiguities.
		 */
		public static double[] empiricalStateFrequencies(PatternList patternList) {
			int i, j, k;
			double total, sum, x, w, difference;

			DataType dataType = patternList.getDataType();
			
			int stateCount = patternList.getStateCount();
			int patternLength = patternList.getPatternLength();
			int patternCount = patternList.getPatternCount();
			
			double[] freqs = equalStateFrequencies(patternList);
				
			double[] tempFreq = new double[stateCount];
			int[] pattern;
			boolean[] state;

			do {
				for (i = 0; i < stateCount; i++)
					tempFreq[i] = 0.0;
					
				total=0.0;
				for (i = 0; i < patternCount; i++) {
					pattern = patternList.getPattern(i);
					w = patternList.getPatternWeight(i);
				
					for (k = 0; k < patternLength; k++) {
						state = dataType.getStateSet(pattern[k]);
						
						sum=0.0;
						for (j = 0; j < stateCount; j++)
							if (state[j])
								sum += freqs[j];
							
						for (j = 0; j < stateCount; j++) {
							if (state[j]) {
								x = (freqs[j] * w) / sum;
								tempFreq[j] += x;
								total += x;
							}
						}
					}
					
				}
				
				difference = 0.0;
				for (i = 0; i < stateCount; i++) {
					difference += Math.abs((tempFreq[i] / total) - freqs[i]);
					freqs[i] = tempFreq[i] / total;
				}
			} while (difference > 1E-8);
			
			return freqs;
		}

		/**
		 * Returns a double array containing the equal frequencies
		 * for the states of patternList.
		 */
		public static double[] equalStateFrequencies(PatternList patternList) {
			int i, n = patternList.getStateCount();
			double[] freqs = new double[n];
			double f = 1.0 / n;
			
			for (i = 0; i < n; i++) 
				freqs[i] = f;
			
			return freqs;
		}
	}
}
