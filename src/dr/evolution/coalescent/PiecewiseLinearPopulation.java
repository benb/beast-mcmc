/*
 * PiecewiseLinearPopulation.java
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

package dr.evolution.coalescent;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: PiecewiseLinearPopulation.java,v 1.7 2005/05/24 20:25:56 rambaut Exp $
 */
public class PiecewiseLinearPopulation extends PiecewiseConstantPopulation {

    /**
     * Construct demographic model with default settings
     */
    public PiecewiseLinearPopulation(double[] intervals, double[] thetas, Type units) {

        super(intervals, thetas, units);
    }

    // **************************************************************
    // Implementation of abstract methods
    // **************************************************************

    /**
     * @return the value of the demographic function for the given epoch and time relative to start of epoch.
     */
    protected final double getDemographic(int epoch, double t) {
        // if in last epoch then the population is flat.
        if (epoch == (thetas.length - 1)) {
            return getEpochDemographic(epoch);
        }

        final double popSize1 = getEpochDemographic(epoch);
        final double popSize2 = getEpochDemographic(epoch + 1);

        final double width = getEpochDuration(epoch);

        assert 0 <= t && t <= width;

        return popSize1 + (t / width) * (popSize2 - popSize1);
    }

    public DemographicFunction getCopy() {
        PiecewiseLinearPopulation df = new PiecewiseLinearPopulation(new double[intervals.length], new double[thetas.length], getUnits());
        System.arraycopy(intervals, 0, df.intervals, 0, intervals.length);
        System.arraycopy(thetas, 0, df.thetas, 0, thetas.length);

        return df;
    }

    /**
     * @return the value of the intensity function for the given epoch.
     */
    protected final double getIntensity(int epoch) {
        return 2.0 * getEpochDuration(epoch) / (getEpochDemographic(epoch) + getEpochDemographic(epoch + 1));
    }

    /**
     * @return the value of the intensity function for the given epoch and time relative to start of epoch.
     */
    protected final double getIntensity(int epoch, double relativeTime) {
        assert relativeTime <= getEpochDuration(epoch);

        return 2.0 * relativeTime / (getEpochDemographic(epoch) + getDemographic(epoch, relativeTime));
    }

    /**
     * @param targetI the intensity
     * @return the time corresponding to the given target intensity
     */
    public final double getInverseIntensity(double targetI) {

        int epoch = 0;
        double cI = 0;
        double eI = getIntensity(epoch);
        double time = 0.0;
        while (cI + eI < targetI) {
            cI += eI;
            time += getEpochDuration(epoch);

            epoch += 1;
            eI = getIntensity(epoch);
        }

        time += getInverseIntensity(epoch, targetI - cI);

        return time;
    }

    /**
     * @param epoch the epoch
     * @param I     intensity corresponding to relative time within this epoch
     * @return the relative time within the given epoch to accumulate the given residual intensity
     */
    private double getInverseIntensity(int epoch, double I) {

        final double N1 = getEpochDemographic(epoch);
        final double N2 = getEpochDemographic(epoch + 1);
        final double w = getEpochDuration(epoch);

        double time = 2 * N1 * I * w / (2 * w + I * (N1 - N2));

        return time;
    }
}
