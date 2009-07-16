/*
 * DistributionLikelihood.java
 *
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

package dr.inference.distribution;

import dr.inference.model.Statistic;
import dr.math.distributions.Distribution;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class that returns the log likelihood of a set of data (statistics)
 * being distributed according to the given parametric distribution.
 *
 * @author Alexei Drummond
 * @version $Id: DistributionLikelihood.java,v 1.11 2005/05/25 09:35:28 rambaut Exp $
 */

public class DistributionLikelihood extends AbstractDistributionLikelihood {

    public static final String DISTRIBUTION_LIKELIHOOD = "distributionLikelihood";

    private int from = -1;
    private int to = Integer.MAX_VALUE;

    public DistributionLikelihood(Distribution distribution) {
        super(null);
        this.distribution = distribution;
        this.offset = 0.0;
    }

    public DistributionLikelihood(Distribution distribution, double offset) {
        super(null);
        this.distribution = distribution;
        this.offset = offset;
    }

    public DistributionLikelihood(ParametricDistributionModel distributionModel) {
        super(distributionModel);
        this.distribution = distributionModel;
        this.offset = 0.0;
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public void setRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    public double calculateLogLikelihood() {

        double logL = 0.0;

        for (Statistic statistic : dataList) {
            for (int j = Math.max(0, from); j < Math.min(statistic.getDimension(), to); j++) {

                double value = statistic.getStatisticValue(j) - offset;
                if (offset > 0.0 && value < 0.0) {
                    // fixes a problem with the offset on exponential distributions not
                    // actually bounding the distribution. This only performs this check
                    // if a non-zero offset is actually given otherwise it assumes the
                    // parameter is either legitimately allowed to go negative or is bounded
                    // at zero anyway.
                    return Double.NEGATIVE_INFINITY;
                }

                logL += distribution.logPdf(value);
            }
        }
        return logL;
    }

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public Element createElement(Document d) {
        throw new RuntimeException("Not implemented yet!");
    }

    private final Distribution distribution;
    private final double offset;
}

