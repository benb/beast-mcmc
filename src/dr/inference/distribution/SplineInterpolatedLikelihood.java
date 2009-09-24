/*
 * SplineInterpolatedLikelihood.java
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

import dr.inference.model.SplineBasis;
import dr.inference.model.Variable;

/**
 * @author Marc A. Suchard
 * @author Andrew Rambaut
 */
public class SplineInterpolatedLikelihood extends EmpiricalDistributionLikelihood {

    public SplineInterpolatedLikelihood(String fileName, int degree, boolean inverse, boolean byColumn) {
        super(fileName, inverse, byColumn);

         // Set-up spline basis, could be degree = 1 for linear interpolation
//        splineBasis = new SplineBasis(getId(),new Variable.D(values), new Variable.D(density), degree);

        // Something is wrong with the spline basis routines...  just do simple linear interpolation

    }

    @Override
    protected double logPDF(double x) {
//        return splineBasis.evaluate(x);

        final int len = values.length;

        if (x < values[0] || x > values[len - 1])
            return Double.NEGATIVE_INFINITY;

        double rtnValue = 0;

        for(int i=1; i<len; i++) {
            if (values[i] > x) { // first largest point
                final double diffValue = values[i] - values[i-1];
                final double diffDensity = density[i] - density[i-1];
                rtnValue = density[i] - (values[i]-x) / diffValue * diffDensity;
                break;
            }
        }

        rtnValue = Math.log(rtnValue);
        if (inverse)
            rtnValue *= -1;
  
        return rtnValue;
    }

    private SplineBasis splineBasis = null;
}
