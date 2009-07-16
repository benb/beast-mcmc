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
 * being distributed according to a distribution generated empirically from some data.
 *
 * @author Andrew Rambaut
 * @version $Id:$
 */

public class EmpiricalDistributionLikelihood extends AbstractDistributionLikelihood {

    public static final String EMPIRICAL_DISTRIBUTION_LIKELIHOOD = "empricalDistributionLikelihood";

    private int from = -1;
    private int to = Integer.MAX_VALUE;

    public EmpiricalDistributionLikelihood(String fileName) {
        super(null);

        // Load data
        values = null;
        density = null;
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

                double value = statistic.getStatisticValue(j);
                logL += logPDF(value);
            }
        }
        return logL;
    }

    private double logPDF(double value) {
        return 0.0;
    }

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public Element createElement(Document d) {
        throw new RuntimeException("Not implemented yet!");
    }

    private final double[] values;
    private final double[] density;
}

