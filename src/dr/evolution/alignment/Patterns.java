/*
 * Patterns.java
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
import dr.evolution.util.Taxon;
import dr.evolution.util.TaxonList;

/**
 * A concrete implementation of PatternList. Patterns can be added and
 * removed from the list individually or in bulk from an alignment.
 *
 * @version $Id: Patterns.java,v 1.10 2005/07/08 11:27:53 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class Patterns implements PatternList {

    public static final int COUNT_INCREMENT = 100;

    /** number of patterns */
    protected int patternCount = 0;

    /** length of patterns */
    protected int patternLength = 0;

    /** weights of each pattern */
    protected double[] weights = new double[COUNT_INCREMENT];

    /** site patterns [pattern][taxon] */
    protected int[][] patterns = new int[COUNT_INCREMENT][];

    protected DataType dataType = null;

    protected TaxonList taxonList = null;

    /**
     * Constructor
     */
    public Patterns(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * Constructor
     */
    public Patterns(DataType dataType, TaxonList taxonList) {
        this.dataType = dataType;
        this.taxonList = taxonList;
    }

    /**
     * Constructor
     */
    public Patterns(SiteList siteList) {
        addPatterns(siteList, 0, 0, 1);
    }

    /**
     * Constructor
     */
    public Patterns(SiteList siteList, int from, int to, int every) {
        addPatterns(siteList, from, to, every);
    }

	/**
	 * Constructor
	 */
	public Patterns(PatternList patternList) {
	    addPatterns(patternList);
	}

    /**
     * adds patterns to the list from a SiteList
     */
    public void addPatterns(SiteList siteList, int from, int to, int every) {

        if (siteList == null) {
            return;
        }

        if (taxonList == null) {
            taxonList = siteList;
            patternLength = taxonList.getTaxonCount();
        }

        if (dataType == null) {
            dataType = siteList.getDataType();
        } else if (dataType != siteList.getDataType()) {
            throw new IllegalArgumentException("Patterns' existing DataType does not match that of added SiteList");
        }

        if (from < 0)
            from = 0;

        if (to <= 0)
            to = siteList.getSiteCount() - 1;

        if (every <= 0)
            every = 1;

        for (int i = from; i <= to; i += every) {
            int[] pattern = siteList.getSitePattern(i);

	        // don't add patterns that are all gaps or all ambiguous
            if (!isInvariant(pattern) ||
                    (	!isGapped(pattern) &&
                    !isAmbiguous(pattern) &&
                    !isUnknown(pattern) ) ) {

                addPattern(pattern, 1.0);
            }
        }

    }

	/**
	 * adds patterns to the list from a SiteList
	 */
	public void addPatterns(PatternList patternList) {

	    if (patternList == null) {
	        return;
	    }

	    if (taxonList == null) {
	        taxonList = patternList;
	        patternLength = taxonList.getTaxonCount();
	    }

	    if (dataType == null) {
	        dataType = patternList.getDataType();
	    } else if (dataType != patternList.getDataType()) {
	        throw new IllegalArgumentException("Patterns' existing DataType does not match that of added PatternList");
	    }

	    for (int i = 0; i < patternList.getPatternCount(); i++) {
	        int[] pattern = patternList.getPattern(i);

		    // don't add patterns that are all gaps or all ambiguous
	        if (!isInvariant(pattern) ||
	                (	!isGapped(pattern) &&
	                !isAmbiguous(pattern) &&
	                !isUnknown(pattern) ) ) {

	            addPattern(pattern, patternList.getPatternWeight(i));
	        }
	    }

	}

    /**
     * adds a pattern to the pattern list with a default weight of 1
     */
    public void addPattern(int[] pattern) {
        addPattern(pattern, 1.0);
    }

    /**
     * adds a pattern to the pattern list
     */
    public void addPattern(int[] pattern, double weight) {

        if (patternLength == 0) {
            patternLength = pattern.length;
        }

        if (patternLength != 0 && pattern.length != patternLength) {
            throw new IllegalArgumentException("Added pattern's length (" + pattern.length + ") does not match those of existing patterns (" + patternLength + ")");
        }

        for (int i = 0; i < patternCount; i++) {

            if (comparePatterns(patterns[i], pattern)) {

                weights[i] += weight;
                return;
            }
        }

        if (patternCount == patterns.length) {
            int[][] newPatterns = new int[patternCount + COUNT_INCREMENT][];
            double[] newWeights = new double[patternCount + COUNT_INCREMENT];
            for (int i = 0; i < patternCount; i++) {
                newPatterns[i] = patterns[i];
                newWeights[i] = weights[i];
            }
            patterns = newPatterns;
            weights = newWeights;
        }

        patterns[patternCount] = pattern;
        weights[patternCount] = weight;
        patternCount++;
    }

    /**
     * removes a pattern from the pattern list
     */
    public void removePattern(int[] pattern) {

        int index = -1;
        for (int i = 0; i < patternCount; i++) {

            if (comparePatterns(patterns[i], pattern)) {
                index = i;
                break;
            }
        }

        if (index == -1) throw new IllegalArgumentException("Pattern not found");

        weights[index] -= 1;
        if (weights[index] == 0 && patternCount > 1) {
            patterns[index] = patterns[patternCount - 1];
            patterns[patternCount - 1] = null;
            weights[index] = weights[patternCount - 1];
            patternCount--;
        }

    }

    /**
     * removes all patterns from the pattern list
     */
    public void removeAllPatterns() {
        patternCount = 0;
        for (int i = 0; i < patterns.length; i++) patterns[i] = null;
    }

    /**
     * @return true if the pattern has one or more gaps
     */
    private boolean isGapped(int[] pattern) {
        int len = pattern.length;

        for (int i = 0; i < len; i++) {
            if (getDataType().isGapState(pattern[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the pattern has one or more ambiguous states
     */
    private boolean isAmbiguous(int[] pattern) {
        int len = pattern.length;

        for (int i = 0; i < len; i++) {
            if (getDataType().isAmbiguousState(pattern[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the pattern is invariant
     */
    private boolean isUnknown(int[] pattern) {
        int len = pattern.length;

        for (int i = 0; i < len; i++) {
            if (getDataType().isUnknownState(pattern[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the pattern is invariant
     */
    private boolean isInvariant(int[] pattern) {
        int len = pattern.length;

        int state = pattern[0];
        for (int i = 1; i < len; i++) {
            if (pattern[i] != state) {
                return false;
            }
        }

        return true;
    }

    /**
     * compares two patterns
     * @return true if they are identical
     */
    private boolean comparePatterns(int[] pattern1, int[] pattern2) {

        int len = pattern1.length;
        for (int i = 0; i < len; i++) {
            if (pattern1[i] != pattern2[i]) {
                return false;
            }
        }

        return true;
    }

    // **************************************************************
    // PatternList IMPLEMENTATION
    // **************************************************************

    /**
     * @return number of patterns
     */
    public int getPatternCount() {
        return patternCount;
    }

    /**
     * @return number of states for this siteList
     */
    public int getStateCount() {
        return dataType.getStateCount();
    }

    /**
     * Gets the length of the pattern strings which will usually be the
     * same as the number of taxa
     * @return the length of patterns
     */
    public int getPatternLength() {
        return patternLength;
    }

    /**
     * Gets the pattern as an array of state numbers (one per sequences)
     * @return the pattern at patternIndex
     */
    public int[] getPattern(int patternIndex) {
        return patterns[patternIndex];
    }

    /**
     * @return state at (taxonIndex, patternIndex)
     */
    public int getPatternState(int taxonIndex, int patternIndex) {
        return patterns[patternIndex][taxonIndex];
    }

    /**
     * Gets the weight of a site pattern
     */
    public double getPatternWeight(int patternIndex) {
        return weights[patternIndex];
    }

    /**
     * @return the array of pattern weights
     */
    public double[] getPatternWeights() {
        double[] w = new double[weights.length];
        for (int i = 0; i < weights.length; i++) w[i] = weights[i];
        return w;
    }

    /**
     * @return the DataType of this siteList
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @return the frequency of each state
     */
    public double[] getStateFrequencies() {
        return PatternList.Utils.empiricalStateFrequencies(this);
    }

    // **************************************************************
    // TaxonList IMPLEMENTATION
    // **************************************************************

    /**
     * @return a count of the number of taxa in the list.
     */
    public int getTaxonCount() {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonCount();
    }

    /**
     * @return the ith taxon.
     */
    public Taxon getTaxon(int taxonIndex) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxon(taxonIndex);
    }

    /**
     * @return the ID of the ith taxon.
     */
    public String getTaxonId(int taxonIndex) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonId(taxonIndex);
    }

    /**
     * returns the index of the taxon with the given id.
     */
    public int getTaxonIndex(String id) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonIndex(id);
    }

    /**
     * returns the index of the given taxon.
     */
    public int getTaxonIndex(Taxon taxon) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonIndex(taxon);
    }

    /**
     * @return an object representing the named attributed for the given taxon.
     * @param taxonIndex the index of the taxon whose attribute is being fetched.
     * @param name the name of the attribute of interest.
     */
    public Object getTaxonAttribute(int taxonIndex, String name) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonAttribute(taxonIndex, name);
    }

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

    protected String id = null;

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     */
    public void setId(String id) {
        this.id = id;
    }
}
