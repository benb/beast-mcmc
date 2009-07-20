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

    public SplineInterpolatedLikelihood(String fileName, int degree) {
        super(fileName);

        // Set-up spline basis, could be degree = 1 for linear interpolation
        splineBasis = new SplineBasis(getId(),new Variable.D(values), new Variable.D(density), degree);
    }

    public SplineInterpolatedLikelihood(String fileName) {
    	super(fileName);
	}

	@Override
    protected double logPDF(double x) {
        return splineBasis.evaluate(x);
    }

    private SplineBasis splineBasis = null;
}
