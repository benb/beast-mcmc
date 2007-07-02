/*
 * VariableSkylineLikelihood.java
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

package dr.evomodel.coalescent;

import dr.evolution.tree.Tree;
import dr.evolution.coalescent.ConstantPopulation;
import dr.evolution.coalescent.Coalescent;
import dr.evolution.util.Units;
import dr.inference.model.Parameter;
import dr.inference.model.Likelihood;
import dr.evomodel.tree.TreeModel;
import dr.xml.*;

import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;

/**
 * A likelihood function for the variable skyline plot coalescent.
 * Takes a tree, population size and an indicator parameter.
 *
 * @author Alexei Drummond
 */
public class VariableSkylineLikelihood extends CoalescentLikelihood {

    // PUBLIC STUFF

    public static final String SKYLINE_LIKELIHOOD = "ovariableSkyLineLikelihood";
    public static final String POPULATION_SIZES = "populationSizes";
    public static final String INDICATOR_PARAMETER = "indicators";
    public static final String LOG_SPACE = "logUnits";

    public static final String TYPE = "type";
    public static final String STEPWISE = "stepwise";
    public static final String LINEAR = "linear";
    public static final String EXPONENTIAL = "exponential";


    public enum Type {
        STEPWISE,
        LINEAR,
        EXPONENTIAL
    }

    public VariableSkylineLikelihood(Tree tree, Parameter popSizeParameter, Parameter indicatorParameter,
                                     Type type, boolean logSpace) {
        super(SKYLINE_LIKELIHOOD);

        this.popSizeParameter = popSizeParameter;
        this.indicatorParameter = indicatorParameter;
        final int redcueDim = type == Type.STEPWISE ? 1 : 0;
        final int events = tree.getExternalNodeCount() - redcueDim;
        final int paramDim1 = popSizeParameter.getDimension();
        final int paramDim2 = indicatorParameter.getDimension();
        this.type = type;
        this.logSpace = logSpace;

        if (paramDim1 != events) {
            throw new IllegalArgumentException("Dimension of population parameter must be the same as the number of internal nodes in the tree. ("
            + paramDim1 + " != " + events + ")");
        }
        if (paramDim2 != events - 1) {
            throw new IllegalArgumentException("Dimension of indicator parameter must one less than the number of internal nodes in the tree. ("
            + paramDim2 + " != " + (events-1) + ")");
        }

        this.tree = tree;
        if (tree instanceof TreeModel) {
            addModel((TreeModel) tree);
        }

        addParameter(indicatorParameter);
        addParameter(popSizeParameter);

        setupIntervals();
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     */
    public synchronized double getLogLikelihood() {

        insureValid();

        double logL = 0.0;

        double currentTime = 0.0;

        // index of current interval in the population function
        int groupIndex = 0;

        // how many intervals precessed in the current group - tell when to switch to next group
        int subIndex = 0;
        ConstantPopulation cp = new ConstantPopulation(Units.Type.YEARS);

        final double[] pop = new double[1];
        if( type == Type.LINEAR ) {
            cp = new ConstantPopulation(Units.Type.YEARS) {
                public double getDemographic(double t) {
                    return pop[0];
                }
            };
        }

        if( Coalescent.detaildPrint ) { System.err.println("Old:"); }

        for (int j = 0; j < intervalCount; j++) {

            // set the population size to the size of the middle of the current interval
            if( type == Type.LINEAR ) {
                pop[0] = getPopSize(groupIndex, currentTime + intervals[j]);
            }
            cp.setN0(getPopSize(groupIndex, currentTime + (intervals[j] / 2.0)));
            final CoalescentEventType iType = getIntervalType(j);
            if (iType == CoalescentEventType.COALESCENT) {
                subIndex += 1;
                if (subIndex >= groupSizes.get(groupIndex)) {
                    groupIndex += 1;
                    subIndex = 0;
                }
            }

            logL += calculateIntervalLikelihood(cp, intervals[j], currentTime, lineageCounts[j], iType);

            if( Coalescent.detaildPrint ) { System.err.println(" lgl " + logL); }

            if( logL > 0 && j > 1 ) {
                System.out.println(logL);
            }

            // insert zero-length coalescent intervals
            final int diff = getCoalescentEvents(j) - 1;
            for (int k = 0; k < diff; k++) {
                // not clear, seems wrong - how can population size change in 0 time?
                pop[0] = getPopSize(groupIndex, currentTime);
                cp.setN0(pop[0]);
                logL += calculateIntervalLikelihood(cp, 0.0, currentTime, lineageCounts[j] - k - 1,
                        CoalescentEventType.COALESCENT);
                subIndex += 1;
                if (subIndex >= groupSizes.get(groupIndex)) {
                    groupIndex += 1;
                    subIndex = 0;
                }
            }

            currentTime += intervals[j];
        }

        return logL;
    }

    private void insureValid() {
        if( !intervalsKnown ) {
            //            System.out.println("**" + intervalsKnown);
            //            assert !intervalsKnown;
            setupIntervals();
            groupsValid = false;
        }

        if( ! groupsValid ) {
            calculateGroupSizesHeightsAndEnds();
            popPoints = null;
        }
    }

    private double[] popPoints = null;

    public double[] getPopulation(int dim) {
        insureValid();

        if( popPoints != null && popPoints.length == dim ) {
            return popPoints;
        }
       
        if( popPoints == null || popPoints.length != dim) {
            popPoints = new double[dim];
        }

        final double height = tree.getNodeHeight(tree.getRoot());
        double hstep = height / popPoints.length;
        double h = 0;
        int groupIndex = 0;
        double currentTime = 0.0;
        double switchAt = groupEnds.get(groupIndex);
        for(int k = 0; k < popPoints.length-1; ++k) {
            popPoints[k] = getPopSize(groupIndex, currentTime);
            currentTime += hstep;
            while( currentTime >= switchAt ) {
                ++groupIndex;
                switchAt = groupEnds.get(groupIndex);
            }
        }
        popPoints[popPoints.length-1] = getPopSize(groupIndex, currentTime);
        return popPoints;
    }

    /**
     * @return the population size for the given time. If linear model is being used then this pop size is
     *         interpolated between the two pop sizes at either end of the grouped interval.
     * @param groupIndex time inside this group
     * @param atTime given time
     */
    private double getPopSize(int groupIndex, double atTime) {

        final double startGroupTime = groupIndex > 0 ? groupEnds.get(groupIndex - 1) : 0.0;
        final double endGroupTime = groupEnds.get(groupIndex);
        final double startGroupPopSize = groupHeights.get(groupIndex);

        if(! ( startGroupTime <= atTime &&
                ((type != Type.STEPWISE && atTime <= endGroupTime) ||
                 (type == Type.STEPWISE && atTime < endGroupTime))) ) {
            System.out.println(atTime + " " + startGroupTime + "/" + endGroupTime);
        }

        switch( type ) {
            case EXPONENTIAL:
            {
              throw new UnsupportedOperationException("Exponential Skyline Plot not implemented yet");
            }
            case LINEAR:
            {
                if( groupIndex + 1 >= groupHeights.size() ) {
                    return startGroupPopSize;
                }
                
                final double endGroupPopSize = groupHeights.get(groupIndex + 1);

                // calculate the gradient
                //final double m = (endGroupPopSize - startGroupPopSize) / (endGroupTime - startGroupTime);
                final double a = (atTime - startGroupTime) / (endGroupTime - startGroupTime);
                // calculate the population size at atTime using linear interpolation

                double v = (1-a) * startGroupPopSize + a * endGroupPopSize;
                if( v <= 0 ) {
                    // numerical problems, very small endGroupPopSize
                    if( a == 1 ) {
                       assert endGroupPopSize > 0;
                       return endGroupPopSize;
                    }
                    System.out.println(v);
                    assert v > 0;
                }
                return v;
            }
            case STEPWISE:
            {
              return startGroupPopSize;
            }
        }

        return -1; // never reached
    }

    private void calculateGroupSizesHeightsAndEnds() {
//        System.out.println(intervalsKnown);
//        if( !intervalsKnown ) {
////            System.out.println("**" + intervalsKnown);
////            assert !intervalsKnown;
//            setupIntervals();
//       }
        // need to do this only if demo model or indicator changed
        groupSizes.clear();
        groupHeights.clear();
        groupEnds.clear();

        int groupSize = 1;
        int nextPopSizeIndex = 0;
        double height = 0.0;
        for (int i = 0; i < indicatorParameter.getDimension(); i++) {
            height += intervals[i];
            if (indicatorParameter.getParameterValue(i) != 0.0) {
                groupSizes.add(groupSize);
                groupHeights.add(popSizeParameter.getParameterValue(nextPopSizeIndex));
                groupSize = 1;
                nextPopSizeIndex = i + 1;

                groupEnds.add(height);

            } else {
                groupSize += 1;
            }
        }
        height += intervals[indicatorParameter.getDimension()];

        groupSizes.add(groupSize);
        groupHeights.add(popSizeParameter.getParameterValue(nextPopSizeIndex));
        groupEnds.add(height);

        if (logSpace) {
            for (int i = 0; i < groupHeights.size(); i++) {
                groupHeights.set(i, Math.exp(groupHeights.get(i)));
            }
        }

        groupsValid = true;
    }

    protected void handleParameterChangedEvent(Parameter parameter, int index) {
        super.handleParameterChangedEvent(parameter, index);
//        if( parameter == indicatorParameter || parameter == popSizeParameter ) {
        groupsValid = false;
//        }
    }

    protected void storeState() {
        super.storeState();
        storeSizes.clear();  storeSizes.addAll(groupSizes);
        storeHeights.clear();  storeHeights.addAll(groupHeights);
        storeEnds.clear();   storeEnds.addAll(groupEnds);
        storeValid = groupsValid;
    }

    protected void restoreState() {
        super.restoreState();
        groupsValid = storeValid;
        groupSizes.clear(); groupSizes.addAll(storeSizes);
        groupHeights.clear(); groupHeights.addAll(storeHeights);
        groupEnds.clear(); groupEnds.addAll(storeEnds);
    }
    
    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return VariableSkylineLikelihood.SKYLINE_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            XMLObject cxo = (XMLObject) xo.getChild(VariableSkylineLikelihood.POPULATION_SIZES);
            Parameter param = (Parameter) cxo.getChild(Parameter.class);

            cxo = (XMLObject) xo.getChild(VariableSkylineLikelihood.INDICATOR_PARAMETER);
            Parameter param2 = (Parameter) cxo.getChild(Parameter.class);

            cxo = (XMLObject) xo.getChild(POPULATION_TREE);
            TreeModel treeModel = (TreeModel) cxo.getChild(TreeModel.class);

            Type type = VariableSkylineLikelihood.Type.STEPWISE;
           /* if (xo.hasAttribute(VariableSkylineLikelihood.LINEAR) && !xo.getBooleanAttribute(VariableSkylineLikelihood.LINEAR))
            {
                type = VariableSkylineLikelihood.Type.STEPWISE;
            }*/

            if (xo.hasAttribute(VariableSkylineLikelihood.TYPE)) {
                final String s = xo.getStringAttribute(VariableSkylineLikelihood.TYPE);
                if (s.equalsIgnoreCase(VariableSkylineLikelihood.STEPWISE)) {
                    type = VariableSkylineLikelihood.Type.STEPWISE;
                } else if (s.equalsIgnoreCase(VariableSkylineLikelihood.LINEAR)) {
                    type = VariableSkylineLikelihood.Type.LINEAR;
                } else if (s.equalsIgnoreCase(VariableSkylineLikelihood.EXPONENTIAL)) {
                    type = VariableSkylineLikelihood.Type.EXPONENTIAL;
                } else {
                    throw new XMLParseException("Unknown Bayesian Skyline type: " + s);
                }
            }

            boolean logSpace = xo.getBooleanAttribute(LOG_SPACE);

            Logger.getLogger("dr.evomodel").info("Variable skyline plot: " + type.toString() + " control points");

            return new VariableSkylineLikelihood(treeModel, param, param2, type, logSpace);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the likelihood of the tree given the population size vector.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newBooleanRule(VariableSkylineLikelihood.TYPE, true),
                new ElementRule(VariableSkylineLikelihood.POPULATION_SIZES, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }),
                new ElementRule(VariableSkylineLikelihood.INDICATOR_PARAMETER, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }),
                new ElementRule(POPULATION_TREE, new XMLSyntaxRule[]{
                        new ElementRule(TreeModel.class)
                }),
                AttributeRule.newBooleanRule(LOG_SPACE)
        };
    };

    /**
     * The demographic model.
     */
    private final Parameter popSizeParameter;

    private final Parameter indicatorParameter;

    private boolean groupsValid = false;

    List<Integer> groupSizes = new ArrayList<Integer>();
    List<Double> groupHeights = new ArrayList<Double>();
    List<Double> groupEnds = new ArrayList<Double>();

    private ArrayList<Integer> storeSizes = new ArrayList<Integer>();
    private ArrayList<Double> storeHeights = new ArrayList<Double>();
    private ArrayList<Double> storeEnds = new ArrayList<Double>();
    private boolean storeValid;
    
    private final Type type;

    private boolean logSpace = false;
}
